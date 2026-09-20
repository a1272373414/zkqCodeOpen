# -*- coding: utf-8 -*-
"""探测法术/攻城器面板里哪个 y 能真正滑动列表（每组：钉到最左 → 在若干 y 上左滑并比较画面变化）。"""
import sys
import cv2
import numpy as np

from game_state import (cap, tap, swipe, find_first, page_of, panel_open, parse_file,
                        _feat_train_troops, F_TRAINING, ensure_online)

VERIFY = {f[0]: f for f in parse_file(F_TRAINING, {"TrainLighteningSpell", "TrainSiegeMachine"})}
TABS = {'spell': ((797, 420), 'TrainLighteningSpell'), 'siege': ((1126, 423), 'TrainSiegeMachine')}
CLOSE_PANEL = (219, 139)


def diff(a, b):
    return float(np.abs(a.astype(np.int16) - b.astype(np.int16)).mean())


def main():
    tag = sys.argv[1] if len(sys.argv) > 1 else 'siege'
    tab, verify = TABS[tag]
    ensure_online()
    img = cv2.imread(cap('psy0.png'))
    if page_of(img) == 'main_village':
        h = find_first(img, _feat_train_troops)
        if h:
            tap(h[1], h[2], dt=2.5)
    if panel_open(cv2.imread(cap('psy1.png'))):
        tap(*CLOSE_PANEL, dt=1.0)
    tap(tab[0], tab[1], dt=1.5)
    print('picker ok =', find_first(cv2.imread(cap('psy2.png')), [VERIFY[verify]]) is not None)
    for y in (500, 520, 560, 600, 630, 650):
        for _ in range(5):
            swipe(120, y, 1180, y, 300)          # 先钉回最左
        before = cv2.imread(cap('psy_left.png'))
        swipe(1180, y, 120, y, 400)              # 再左滑一次
        after = cv2.imread(cap('psy_next.png'))
        print('y=%d 左滑后画面变化=%.2f %s' % (y, diff(before, after),
                                            '→ 有效' if diff(before, after) > 1.0 else ''))
        tap(*CLOSE_PANEL, dt=1.0)
        tap(tab[0], tab[1], dt=1.5)


if __name__ == '__main__':
    main()
