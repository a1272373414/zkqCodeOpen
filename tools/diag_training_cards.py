# -*- coding: utf-8 -*-
"""Diagnostic v2: for ONE shot, print every 圣水兵 icon's best NCC match (score + center),
so we can see the full layout and which troops are present / where."""
import os
import cv2
import numpy as np
from PIL import Image

ICON_ROOT = r'D:\work\my\codebuddy_test\coc_manager_test01\src\main\resources\static\icon'
SHOT = r'tools\captures\training\02_troops_top.png'
T = 88
SCALES = [0.7, 0.85, 1.0, 1.15, 1.3]


def load_icon(name, color):
    im = Image.open(os.path.join(ICON_ROOT, color, name + '.png')).convert('RGBA')
    a = np.asarray(im)[:, :, 3]; rgb = np.asarray(im)[:, :, :3]
    ys, xs = np.where(a > 128)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    art = rgb[y0:y1 + 1, x0:x1 + 1]; am = a[y0:y1 + 1, x0:x1 + 1]
    ah, aw = art.shape[:2]; side = max(ah, aw)
    cv = np.zeros((side, side, 3), np.uint8); cm = np.zeros((side, side), np.uint8)
    cv[(side - ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = art
    cm[(side - ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = am
    g = cv2.cvtColor(cv2.resize(cv, (T, T), cv2.INTER_AREA), cv2.COLOR_RGB2GRAY)
    m = cv2.resize(cm, (T, T), cv2.INTER_NEAREST); _, m = cv2.threshold(m, 128, 255, cv2.THRESH_BINARY)
    return g, m


ICONS = []
d = os.path.join(ICON_ROOT, '圣水兵')
for f in sorted(os.listdir(d)):
    if f.endswith('.png'):
        g, m = load_icon(f[:-4], '圣水兵')
        ICONS.append((f[:-4], g, m))

img = np.asarray(Image.open(SHOT).convert('RGB'))[:, :, ::-1].copy()
gimg = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
H, W = gimg.shape
print('shot %s  (%d x %d), icons=%d' % (SHOT, W, H, len(ICONS)))

rows = []
for name, tg, tm in ICONS:
    best_sc, best_pt = -1, None
    for s in SCALES:
        tw = max(8, int(T * s)); th = max(8, int(T * s))
        t = cv2.resize(tg, (tw, th)); mk = cv2.resize(tm, (tw, th), cv2.INTER_NEAREST)
        _, mk = cv2.threshold(mk, 128, 255, cv2.THRESH_BINARY)
        res = cv2.matchTemplate(gimg, t, cv2.TM_CCOEFF_NORMED, mask=mk)
        _, mx, _, mp = cv2.minMaxLoc(res)
        if mx > best_sc:
            best_sc, best_pt = mx, (mp[0] + tw//2, mp[1] + th//2)
    rows.append((name, best_sc, best_pt[0], best_pt[1]))
rows.sort(key=lambda r: -r[1])
for name, sc, x, y in rows:
    print('  %-10s  score=%.2f  center=(%4d,%4d)' % (name, sc, x, y))
