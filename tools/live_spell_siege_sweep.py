# -*- coding: utf-8 -*-
"""Live：全量校验法术（18）/ 攻城器（9）的训练卡片特征命中情况。

前置：先跑 clear_queues.py（队列满会导致卡片变灰、特征不命中）。
用法：python live_spell_siege_sweep.py
"""
import os
import cv2

from game_state import (cap, tap, swipe, parse_file, find_first, page_of, panel_open,
                        _feat_train_troops, F_TRAINING, ensure_online)

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
CARDS = {f[0]: f for f in parse_file(KT)}
VERIFY = {f[0]: f for f in parse_file(F_TRAINING, {"TrainLighteningSpell", "TrainSiegeMachine"})}

# 法术 18 种（原脚本「造XX」键）
SPELLS = ['雷电', '冰冻', '冰障', '地震', '毒药', '疗伤', '弹跳', '急速', '狂暴',
          '镜像', '隐形', '回溯', '图腾', '愤怒法术', '骷髅', '蝙蝠', '复苏', '蔓生']
# 攻城器 9 种
SIEGE = ['战车', '飞艇', '战球', '战营', '滚木', '烈焰', '钻机', '空中战车', '部队发射器']

TAB_SPELLS = (797, 420)
TAB_SIEGE = (1126, 423)
CLOSE_PANEL = (219, 139)
PIN = (120, 560, 1180, 560, 300)
NEXT = (1180, 560, 120, 560, 400)


def sweep(label, keys, tag):
    pend = list(keys)
    for _ in range(6):
        swipe(*PIN)
    for page in range(1, 5):
        img = cv2.imread(cap('sw_%s_p%d.png' % (tag, page)))
        hits = []
        for k in list(pend):
            h = find_first(img, [CARDS['TrainCard' + k]])
            if h:
                hits.append('%s@%d,%d' % (k, h[1], h[2]))
                pend.remove(k)
        print('%s page%d: %d/%d  %s' % (label, page, len(hits), len(keys), ' '.join(hits)))
        if not pend:
            break
        swipe(*NEXT)
    if pend:
        print('%s 未命中(%d): %s' % (label, len(pend), '、'.join(pend)))


def main():
    ensure_online()
    img = cv2.imread(cap('sw0.png'))
    if page_of(img) == 'main_village':
        h = find_first(img, _feat_train_troops)
        if h:
            tap(h[1], h[2], dt=2.5)
    if panel_open(cv2.imread(cap('sw1.png'))):
        tap(*CLOSE_PANEL, dt=1.0)
    print('=== 法术 ===')
    tap(*TAB_SPELLS, dt=1.5)
    print('picker ok =', find_first(cv2.imread(cap('sw2.png')), [VERIFY['TrainLighteningSpell']]) is not None)
    sweep('法术', SPELLS, 'spell')
    tap(*CLOSE_PANEL, dt=1.0)
    print('=== 攻城器 ===')
    tap(*TAB_SIEGE, dt=1.5)
    print('picker ok =', find_first(cv2.imread(cap('sw3.png')), [VERIFY['TrainSiegeMachine']]) is not None)
    sweep('攻城器', SIEGE, 'siege')


if __name__ == '__main__':
    main()
