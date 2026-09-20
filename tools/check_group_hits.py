# -*- coding: utf-8 -*-
"""Live：打开某类选兵面板 → 钉到最左页 → **逐页左滑**，报告该类全部特征的命中位置。

用法：python check_group_hits.py [spell|siege|troops]

面板是否打开用"该类的特征命中数"判断（比旧的单点校验特征可靠：列表滚动后单点特征会失效）。
"""
import os
import sys
import cv2

from game_state import (cap, tap, swipe, parse_file, find_first, page_of, panel_open,
                        _feat_train_troops, close_dialogs, ensure_online, F_TRAINING)

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
CARDS = {f[0]: f for f in parse_file(KT)}

GROUPS = {
    'spell': (['雷电', '冰冻', '冰障', '地震', '毒药', '疗伤', '弹跳', '急速', '狂暴',
               '镜像', '隐形', '回溯', '图腾', '愤怒法术', '骷髅', '蝙蝠', '复苏', '蔓生'], (797, 420)),
    'siege': (['战车', '飞艇', '战球', '战营', '滚木', '烈焰', '钻机', '空中战车', '部队发射器'], (1126, 423)),
    'troops': (['野蛮', '弓箭', '巨人', '小偷', '炸弹', '气球', '法师', '天使', '飞龙', '龙宝', '皮卡',
                '雪怪', '矿工', '雷龙', '亡灵', '女巫', '学徒', '投手', '废墟女巫', '鲁伊', '冰人', '石头',
                '熔炉', '猎犬', '武神', '猎手', '野猪'], (891, 234)),
}

PIN = (120, 560, 1180, 560, 300)
NEXT = (1180, 520, 120, 520, 400)


def group_hits(img, keys):
    return [k for k in keys if find_first(img, [CARDS['TrainCard' + k]])]


def open_group(tab, keys):
    for _ in range(3):
        ensure_online()
        close_dialogs()
        img = cv2.imread(cap('cg0.png'))
        if page_of(img) == 'main_village':
            h = find_first(img, _feat_train_troops)
            if h:
                tap(h[1], h[2], dt=2.5)
        if panel_open(cv2.imread(cap('cg1.png'))):
            tap(219, 139, dt=1.0)
        tap(tab[0], tab[1], dt=1.8)
        for _ in range(6):          # 钉回最左页后再判断该分类是否打开
            swipe(*PIN)
        if len(group_hits(cv2.imread(cap('cg2.png')), keys)) >= 3:
            return True
    return False


def main():
    tag = sys.argv[1] if len(sys.argv) > 1 else 'siege'
    keys, tab = GROUPS[tag]
    if not open_group(tab, keys):
        print('无法打开 %s 选兵面板' % tag)
        return
    print('=== %s：面板已打开，钉回最左页并逐页左滑 ===' % tag)
    for _ in range(6):
        swipe(*PIN)
    pend = list(keys)
    for page in range(1, 6):
        img = cv2.imread(cap('gsw_%s_p%d.png' % (tag, page)))
        hits = []
        for k in list(pend):
            h = find_first(img, [CARDS['TrainCard' + k]])
            if h:
                hits.append('%s@%d,%d' % (k, h[1], h[2]))
                pend.remove(k)
        print('page%d: 命中 %d/%d  %s' % (page, len(hits), len(keys), ' '.join(hits)))
        if not pend:
            print('全部命中，共 %d 页' % page)
            break
        swipe(*NEXT)
    if pend:
        print('未命中(%d): %s' % (len(pend), '、'.join(pend)))


if __name__ == '__main__':
    main()
