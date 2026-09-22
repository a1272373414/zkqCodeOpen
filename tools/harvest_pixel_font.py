# -*- coding: utf-8 -*-
"""
Rebuild the pixel font library (PixelFontDigits.kt) from the CURRENT game version.

The legacy library came from the freescript 图灵 OCR and only reaches ~40-50% accuracy on the
current build, because the digit shapes changed. This script harvests real glyphs instead:

  * Resource numbers are printed as bright text, and the screenshot filenames in
    I:\\coc\\游戏截图\\补充1 carry the exact values
    (e.g. "...金币18942001-圣水22392490-黑油405237-宝石5682.png"), so the glyph labels come
    from the filename and do NOT need any manual annotation.
  * Binarization uses the same threshold as PixelFontOcr.recognize() (gray >= 200), so the
    harvested glyphs match what the matcher sees at runtime.

Output format is the existing one:  "字符|高,宽|64进制点阵串" (row-major, MSB first, bit=1=ink).

Usage:  python tools/harvest_pixel_font.py
"""
import io
import os
import re
from collections import defaultdict

import numpy as np
from PIL import Image
from scipy import ndimage

SHOT_DIR = r'I:\coc\游戏截图\补充1'
OUT_KT = r'd:\work\my\zkqfz\zkqCodeOpen\app\src\main\java\com\coc\zkqcode\jar\code\universal\recognizer\PixelFontDigits.kt'

ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz&#"

# Resource rows in the 1280x720 main-village layout: (label key, y1, y2)
ROWS = [('gold', 18, 58), ('elixir', 70, 112), ('dark', 122, 165), ('gems', 170, 222)]
X1, X2 = 1070, 1272

NAME_RE = re.compile(r'金币(\d+)-圣水(\d+)-黑油-?(\d+)-宝石(\d+)')
KEY_MAP = {'gold': 1, 'elixir': 2, 'dark': 3, 'gems': 4}


def load(name):
    return np.asarray(Image.open(os.path.join(SHOT_DIR, name)).convert('RGB')).astype(np.int16)


def ink_mask(img, y1, y2):
    """
    Ink criterion used for harvesting: bright AND (near) neutral.

    The resource numbers are white-ish text drawn on strongly coloured bars (gold / elixir /
    dark / gem). A plain luminance threshold would also swallow the coloured bar itself and
    merge neighbouring digits, so we additionally require the pixel to be desaturated.
    PixelFontOcr uses the same rule (see INK_MODE_NEAR_WHITE).
    """
    sub = img[y1:y2, X1:X2].astype(np.int16)
    gray = (sub[:, :, 0] * 0.299 + sub[:, :, 1] * 0.587 + sub[:, :, 2] * 0.114)
    sat = sub.max(axis=2) - sub.min(axis=2)
    return (gray >= 200) & (sat <= 50)


def components(mask, min_h=10, max_h=45, min_w=3):
    lbl, n = ndimage.label(mask)
    out = []
    for sl in ndimage.find_objects(lbl):
        if sl is None:
            continue
        ys, xs = sl
        h = ys.stop - ys.start
        w = xs.stop - xs.start
        if not (min_h <= h <= max_h) or w < min_w:
            continue
        out.append((xs.start, ys.start, xs.stop, ys.stop))
    out.sort(key=lambda b: b[0])
    return out


def encode_glyph(ch, mask_bits, h, w):
    """mask_bits: flat list of bools, row-major. -> "ch|h,w|base64" (loader keeps last h*w bits)."""
    bits = ''.join('1' if b else '0' for b in mask_bits)
    assert len(bits) == h * w, (len(bits), h, w)
    pad = (-len(bits)) % 6
    full = '0' * pad + bits
    out = []
    for i in range(0, len(full), 6):
        out.append(ALPHABET[int(full[i:i + 6], 2)])
    return '%s|%d,%d|%s' % (ch, h, w, ''.join(out))


def decode_glyph(h, w, data):
    raw = ''
    for c in data:
        v = ALPHABET.find(c)
        if v < 0:
            continue
        raw += ''.join('1' if (v >> i) & 1 else '0' for i in range(5, -1, -1))
    raw = raw[-h * w:]
    return np.array([b == '1' for b in raw], dtype=bool).reshape(h, w)


def select_window(comps, n):
    """
    Pick the contiguous run of n components that forms the number.

    A row can contain an extra near-white blob next to the number (e.g. a badge). The digits
    of a number are the tightest contiguous group, so choose the window with the smallest span
    instead of dropping "the smallest" blob (which would wrongly remove a narrow '1').
    """
    if len(comps) <= n:
        return comps
    best, best_span = None, None
    for i in range(0, len(comps) - n + 1):
        window = comps[i:i + n]
        span = window[-1][2] - window[0][0]
        if best_span is None or span < best_span:
            best, best_span = window, span
    return best


def harvest(files):
    """Return list of (digit_char, h, w, bitmask_2d) samples plus per-row stats."""
    samples = []
    stats = defaultdict(lambda: [0, 0])
    for name in files:
        m = NAME_RE.search(name)
        if not m:
            continue
        img = load(name)
        for key, y1, y2 in ROWS:
            digits = m.group(KEY_MAP[key])
            mask = ink_mask(img, y1, y2)
            comps = components(mask)
            stats[key][1] += 1
            picked = select_window(comps, len(digits))
            if len(picked) != len(digits):
                continue
            stats[key][0] += 1
            for (xs, ys, xe, ye), d in zip(picked, digits):
                cm = mask[ys:ye, xs:xe]
                samples.append((d, cm.shape[0], cm.shape[1], cm))
    return samples, stats


def match_glyph(cm, glyphs, size_tol=2):
    h, w = cm.shape
    best, best_score = None, 0.0
    for (gch, gh, gw, gm) in glyphs:
        if abs(gh - h) > size_tol or abs(gw - w) > size_tol:
            continue
        rows, cols = max(gh, h), max(gw, w)
        gm2 = np.zeros((rows, cols), bool)
        cm2 = np.zeros((rows, cols), bool)
        gm2[:gh, :gw] = gm
        cm2[:h, :w] = cm
        score = (gm2 == cm2).sum() / float(rows * cols)
        if score > best_score:
            best_score, best = score, gch
    return best, best_score


def parse_separators():
    """Parse the legacy '.'/'/' entries into the same (ch,h,w,mask) tuples as the harvest."""
    out = []
    for line in LEGACY_SEPARATORS.strip().split('\n'):
        parts = line.split('|')
        if len(parts) < 3:
            continue
        ch = parts[0]
        try:
            h, w = [int(v) for v in parts[1].split(',')]
        except ValueError:
            continue
        raw = ''
        for c in '|'.join(parts[2:]):
            v = ALPHABET.find(c)
            if v < 0:
                continue
            raw += ''.join('1' if (v >> i) & 1 else '0' for i in range(5, -1, -1))
        if len(raw) < h * w:
            continue
        gm = np.array([b == '1' for b in raw[-h * w:]], bool).reshape(h, w)
        out.append((ch, h, w, gm))
    return out


def evaluate(train_files, test_files, min_similarity=0.75):
    samples, _ = harvest(train_files)
    # Include the legacy '.'/'/' entries so the test also checks they do not steal digits.
    glyphs = [(d, h, w, cm) for (d, h, w, cm) in samples] + parse_separators()
    total = ok = 0
    for name in test_files:
        m = NAME_RE.search(name)
        if not m:
            continue
        img = load(name)
        for key, y1, y2 in ROWS:
            digits = m.group(KEY_MAP[key])
            mask = ink_mask(img, y1, y2)
            comps = components(mask)
            picked = select_window(comps, len(digits))
            if len(picked) != len(digits):
                total += len(digits)
                continue
            pred = ''
            for (xs, ys, xe, ye) in picked:
                cm = mask[ys:ye, xs:xe]
                ch, sc = match_glyph(cm, glyphs)
                pred += ch if (ch and sc >= min_similarity) else '?'
            total += len(digits)
            ok += sum(1 for a, b in zip(pred, digits) if a == b)
            print('    %-8s truth=%-9s pred=%-9s %s' % (key, digits, pred,
                                                        'OK' if pred == digits else 'x'))
    return ok, total


def main():
    files = sorted(f for f in os.listdir(SHOT_DIR) if NAME_RE.search(f))
    print('labeled screenshots: %d' % len(files))
    if not files:
        return

    # Hold out the last two screenshots so the accuracy is not self-validating.
    test, train = files[-2:], files[:-2]
    print('train: %d files, test: %d files' % (len(train), len(test)))

    samples, stats = harvest(train)
    print('harvested %d labeled digit samples' % len(samples))
    per = defaultdict(int)
    for d, h, w, cm in samples:
        per[d] += 1
    print('per digit:', dict(sorted(per.items())))
    print('rows aligned / total:', {k: '%d/%d' % (v[0], v[1]) for k, v in stats.items()})

    print('\nhold-out accuracy (train on %d, test on %d):' % (len(train), len(test)))
    ok, total = evaluate(train, test)
    print('  digits %d/%d = %.1f%%' % (ok, total, 100.0 * ok / total if total else 0))

    # Rebuild the library from ALL screenshots (more variants -> better coverage)
    samples, _ = harvest(files)
    entries, seen = [], set()
    for d, h, w, cm in samples:
        e = encode_glyph(d, cm.reshape(-1), h, w)
        if e in seen:
            continue
        seen.add(e)
        entries.append(e)
    print('\nfinal library: %d unique glyph entries' % len(entries))
    write_kotlin(entries)


def write_kotlin(entries):
    body = '\n'.join(entries)
    kt = '''package com.coc.zkqcode.jar.code.universal.recognizer

/**
 * Pixel font library for digits, harvested from the CURRENT game build.
 *
 * Entry format (note: the legacy header comment says "宽,高" but the data is "高,宽"):
 *
 *     字符|高,宽|64进制点阵串
 *
 * - 64-base alphabet: 0-9 A-Z a-z &# (6 bits per char)
 * - The bitmap is stored row-major, most significant bit first, 高*宽 bits in total
 * - Only the LAST 高*宽 bits are used (leading bits are padding)
 * - bit = 1 means a foreground (ink) pixel
 *
 * Source of the glyphs: the resource-number rows of real 1280x720 screenshots whose
 * filenames carry the exact values, binarized with exactly the criterion PixelFontOcr uses:
 * luminance >= 200 AND saturation (max-min channel) <= 50. The saturation term is required
 * because the numbers are white text on strongly coloured bars, otherwise the bar itself
 * becomes ink and neighbouring digits merge. Regenerate with:
 *   python tools/harvest_pixel_font.py
 *
 * Contains digits 0-9 and '.'; the CURRENT-build '/' glyphs are appended by
 * tools/harvest_slash.py (run it AFTER this script).
 */
internal const val PIXEL_FONT_DIGITS: String = """
%s
%s
"""
''' % (body, LEGACY_DOTS)
    io.open(OUT_KT, 'w', encoding='utf-8').write(kt)
    print('wrote %s (%d entries)' % (OUT_KT, len(entries) + LEGACY_DOTS.count('\n') + 1))


# Legacy entries kept for '.' and '/' (used by "8/12" style army counts). These are copied
# verbatim from the legacy 图灵 library (tools/generate_pixel_font.py source); there is no
# ground-truth screenshot for them, and every entry is verified to have enough bits.
LEGACY_SEPARATORS = """/|14,9|m70u30S1WE0m70S3WE0m7
/|13,8|30O60mC1WO30m61WC1
/|14,8|C30O60mC1WO30m61WC3
/|14,10|3W70S0u3W70S0u3W70S0u3W7
/|14,10|3WF0S1u3WF0S1u3WF0U1u3m7
/|14,9|m70O3WC1m70u3WS1mE0u7
/|14,10|3WE0S1m3WE0S1m3WE0S1m3WF
/|14,10|3WE0y1m7WE0y1m7WF0y1u7WF
/|13,9|70O3WC1m60u30S1WE0u3
/|13,8|30mE1WS30u61mC3WO3
/|13,10|E0y1m7WE0y1m7WE0y1m3WF
/|13,9|70y3WS1mE0u7WS3mE0u7
/|13,8|30m61WC30O70mE1WS3
/|11,7|S63WmO631mOE3
/|11,7|S630mOE31WOC7
/|9,6|mGO8C4623
.|2,4|3&
.|3,4|&&
/|11,8|C1WO30m61WC30O7
/|11,7|O630mO630mO61
/|16,11|3W7070E0E0S0S0u0u1u1m3m3W3W707
/|16,11|3W7W70E0E0S0y0u1u1u1m3m3W7W707
/|16,10|E0y1m70E0u1m70E0y1m7WE0y1m3
/|16,11|3W7WF0E0U0S0y0u1u1u3m3m3W7W70F
/|16,10|E0O1m30E0O1m70E0O1m3WE0S1m3
/|16,10|E0O1m70E0u1m70E0u1m7WE0S1m3
/|16,10|E0O1m30E0u1m70E0u1m7WE0S1m2
/|16,12|y0U0U0F0F0707W3m3m1u1u0y0S0U0E0F
/|16,11|3W7WF0F0U0S0y0y1u1u1m3m3W7W70F
/|16,10|E0O1m30E0u1m30E0S1m3WE0S1m3
/|15,9|60u30S1WE0m70O3WC1m70O3
/|15,10|m3W60S0m3W60S0m3W60S0u3W7
/|15,10|m3W60S0m3W60S0m3W60S0u1W7
/|16,10|E0O1m30E0O1m30E0O1m3WE0S1m2
/|15,9|60u30O1WC0m70O3WC1m60u2
/|15,9|60u30O1WC0m70O3WC1m70O3
/|15,9|60u30O1WC0m70O3WC1m70O1
/|15,9|60u30O1WC0m70O3WC1m70O2
/|16,10|E0u1m70E0u1m70E0u1m7WE0S1m2
/|16,10|E0u1m70E0u1m70E0u1m7WE0S1m3
/|16,10|E0O1m30E0u1m30E0S1m3WE0S1m2
/|16,10|E0O1m30E0u1m70E0u1m3WE0S1m2
/|16,10|E0O1m70E0u1m70E0u1m7WE0S1m2
/|16,10|E0O1m70E0u1m70E0u1m3WE0S1m2
/|16,10|E0O1m30E0O1m30E0O1m3WE0S1m3
/|14,9|m30O3WC1m60u30S1W60u3
/|14,9|m30O1WC1m60u30C1W60u3
/|15,11|7W70F0E0U0S0y0u1u1m3m3W7W7W6
/|15,10|y1m7WE0y1m7WE0y1m7WE0y1m3
/|14,10|3W70S0u3W70S0u3W70U0u1m7
/|14,9|m70u3WS1mE0u70S3mE0u7
/|15,9|60u70S3WE1m70u3WU1m70u3
/|14,9|m70O3WC1m60u30S1mE0u3
/|14,9|m70O3WC1m60u30S1mE0u7
/|14,9|m70u3WS1mE0u70S1mE0u7
/|14,9|m70O3WC1mE0u70S1mE0u7
/|14,10|3m70S0u3W70S0u3m70U0u1m7
/|15,10|y1m70E0u1m70E0y1m7WE0S1m3
/|15,9|60u70O3WC1mE0u3WS1mE0u3
/|16,10|C0u3W70S0u1W60C0u1W70C0u1m3
/|16,10|E0O1W30C0O1W30C0u1m70E0u1m3
/|16,9|m60u30O1WE0m70y3WS1m60O3
/|16,10|E0O1W30C0O1W70C0y1m70E0u1m3
/|16,10|C0m3W70S0u3W70S1u3W70C0u1W7
/|16,10|C0u3W70S0u3W60C0u1W70C0u1W7
/|16,10|C0m3W60S0u3W60S1u3WF0S0m1W7
/|12,8|mC1WO30m60mC1WO3
/|12,8|m61WC30O60mC1WS3
/|12,7|mO630WOC31WOE3
/|12,7|mO630mO63Wm863
/|12,7|mOC31WOC31WOE3
/|12,8|mC1WS30O60mC1WS3
/|12,8|m61WC30O20m41W83
/|12,8|m410O20m61WC10O3
/|12,7|mO630mO630mOE3
/|12,8|WC10O20m41WC10O3
/|12,7|mO630mO431WOE3
/|12,8|m61W830m61WC10O3
/|10,6|mGGO8C4621
/|12,8|m60WC10O20m61WC3
/|12,7|GC63WmS63WuV#&
/|12,7|WO430WO430mO61
/|11,7|GC21WGC20WO43
/|12,8|mC1WO30G60mC1WS3"""

# The legacy '/' shapes target an OLD font and score only ~0.6 on the current build (which made
# "used/total" read as "340?340"), so they are intentionally NOT emitted here. Only '.' is kept;
# refresh '/' with tools/harvest_slash.py and run it AFTER this script.
LEGACY_DOTS = '\n'.join(
    ln for ln in LEGACY_SEPARATORS.strip().split('\n') if ln.split('|')[0] == '.')


if __name__ == '__main__':
    main()
