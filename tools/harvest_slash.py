# -*- coding: utf-8 -*-
"""Harvest the CURRENT-build '/' glyphs and refresh them inside PixelFontDigits.kt.

Why: the legacy library shipped with old-font '/' shapes (harvest_pixel_font.py keeps
them verbatim because there is no ground truth), and on the current build they match at
only ~0.6 (< PixelFontOcr's 0.75), so "used/total" readouts come back as "340?340".

The '/' lives between the two numbers of an "used/total" readout, so we locate it from
real screenshots with a shape test instead of a label:
  * digit-sized bounding box (tall & narrow),
  * ink drifts monotonically from the TOP-RIGHT to the BOTTOM-LEFT (a slash, not '7'
    whose top bar would put ink in the top-left as well).

Digits and '.' are preserved; every '/' entry (legacy + previously harvested) is replaced
by the freshly harvested ones, so the result is reproducible: run this script again after
a game update.

Usage:
  python tools/harvest_slash.py            # inspect only (print candidates as ASCII)
  python tools/harvest_slash.py --apply    # rewrite PixelFontDigits.kt
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image

TOOLS = os.path.dirname(os.path.abspath(__file__))
KT = os.path.join(TOOLS, '..', 'app', 'src', 'main', 'java', 'com', 'coc', 'zkqcode',
                  'jar', 'code', 'universal', 'recognizer', 'PixelFontDigits.kt')
sys.path.insert(0, TOOLS)
from harvest_pixel_font import ALPHABET, encode_glyph          # noqa: E402

# (screenshot, region x0,y0,x1,y1 around the "used/total" readout, grayMin, grayMax, maxSat)
SHOTS = [
    (r'I:\coc\游戏截图\主世界-配兵-兵种1.png',              (400, 130, 760, 210), 160, 255, 70),
    (r'I:\coc\游戏截图\主世界-配兵-兵种2.png',              (400, 130, 760, 210), 160, 255, 70),
    (r'I:\coc\游戏截图\主世界-配兵-法术.png',               (400, 130, 760, 210), 160, 255, 70),
    (r'I:\coc\游戏截图\主世界-配兵-攻城器1.png',            (400, 130, 760, 210), 160, 255, 70),
    (r'I:\coc\游戏截图\主世界-配兵-攻城机器配置2.png',      (400, 130, 760, 210), 160, 255, 70),
    (r'I:\coc\游戏截图\主世界-准备进攻页3-援兵已就位.png',  (400, 130, 760, 210), 160, 255, 70),
    (r'I:\coc\游戏截图\主世界-准备进攻页3-部落城堡蛋糕生效中.png', (400, 130, 760, 210), 160, 255, 70),
    (r'I:\coc\游戏截图\补充1\都城-配兵01.png', (150, 80, 290, 115), 200, 255, 50),
    (r'I:\coc\游戏截图\补充1\都城-配兵02.png', (150, 80, 290, 115), 200, 255, 50),
    (r'I:\coc\游戏截图\补充1\都城-配兵03.png', (150, 80, 290, 115), 200, 255, 50),
    (r'I:\coc\游戏截图\补充1\都城-配兵04.png', (150, 80, 290, 115), 200, 255, 50),
    (r'I:\coc\游戏截图\补充1\都城-配兵05.png', (150, 80, 290, 115), 200, 255, 50),
    (r'I:\coc\游戏截图\补充1\都城-配兵06.png', (150, 80, 290, 115), 200, 255, 50),
    (r'I:\coc\游戏截图\补充1\都城-配兵07.png', (150, 80, 290, 115), 200, 255, 50),
]


def ink(img, gm, gx, ms):
    r = img[:, :, 0].astype(np.int16)
    g = img[:, :, 1].astype(np.int16)
    b = img[:, :, 2].astype(np.int16)
    gray = r * 0.299 + g * 0.587 + b * 0.114
    sat = img.max(2).astype(np.int16) - img.min(2).astype(np.int16)
    return (gray >= gm) & (gray <= gx) & (sat <= ms)


def components(mask):
    h, w = mask.shape
    seen = np.zeros((h, w), bool)
    out = []
    for y in range(h):
        for x in range(w):
            if not mask[y, x] or seen[y, x]:
                continue
            stack = [(y, x)]
            seen[y, x] = True
            pix = []
            while stack:
                cy, cx = stack.pop()
                pix.append((cy, cx))
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        ny, nx = cy + dy, cx + dx
                        if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not seen[ny, nx]:
                            seen[ny, nx] = True
                            stack.append((ny, nx))
            ys = [p[0] for p in pix]; xs = [p[1] for p in pix]
            y0, y1, x0, x1 = min(ys), max(ys), min(xs), max(xs)
            bits = np.zeros((y1 - y0 + 1, x1 - x0 + 1), bool)
            for (cy, cx) in pix:
                bits[cy - y0, cx - x0] = True
            out.append(bits)
    return out


def is_slash(bits):
    """True for a '/' : tall & narrow, ink on the top-RIGHT and bottom-LEFT (not '7')."""
    h, w = bits.shape
    if not (10 <= h <= 20 and 3 <= w <= 10):
        return False
    rows = [np.where(bits[y])[0] for y in range(h)]
    rows = [r for r in rows if len(r)]
    if len(rows) < h * 0.7:
        return False
    top = np.mean([np.mean(r) for r in rows[:2]])
    bot = np.mean([np.mean(r) for r in rows[-2:]])
    # top ink on the right half, bottom ink on the left half, and a clear leftward drift
    return top >= w * 0.55 and bot <= w * 0.45 and (top - bot) >= 3


def parse_existing_digits():
    text = open(KT, encoding='utf-8').read()
    body = text.split('"""')[1]
    out = []
    for ln in body.split('\n'):
        ln = ln.strip()
        if ln and ln.split('|')[0] != '/':
            out.append(ln)
    return out


def harvest():
    glyphs = {}
    for path, (x0, y0, x1, y1), gm, gx, ms in SHOTS:
        if not os.path.exists(path):
            print('  skip (missing):', path)
            continue
        img = np.asarray(Image.open(path).convert('RGB'))[y0:y1, x0:x1]
        m = ink(img, gm, gx, ms)
        for bits in components(m):
            if is_slash(bits):
                h, w = bits.shape
                key = (h, w, bits.tobytes())
                if key not in glyphs:
                    glyphs[key] = (bits, os.path.basename(path))
    return glyphs


def show(bits):
    for row in bits:
        print('      ' + ''.join('#' if v else '.' for v in row))


def rewrite(entries):
    head = ('package com.coc.zkqcode.jar.code.universal.recognizer\n\n'
            '/**\n'
            ' * Pixel font library for in-game numbers (digits and separators).\n'
            ' *\n'
            ' * Entry format: "char|height,width|base-64 bitmap" (row-major, MSB first, bit=1=ink;\n'
            ' * only the last h*w bits are used). Digits are harvested from the resource rows of\n'
            ' * labeled 1280x720 screenshots (tools/harvest_pixel_font.py); the separators\n'
            ' * ("/", ".") come from tools/harvest_slash.py so they match the CURRENT build font\n'
            ' * (the old-font legacy "/" shapes scored ~0.6 and made "used/total" read as "340?340").\n'
            ' */\n'
            'internal const val PIXEL_FONT_DIGITS: String = """\n')
    body = '\n'.join(entries)
    io = open(KT, 'w', encoding='utf-8')
    io.write(head + body + '\n"""\n')
    io.close()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true', help='rewrite PixelFontDigits.kt')
    args = ap.parse_args()

    kept = parse_existing_digits()
    print('existing non-slash entries kept:', len(kept))
    glyphs = harvest()
    print('harvested distinct "/" glyphs:', len(glyphs))
    slash_entries = []
    for (bits, src) in glyphs.values():
        h, w = bits.shape
        print('  %dx%d  <- %s' % (h, w, src))
        show(bits)
        slash_entries.append(encode_glyph('/', bits.reshape(-1), h, w))
    slash_entries.sort()
    if args.apply:
        rewrite(kept + slash_entries)
        print('rewrote', os.path.abspath(KT), 'with', len(kept), '+', len(slash_entries), 'entries')
    else:
        print('(inspect only, pass --apply to rewrite)')


if __name__ == '__main__':
    main()
