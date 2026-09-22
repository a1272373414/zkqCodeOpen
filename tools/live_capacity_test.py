# -*- coding: utf-8 -*-
"""Live check of MainBaseArmyPlan.readTroopHousingTotal() on emulator-5556.

Reproduces the exact runtime state (training page with the 圣水兵 troop picker open)
and runs the SAME recognition as the app: the calibrated CAPACITY_REGION
(580,153,666,179) + PixelFontOcr's two-phase matcher (same-size, then scaled) with
grayMin=160 / maxSaturation=70, then "last number group" parsing.

Usage: python tools/live_capacity_test.py
"""
import os
import re
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import game_state as gs                                    # noqa: E402
from calibrate_capacity_region import parse_glyphs, recognize_lines   # noqa: E402

TAB_TROOPS = (891, 234)                # TrainTroops.kt: 圣水兵 tab
CAPACITY_REGION = (580, 153, 666, 179)  # MainBaseArmyPlan.CAPACITY_REGION

GLYPHS = parse_glyphs()


def read_capacity(rgb):
    """Return (text, total, per_group_texts) using the app's region + matcher + parse."""
    x0, y0, x1, y1 = CAPACITY_REGION
    sub = rgb[y0:y1, x0:x1]
    lines = recognize_lines(sub, 160, 255, 70, GLYPHS)
    groups = [t for (_, _, _, _, t, _) in lines]
    text = ''.join(groups)
    nums = [int(m) for t in groups for m in re.findall(r'\d+', t)]
    return text, (nums[-1] if nums else None), groups


def main():
    gs.ensure_online()
    gs.close_dialogs()
    img = gs.cv2.imread(gs.cap('cap0.png'))
    page = gs.page_of(img)
    print('起始页面 =', page)

    if page == 'main_village':
        print('主村庄 → 点「训练部队」(52,521)')
        gs.tap(52, 521, dt=2.5)
        img = gs.cv2.imread(gs.cap('cap1.png'))
        page = gs.page_of(img)
        print('打开训练页后页面 =', page)

    if page != 'training':
        print('非训练页 → recover_to_training()')
        if not gs.recover_to_training():
            print('无法进入训练页，终止')
            return

    print('点「圣水兵」分页 (891,234) 打开选兵面板')
    gs.tap(*TAB_TROOPS, dt=1.5)
    shot = gs.cap('cap_capacity.png')

    rgb = np.asarray(Image.open(shot).convert('RGB'))
    text, total, groups = read_capacity(rgb)
    print('-' * 50)
    print('CAPACITY_REGION =', CAPACITY_REGION)
    print('分组文本 =', groups)
    print('整串     =', repr(text))
    print('识别总数 =', total)
    print('截图     =', shot)
    print('(真值请人工核对截图中的 "已用/总")')


if __name__ == '__main__':
    main()
