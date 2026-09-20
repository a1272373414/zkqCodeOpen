# -*- coding: utf-8 -*-
"""Live：按新流程把选兵面板从最左页一路左滑，逐页报告各兵种特征命中（验证自适应翻页 + 活动兵不影响原有兵种）。

用法：python live_picker_sweep.py [页数上限]
"""
import os
import sys
import cv2

from game_state import cap, swipe, parse_file, find_first, OUT

KT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                  r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")
FEATS = {f[0]: f for f in parse_file(KT)}

# 显示名 -> 特征属性名（抽取圣水兵/黑油兵/超级兵代表，覆盖三页）
WANT = [('弓箭手', 'TrainCard弓箭'), ('哥布林', 'TrainCard小偷'), ('气球兵', 'TrainCard气球'),
        ('亡灵', 'TrainCard亡灵'), ('女巫', 'TrainCard女巫'), ('野猪骑士', 'TrainCard野猪'),
        ('隐秘哥布林', 'TrainCard超偷'), ('超级大雪怪', 'TrainCard超怪')]

PIN = (120, 560, 1180, 560, 300)
NEXT = (1180, 560, 120, 560, 400)


def main():
    max_pages = int(sys.argv[1]) if len(sys.argv) > 1 else 6
    for _ in range(max_pages):
        swipe(*PIN)
    remaining = dict(WANT)
    for page in range(1, max_pages + 1):
        img = cv2.imread(cap('sweep_%d.png' % page))
        hits = []
        for name, prop in list(remaining.items()):
            h = find_first(img, [FEATS[prop]])
            if h:
                hits.append('%s@%d,%d' % (name, h[1], h[2]))
                remaining.pop(name)
        print('page%d: %s' % (page, ' '.join(hits) if hits else '(无)'))
        if not remaining:
            print('全部定位完成于第 %d 页' % page)
            break
        swipe(*NEXT)
    if remaining:
        print('未定位：' + '、'.join(remaining))


if __name__ == '__main__':
    main()
