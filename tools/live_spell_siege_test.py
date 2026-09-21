# -*- coding: utf-8 -*-
"""Live：验证法术/攻城器改为「按特征定位」后的效果（对应 TrainTroops.kt 的 SPELL_PLAN / SIEGE_PLAN）。

前置：先跑 clear_queues.py 清空队列（避免卡片变灰影响定位）。
用法：python live_spell_siege_test.py
"""
import os
import cv2
import numpy as np

from game_state import (cap, tap, swipe, parse_file, find_first, page_of, panel_open,
                        _feat_train_troops, F_TRAINING, ensure_online)

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
CARDS = {f[0]: f for f in parse_file(KT)}
VERIFY = {f[0]: f for f in parse_file(F_TRAINING, {"TrainLighteningSpell", "TrainSiegeMachine"})}

TAB_SPELLS = (797, 420)
TAB_SIEGE = (1126, 423)
CLOSE_PANEL = (219, 139)
PIN = (120, 560, 1180, 560, 300)
NEXT = (1180, 560, 120, 560, 400)

SPELLS = [('闪电法术', 'TrainCard雷电', 8)]
SIEGE = [('攻城战车', 'TrainCard战车', 1), ('战斗飞艇', 'TrainCard飞艇', 1), ('攻城气球', 'TrainCard战球', 1),
         ('空中战车', 'TrainCard空中战车', 1), ('钻地机器', 'TrainCard钻机', 1)]


def diff_at(img_a, img_b, x, y):
    a = img_a[max(0, y - 65):y + 65, max(0, x - 65):x + 65].astype(np.int16)
    b = img_b[max(0, y - 65):y + 65, max(0, x - 65):x + 65].astype(np.int16)
    if a.shape != b.shape or a.size == 0:
        return 0
    return int(np.abs(b - a).mean() * 100)


def sweep(label, plan, tag):
    pend = list(plan)
    for _ in range(6):
        swipe(*PIN)
    for page in range(1, 5):
        img = cv2.imread(cap('ss_%s_p%d.png' % (tag, page)))
        for name, prop, times in list(pend):
            h = find_first(img, [CARDS[prop]])
            if not h:
                continue
            for _ in range(times):
                tap(h[1], h[2], dt=0.45)
            after = cv2.imread(cap('ss_%s_after_%d.png' % (tag, plan.index((name, prop, times)))))
            print('  %s page%d %s hit(%d,%d) tap x%d, card diff=%d'
                  % (label, page, name, h[1], h[2], times, diff_at(img, after, h[1], h[2])))
            pend.remove((name, prop, times))
        if not pend:
            break
        swipe(*NEXT)
    if pend:
        print('  %s 未定位：%s' % (label, '、'.join(p[0] for p in pend)))


def main():
    ensure_online()
    img = cv2.imread(cap('ss0.png'))
    if page_of(img) in ('main_village', 'night_village'):
        h = find_first(img, _feat_train_troops)
        if h:
            tap(h[1], h[2], dt=2.5)
    if panel_open(cv2.imread(cap('ss1.png'))):
        tap(*CLOSE_PANEL, dt=1.0)
    print('法术：')
    tap(*TAB_SPELLS, dt=1.5)
    print('  spell picker ok =', find_first(cv2.imread(cap('ss2.png')), [VERIFY['TrainLighteningSpell']]) is not None)
    sweep('法术', SPELLS, 'spell')
    tap(*CLOSE_PANEL, dt=1.0)
    print('攻城器：')
    tap(*TAB_SIEGE, dt=1.5)
    print('  siege picker ok =', find_first(cv2.imread(cap('ss3.png')), [VERIFY['TrainSiegeMachine']]) is not None)
    sweep('攻城器', SIEGE, 'siege')


if __name__ == '__main__':
    main()
