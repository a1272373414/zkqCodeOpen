# -*- coding: utf-8 -*-
"""Live：点击两个"分页"坐标后各抓一张图，用特征命中数判断该页到底是哪一类（法术/攻城器/兵种）。"""
import os
import cv2

from game_state import (cap, tap, swipe, parse_file, find_first, page_of, panel_open,
                        _feat_train_troops, ensure_online)

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
CARDS = {f[0]: f for f in parse_file(KT)}

SPELLS = ['雷电', '冰冻', '冰障', '地震', '毒药', '疗伤', '弹跳', '急速', '狂暴',
          '镜像', '隐形', '回溯', '图腾', '愤怒法术', '骷髅', '蝙蝠', '复苏', '蔓生']
SIEGE = ['战车', '飞艇', '战球', '战营', '滚木', '烈焰', '钻机', '空中战车', '部队发射器']
TROOPS = ['野蛮', '弓箭', '巨人', '小偷', '炸弹', '气球', '法师', '天使', '飞龙', '龙宝', '皮卡',
          '雪怪', '矿工', '雷龙', '亡灵', '女巫', '学徒', '投手', '废墟女巫', '鲁伊', '冰人', '石头',
          '熔炉', '猎犬', '武神', '猎手', '野猪']

CLOSE_PANEL = (219, 139)
PIN = (120, 560, 1180, 560, 300)


def score(img, keys):
    got = [k for k in keys if find_first(img, [CARDS['TrainCard' + k]])]
    return len(got), got


def report(tag, img):
    s_n, s_k = score(img, SPELLS)
    g_n, g_k = score(img, SIEGE)
    t_n, t_k = score(img, TROOPS)
    print('%s: 法术特征 %d/18 | 攻城器特征 %d/9 | 兵种特征 %d/%d' % (tag, s_n, g_n, t_n, len(TROOPS)))
    print('    命中示例 法术%s 攻城器%s 兵种%s' % (s_k[:4], g_k[:4], t_k[:4]))


def main():
    ensure_online()
    img = cv2.imread(cap('idt0.png'))
    if page_of(img) == 'main_village':
        h = find_first(img, _feat_train_troops)
        if h:
            tap(h[1], h[2], dt=2.5)
    if panel_open(cv2.imread(cap('idt1.png'))):
        tap(*CLOSE_PANEL, dt=1.0)
    for coord in ((797, 420), (1126, 423)):
        tap(coord[0], coord[1], dt=1.5)
        img = cv2.imread(cap('idt_%d_%d.png' % coord))
        for _ in range(6):
            swipe(*PIN)
        img = cv2.imread(cap('idt_pin_%d_%d.png' % coord))
        report('点击(%d,%d)' % coord, img)
        tap(*CLOSE_PANEL, dt=1.0)


if __name__ == '__main__':
    main()
