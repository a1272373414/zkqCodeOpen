# -*- coding: utf-8 -*-
"""Validate the SHIPPED MainBaseTrainButtonColors.kt features against a real training-list screenshot.

For every TrainBtn feature it:
  1. locates itself inside its own declared region (the box the app will scan at runtime),
  2. scans the WHOLE screenshot to see if the same feature also matches a DIFFERENT card
     (a cross-match = would mis-tap at runtime).

This mirrors ColorSchema.findMultiColors: anchor pixel within 25/channel of the anchor color,
then every offset pixel within 25/channel of its color. Colours are BGR (0xBBGGRR).
"""
import os
import re
import sys
import cv2
import numpy as np

KOTLIN = r'app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainButtonColors.kt'
SHOT = sys.argv[1] if len(sys.argv) > 1 else r'tools\captures\training\02_troops_top.png'
TH = 25


def hex_bgr(h):
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], dtype=np.int16)


def parse_kotlin():
    txt = open(KOTLIN, encoding='utf-8').read()
    pat = re.compile(
        r'TrainBtn(\w+)\s*=\s*ColorSchema\.parse\(\s*'
        r'(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'          # rx1 ry1 rx2 ry2
        r'"([0-9A-Fa-f]{6})",\s*'                          # anchor hex
        r'"([^"]*)",\s*'                                   # offsets
        r'\d+,\s*([\d.]+),\s*"([^"]*)"', re.DOTALL)
    out = []
    for m in pat.finditer(txt):
        name = m.group(1)
        rx1, ry1, rx2, ry2 = (int(m.group(i)) for i in (2, 3, 4, 5))
        anchor = hex_bgr(m.group(6))
        offs = []
        for tok in m.group(7).split(','):
            if not tok:
                continue
            dx, dy, h = tok.split('|')
            offs.append((int(dx), int(dy), hex_bgr(h)))
        out.append((name, (rx1, ry1, rx2, ry2), anchor, offs, m.group(9)))
    return out


def collect(scan, anchor, offs):
    """All match points (relative to scan's top-left) for anchor+offsets within scan."""
    if scan.size == 0:
        return []
    mask = (np.abs(scan.astype(np.int16) - anchor) <= TH).all(axis=2)
    ys, xs = np.where(mask)
    found = []
    for y, x in zip(ys, xs):
        ok = True
        for dx, dy, c in offs:
            tx, ty = x + dx, y + dy
            if tx < 0 or tx >= scan.shape[1] or ty < 0 or ty >= scan.shape[0]:
                ok = False
                break
            if (np.abs(scan[ty, tx].astype(np.int16) - c) > TH).any():
                ok = False
                break
        if ok:
            found.append((x, y))
    return found


def main():
    feats = parse_kotlin()
    img = cv2.imread(SHOT)
    if img is None:
        print('cannot read', SHOT)
        return
    H, W = img.shape[:2]
    print('shot %s (%d x %d), features=%d' % (SHOT, W, H, len(feats)))
    print('%-12s  %-5s  %-4s  %s' % ('name', 'own', 'cross', 'cross-locations'))
    bad = 0
    for name, (rx1, ry1, rx2, ry2), anchor, offs, desc in feats:
        rx1, ry1, rx2, ry2 = [max(0, v) for v in (rx1, ry1, rx2, ry2)]
        rx2, ry2 = [min(W, rx2), min(H, ry2)]
        region = img[ry1:ry2, rx1:rx2, :]
        own = collect(region, anchor, offs)
        own_in_region = len(own) > 0
        # global scan to find cross-matches (matches outside this feature's own region)
        glob = collect(img, anchor, offs)
        cross = [(x, y) for (x, y) in glob if not (rx1 <= x <= rx2 and ry1 <= y <= ry2)]
        status_own = 'OK' if own_in_region else 'MISS'
        status_cross = '' if not cross else ('cross=%d' % len(cross))
        flag = (not own_in_region) or bool(cross)
        if flag:
            bad += 1
        loc = ','.join('%d,%d' % (x, y) for x, y in cross[:6])
        print('%-12s  %-5s  %-4s  %s' % (name, status_own, len(cross), loc))
    print('\nRESULT:', 'ALL OK' if bad == 0 else '%d feature(s) need attention' % bad)


if __name__ == '__main__':
    main()
