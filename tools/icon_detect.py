# -*- coding: utf-8 -*-
"""Lightweight masked-NCC icon matcher (mirrors gen_mainbase_train_button_colors.detect)."""
import os
import cv2
import numpy as np
from PIL import Image

ICON_ROOT = r'D:\work\my\codebuddy_test\coc_manager_test01\src\main\resources\static\icon'
T = 88
SCALES = [0.7, 0.85, 1.0, 1.15, 1.3]


def load_icon(sub, name):
    im = Image.open(os.path.join(ICON_ROOT, sub, name + '.png')).convert('RGBA')
    a = np.asarray(im)[:, :, 3]
    rgb = np.asarray(im)[:, :, :3]
    ys, xs = np.where(a > 128)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    art = rgb[y0:y1 + 1, x0:x1 + 1]
    am = a[y0:y1 + 1, x0:x1 + 1]
    ah, aw = art.shape[:2]
    side = max(ah, aw)
    cv = np.zeros((side, side, 3), np.uint8)
    cm = np.zeros((side, side), np.uint8)
    cv[(side - ah) // 2:(side - ah) // 2 + ah, (side - aw) // 2:(side - aw) // 2 + aw] = art
    cm[(side - ah) // 2:(side - ah) // 2 + ah, (side - aw) // 2:(side - aw) // 2 + aw] = am
    g = cv2.cvtColor(cv2.resize(cv, (T, T), cv2.INTER_AREA), cv2.COLOR_RGB2GRAY)
    m = cv2.resize(cm, (T, T), cv2.INTER_NEAREST)
    _, m = cv2.threshold(m, 128, 255, cv2.THRESH_BINARY)
    return g, m


def detect_icon(gimg, sub, name):
    """Return best NCC score (0..1) of `name` icon against grayscale image."""
    tg, tm = load_icon(sub, name)
    best = -1.0
    for s in SCALES:
        tw = max(8, int(T * s))
        th = max(8, int(T * s))
        t = cv2.resize(tg, (tw, th))
        mk = cv2.resize(tm, (tw, th), cv2.INTER_NEAREST)
        _, mk = cv2.threshold(mk, 128, 255, cv2.THRESH_BINARY)
        res = cv2.matchTemplate(gimg, t, cv2.TM_CCOEFF_NORMED, mask=mk)
        res[~np.isfinite(res)] = -1.0
        _, mx, _, _ = cv2.minMaxLoc(res)
        if np.isfinite(mx) and mx > best:
            best = float(mx)
    return best


def find_icon(gimg, sub, name):
    """Return (score, cx, cy) of best match center, or (score, None, None)."""
    tg, tm = load_icon(sub, name)
    best = -1.0
    best_pt = None
    for s in SCALES:
        tw = max(8, int(T * s))
        th = max(8, int(T * s))
        t = cv2.resize(tg, (tw, th))
        mk = cv2.resize(tm, (tw, th), cv2.INTER_NEAREST)
        _, mk = cv2.threshold(mk, 128, 255, cv2.THRESH_BINARY)
        res = cv2.matchTemplate(gimg, t, cv2.TM_CCOEFF_NORMED, mask=mk)
        res[~np.isfinite(res)] = -1.0
        _, mx, _, mp = cv2.minMaxLoc(res)
        if np.isfinite(mx) and mx > best:
            best = float(mx)
            best_pt = (mp[0] + tw // 2, mp[1] + th // 2)
    return best, best_pt
