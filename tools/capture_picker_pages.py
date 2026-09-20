# -*- coding: utf-8 -*-
"""Sweep the training troop-picker leftwards and record which troops are visible on each page.

Drives the device to the training page, opens the troop-selection panel, pins the list to its
left-most position, then repeatedly captures + swipes left. For every page it reports which
troop icons are detected (masked-NCC, picker area only) so we can (a) confirm 黑油兵 / 超级兵
pages and (b) pick real screenshots to derive scroll-independent train-button features from.

Usage: python capture_picker_pages.py [--pages 12] [--prefix page]
"""
import argparse
import os
import cv2
import numpy as np

from game_state import recover_to_training, tap, cap, swipe, panel_open, OUT
from icon_detect import load_icon, T, SCALES

ICON_ROOT = r'D:\work\my\codebuddy_test\coc_manager_test01\src\main\resources\static\icon'
GROUPS = ['圣水兵', '黑油兵', '超级兵']
PICKER = (480, 1280)  # y0, y1 of the picker panel
SCORE = 0.60


def names_in(sub):
    return sorted(os.path.splitext(f)[0] for f in os.listdir(os.path.join(ICON_ROOT, sub))
                  if f.endswith('.png'))


def detect_all(gray):
    """Return [(group, name, score, x, y)] for every troop found in the picker area."""
    out = []
    for sub in GROUPS:
        for name in names_in(sub):
            tg, tm = load_icon(sub, name)
            best, pt = -1.0, None
            for s in SCALES:
                tw = th = max(8, int(T * s))
                if tw > gray.shape[1] or th > gray.shape[0]:
                    continue
                t = cv2.resize(tg, (tw, th))
                mk = cv2.resize(tm, (tw, th), cv2.INTER_NEAREST)
                _, mk = cv2.threshold(mk, 128, 255, cv2.THRESH_BINARY)
                res = cv2.matchTemplate(gray, t, cv2.TM_CCOEFF_NORMED, mask=mk)
                res[~np.isfinite(res)] = -1.0
                _, mx, _, mp = cv2.minMaxLoc(res)
                if np.isfinite(mx) and mx > best:
                    best, pt = float(mx), (mp[0] + tw // 2, mp[1] + th // 2)
            if best >= SCORE and pt:
                out.append((sub, name, best, pt[0], pt[1]))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pages', type=int, default=12)
    ap.add_argument('--prefix', default='page')
    args = ap.parse_args()

    assert recover_to_training(), 'cannot reach training page'
    img = cv2.imread(os.path.join(OUT, 'state.png'))
    if not panel_open(img):
        tap(891, 234, dt=1.4)
    print('panel open =', panel_open(cv2.imread(cap('%s_pre.png' % args.prefix))))

    # pin the list to its left-most position (deterministic starting point)
    for _ in range(6):
        swipe(120, 560, 1180, 560, 300)
    print('pinned to left edge')

    for i in range(args.pages):
        p = cap('%s_%02d.png' % (args.prefix, i))
        full = cv2.imread(p)
        gray = cv2.cvtColor(full[PICKER[0]:PICKER[1]], cv2.COLOR_BGR2GRAY)
        hits = detect_all(gray)
        hits.sort(key=lambda h: (h[0], h[3]))
        summary = {}
        for sub, name, sc, x, y in hits:
            summary.setdefault(sub, []).append('%s(%.2f)@%d,%d' % (name, sc, x, y + PICKER[0]))
        print('--- %s_%02d.png: %d hits' % (args.prefix, i, len(hits)))
        for sub in GROUPS:
            if sub in summary:
                print('    %s: %s' % (sub, ', '.join(summary[sub])))
        swipe(1180, 560, 120, 560, 600)
    print('done; screenshots in', OUT)


if __name__ == '__main__':
    main()
