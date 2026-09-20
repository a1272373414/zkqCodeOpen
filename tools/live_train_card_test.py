# -*- coding: utf-8 -*-
"""Live验证：用迁移后的训练卡片特征在选兵面板定位并点击，核对是否落在正确卡片上。

沿用 TrainTroops.kt 的新流程：钉回最左页 → 逐页「当前页找特征 → 找到点击 / 找不到翻页」。
每个兵种点击前后各裁一张卡片图做像素差，差值明显说明点击改变了该卡片（队列计数变化）。
"""
import os
import sys
import time
import cv2
import numpy as np

from game_state import (cap, tap, swipe, parse_file, find_first, panel_open, OUT,
                        _feat_train_troops, _feat_training, page_of)

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
FEATS = {f[0]: f for f in parse_file(KT)}

# 显示名 -> 特征属性名（与 TrainTroops.kt 的 TRAIN_CARD 一致，取子集做验证）
WANT = {'弓箭手': 'TrainCard弓箭', '哥布林': 'TrainCard小偷', '气球兵': 'TrainCard气球',
        '亡灵': 'TrainCard亡灵', '女巫': 'TrainCard女巫'}
PIN_RIGHT = (120, 560, 1180, 560, 300)
NEXT_PAGE = (1180, 560, 120, 560, 400)


def card_crop(img, x, y):
    return img[max(0, y - 65):y + 65, max(0, x - 65):x + 65].astype(np.int16)


def main():
    img = cv2.imread(cap('lt0.png'))
    if page_of(img) == 'main_village':
        h = find_first(img, _feat_train_troops)
        print('主村庄：训练部队按钮特征 =', h)
        if h:
            tap(h[1], h[2], dt=2.5)
        img = cv2.imread(cap('lt1.png'))
    if not panel_open(img):
        print('训练页已打开，点 (891,234) 打开选兵面板')
        tap(891, 234, dt=1.6)
        img = cv2.imread(cap('lt2.png'))
    print('选兵面板 panel_open =', panel_open(img), ' training =', find_first(img, _feat_training) is not None)
    if not panel_open(img):
        print('仍无法打开选兵面板，终止')
        return
    for _ in range(4):
        swipe(*PIN_RIGHT)
    pending = dict(WANT)
    found = {}
    for page in range(1, 4):
        img = cv2.imread(cap('lt_page%d.png' % page))
        for name, prop in list(pending.items()):
            h = find_first(img, [FEATS[prop]])
            if not h:
                continue
            x, y = h[1], h[2]
            found[name] = (page, x, y)
            before = card_crop(img, x, y)
            for _ in range(3):
                tap(x, y, dt=0.45)
            idx = list(WANT).index(name)
            after = card_crop(cv2.imread(cap('lt_after_%d.png' % idx)), x, y)
            diff = int(np.abs(after - before).mean() * 100)
            print('page%d %-6s hit (%d,%d) tapped x3, card pixel diff=%d %s'
                  % (page, name, x, y, diff, 'OK' if diff > 3 else '(no change?)'))
            pending.pop(name)
        if not pending:
            break
        swipe(*NEXT_PAGE)
    if pending:
        print('未定位到：' + '、'.join(pending))
    print('结果：', found)


if __name__ == '__main__':
    main()
