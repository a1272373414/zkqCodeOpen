# -*- coding: utf-8 -*-
"""判定 Python 工具该用哪种通道序：用"生产中必然命中"的特征做判据。

FeatureColors.TrainTroops（主村庄"训练部队"按钮）在生产里稳定命中；分别在
(a) 特征按 RGB 解析、图按 BGR 直接比较（原工具行为）
(b) 特征转 BGR 后与 BGR 图比较（修正后的行为）
下测试，谁命中谁才是与 App 一致的口径。
"""
import os
import re
import cv2
import numpy as np

PKG = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage")
FEATURE = os.path.join(PKG, 'FeatureColors.kt')
SHOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'captures', 'training', '00_main_village.png')
TH = 25


def parse(path, names):
    txt = open(path, encoding='utf-8').read()
    pat = re.compile(r'(\w+)\s*=\s*ColorSchema\.parse\(\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'
                     r'"([0-9A-Fa-f]{6})",\s*"([^"]*)"', re.DOTALL)
    out = []
    for m in pat.finditer(txt):
        if m.group(1) not in names:
            continue
        out.append((m.group(1), tuple(int(m.group(i)) for i in (2, 3, 4, 5)), m.group(6), m.group(7)))
    return out


def to_arr(hexstr, swap):
    v = [int(hexstr[0:2], 16), int(hexstr[2:4], 16), int(hexstr[4:6], 16)]
    if swap:
        v = [v[2], v[1], v[0]]
    return np.array(v, dtype=np.int16)


def find(img, feat, swap):
    name, (x1, y1, x2, y2), main, offs = feat
    sub = img[y1:y2, x1:x2].astype(np.int16)
    m = (np.abs(sub - to_arr(main, swap)) <= TH).all(axis=2)
    pts = [(int(x), int(y)) for y, x in zip(*np.where(m))]
    parsed = []
    for tok in offs.split(','):
        if not tok:
            continue
        dx, dy, h = tok.split('|')
        parsed.append((int(dx), int(dy), to_arr(h.split('-')[0], swap)))
    for (x, y) in pts:
        ok = True
        for dx, dy, c in parsed:
            tx, ty = x + dx, y + dy
            if tx < 0 or ty < 0 or ty >= sub.shape[0] or tx >= sub.shape[1]:
                ok = False
                break
            if (np.abs(sub[ty, tx] - c) > TH).any():
                ok = False
                break
        if ok:
            return (x + x1, y + y1)
    return None


img = cv2.imread(SHOT)
print('shot', SHOT, img.shape)
feats = parse(FEATURE, {'TrainTroops', 'AttackButton', 'CommonDialog'})
for f in feats:
    for swap in (False, True):
        print('%-14s swap=%-5s -> %s' % (f[0], swap, find(img, f, swap)))
