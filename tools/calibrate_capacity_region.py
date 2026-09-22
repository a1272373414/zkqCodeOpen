# -*- coding: utf-8 -*-
"""Calibrate MainBaseArmyPlan.CAPACITY_REGION (training-page top "used/total" housing text).

Mirrors PixelFontOcr.recognize() exactly (same ink criterion, same component/line grouping,
same glyph match) so a region that works here works on-device:

  ink = gray in [grayMin, grayMax] AND saturation <= maxSaturation

Usage:
  python tools/calibrate_capacity_region.py <screenshot.png> [--band y0,y1]
    [--gray-min 160] [--gray-max 255] [--max-sat 70]

It prints every recognized text line in the search band with its bounding box, so the
"X/Y" housing line can be read off and written into CAPACITY_REGION.
"""
import argparse
import os
import re
from collections import deque

import numpy as np
from PIL import Image

ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz&#"
KT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main',
                  'java', 'com', 'coc', 'zkqcode', 'jar', 'code', 'universal',
                  'recognizer', 'PixelFontDigits.kt')


def parse_glyphs(path=KT):
    """Return list of (char, h, w, bits_bool_2d) from PixelFontDigits.kt."""
    text = open(path, encoding='utf-8').read()
    glyphs = []
    for line in text.splitlines():
        line = line.strip()
        parts = line.split('|')
        if len(parts) < 3:
            continue
        ch = parts[0]
        dims = parts[1].split(',')
        if len(dims) != 2:
            continue
        try:
            h, w = int(dims[0]), int(dims[1])
        except ValueError:
            continue
        if w <= 0 or h <= 0:
            continue
        data = '|'.join(parts[2:])
        raw = ''.join('1' if (ALPHABET.find(c) >> i) & 1 else '0'
                      for c in data for i in range(5, -1, -1))
        need = w * h
        if len(raw) < need:
            continue
        bits = np.frombuffer(raw[-need:].encode(), np.uint8) == ord('1')
        glyphs.append((ch, h, w, bits.reshape(h, w)))
    return glyphs


def ink_mask(img, gray_min, gray_max, max_sat):
    r = img[:, :, 0].astype(np.int16)
    g = img[:, :, 1].astype(np.int16)
    b = img[:, :, 2].astype(np.int16)
    gray = (r * 0.299 + g * 0.587 + b * 0.114)
    sat = img.max(axis=2).astype(np.int16) - img.min(axis=2).astype(np.int16)
    return (gray >= gray_min) & (gray <= gray_max) & (sat <= max_sat)


def components(mask):
    h, w = mask.shape
    seen = np.zeros((h, w), bool)
    out = []
    for y in range(h):
        for x in range(w):
            if not mask[y, x] or seen[y, x]:
                continue
            q = deque([(y, x)])
            seen[y, x] = True
            minx = maxx = x
            miny = maxy = y
            pix = []
            while q:
                cy, cx = q.popleft()
                pix.append((cy, cx))
                if cx < minx: minx = cx
                if cx > maxx: maxx = cx
                if cy < miny: miny = cy
                if cy > maxy: maxy = cy
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        ny, nx = cy + dy, cx + dx
                        if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not seen[ny, nx]:
                            seen[ny, nx] = True
                            q.append((ny, nx))
            cw, chh = maxx - minx + 1, maxy - miny + 1
            sub = np.zeros((chh, cw), bool)
            for (cy, cx) in pix:
                sub[cy - miny, cx - minx] = True
            out.append((minx, miny, maxx, maxy, sub))
    return out


def _scaled(gm, H, W):
    h, w = gm.shape
    ys = (np.arange(H) * h // H).clip(0, h - 1)
    xs = (np.arange(W) * w // W).clip(0, w - 1)
    return gm[ys][:, xs]


def match_glyph(cm, glyphs, min_sim=0.75, size_tol=2):
    """Mirror PixelFontOcr.matchGlyph: phase 1 same-size, phase 2 scaled fallback."""
    h, w = cm.shape
    best_ch, best_score = None, 0.0
    for (ch, gh, gw, gm) in glyphs:
        if abs(gh - h) > size_tol or abs(gw - w) > size_tol:
            continue
        rows, cols = max(gh, h), max(gw, w)
        g2 = np.zeros((rows, cols), bool)
        c2 = np.zeros((rows, cols), bool)
        g2[:gh, :gw] = gm
        c2[:h, :w] = cm
        score = (g2 == c2).sum() / float(rows * cols)
        if score > best_score:
            best_score, best_ch = score, ch
    if best_ch is not None and best_score >= min_sim:
        return best_ch, best_score
    if 8 <= h <= 34 and 3 <= w <= 40:
        for (ch, gh, gw, gm) in glyphs:
            score = (_scaled(gm, h, w) == cm).mean()
            if score > best_score:
                best_score, best_ch = score, ch
    return best_ch if best_score >= min_sim else None, best_score


def recognize_lines(img, gray_min, gray_max, max_sat, glyphs,
                    min_w=2, max_size=50, min_h=3, word_gap=12):
    mask = ink_mask(img, gray_min, gray_max, max_sat)
    comps = [c for c in components(mask)
             if min_w <= (c[2] - c[0] + 1) <= max_size and min_h <= (c[3] - c[1] + 1) <= max_size]
    comps.sort(key=lambda c: (c[1], c[0]))
    lines = []
    for c in comps:
        placed = False
        for ln in lines:
            if c[1] <= max(x[3] for x in ln) and c[3] >= min(x[1] for x in ln):
                ln.append(c)
                placed = True
                break
        if not placed:
            lines.append([c])
    result = []
    for ln in lines:
        ln.sort(key=lambda c: c[0])
        # split the row into groups by horizontal gap > word_gap
        groups = [[ln[0]]]
        for c in ln[1:]:
            if c[0] - groups[-1][-1][2] > word_gap:
                groups.append([c])
            else:
                groups[-1].append(c)
        for g in groups:
            text = ''
            for c in g:
                ch, sc = match_glyph(c[4], glyphs)
                text += ch if ch else '?'
            x0 = min(c[0] for c in g); y0 = min(c[1] for c in g)
            x1 = max(c[2] for c in g) + 1; y1 = max(c[3] for c in g) + 1
            result.append((x0, y0, x1, y1, text, g))
    result.sort(key=lambda r: (r[1], r[0]))
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('shot')
    ap.add_argument('--band', default='0,160', help='y0,y1 search band')
    ap.add_argument('--x-band', default=None, help='x0,x1 crop (mirrors CAPACITY_REGION)')
    ap.add_argument('--gray-min', type=int, default=160)
    ap.add_argument('--gray-max', type=int, default=255)
    ap.add_argument('--max-sat', type=int, default=70)
    ap.add_argument('--debug', action='store_true', help='dump every component with size + best match')
    ap.add_argument('--size-tol', type=int, default=2)
    ap.add_argument('--min-sim', type=float, default=0.75)
    args = ap.parse_args()

    y0, y1 = (int(v) for v in args.band.split(','))
    img = np.asarray(Image.open(args.shot).convert('RGB'))
    xa, xb = (0, img.shape[1])
    if args.x_band:
        xa, xb = (int(v) for v in args.x_band.split(','))
    band = img[y0:y1, xa:xb]
    glyphs = parse_glyphs()
    print('glyphs:', len(glyphs), '| shot', img.shape[1], 'x', img.shape[0],
          '| region x[%d:%d] y[%d:%d]' % (xa, xb, y0, y1),
          '| size_tol', args.size_tol, 'min_sim', args.min_sim)
    if args.debug:
        mask = ink_mask(band, args.gray_min, args.gray_max, args.max_sat)
        comps = components(mask)
        comps.sort(key=lambda c: (c[1], c[0]))
        for c in comps:
            w = c[2] - c[0] + 1; h = c[3] - c[1] + 1
            if w < 2 or h < 3:
                continue
            ch, sc = match_glyph(c[4], glyphs, args.min_sim, args.size_tol)
            print('  comp bbox=(%d,%d,%d,%d) size=%dx%d best=%s(%.2f)' %
                  (c[0] + xa, c[1] + y0, c[2] + xa, c[3] + y0, w, h, ch, sc))
        return
    lines = recognize_lines(band, args.gray_min, args.gray_max, args.max_sat, glyphs)
    for (x0, by0, x1, by1, text, comps) in lines:
        print('group bbox=(%d,%d,%d,%d) text=%r comps=%d' %
              (x0 + xa, by0 + y0, x1 + xa, by1 + y0, text, len(comps)))
        digits = ''.join(ch for ch in text if ch.isdigit())
        if '/' not in text and digits:
            print('   -> parse total (last digit group) via regex:', digits)


if __name__ == '__main__':
    main()
