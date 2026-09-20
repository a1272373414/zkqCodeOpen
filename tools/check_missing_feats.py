# -*- coding: utf-8 -*-
"""诊断：在已抓取的选兵页截图上，报告法术/攻城器里"未命中"的特征键（ASCII 安全输出）。"""
import os
import cv2

from game_state import parse_file, find_first

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
CARDS = {f[0]: f for f in parse_file(KT)}

SPELLS = ['雷电', '冰冻', '冰障', '地震', '毒药', '疗伤', '弹跳', '急速', '狂暴',
          '镜像', '隐形', '回溯', '图腾', '愤怒法术', '骷髅', '蝙蝠', '复苏', '蔓生']
SIEGE = ['战车', '飞艇', '战球', '战营', '滚木', '烈焰', '钻机', '空中战车', '部队发射器']
PAGES = {'spell': [1, 2], 'siege': [1, 2]}

for tag, keys in (('spell', SPELLS), ('siege', SIEGE)):
    print('== %s ==' % tag)
    for k in keys:
        where = []
        for pg in PAGES[tag]:
            p = 'captures/training/sw_%s_p%d.png' % (tag, pg)
            if not os.path.exists(p):
                continue
            img = cv2.imread(p)
            h = find_first(img, [CARDS['TrainCard' + k]])
            if h:
                where.append('pg%d(%d,%d)' % (pg, h[1], h[2]))
        if not where:
            print('  MISS %s' % k.encode('unicode_escape').decode())
        else:
            print('  OK   %s %s' % (k.encode('unicode_escape').decode(), ' '.join(where)))
