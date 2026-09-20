# -*- coding: utf-8 -*-
"""进攻流程用的特征（只在 Python 工具里用，不进 App）：

- SEARCH_OPPONENTS：进攻菜单里的黄色「搜索对手」按钮（从 准备进攻页1 派生，紧区域）
- IN_BATTLE：用 App 里已验证命中的部署栏泛化特征判断"已进入部署阶段"
"""
import os
import numpy as np

from game_state import parse_file

PKG = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage")


def bgr(h):
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], dtype=np.int16)


def parse_hex_offsets(s):
    out = []
    for tok in s.split(','):
        if not tok:
            continue
        dx, dy, c = tok.split('|')
        out.append((int(dx), int(dy), bgr(c)))
    return out


# 搜索对手（黄底按钮）：紧区域 = 按钮所在屏位
SEARCH_OPPONENTS = (
    'SearchOpponentsNew', (140, 500, 340, 640), bgr('5BFFFF'),
    parse_hex_offsets('-3|-2|57FFFF,0|-5|5BFFFF,0|-2|59FFFF,-3|1|57FFFF,3|-5|5CFFFF,0|1|59FFFF,'
                      '-15|7|57EAFF,-3|-5|58FFFF,-6|-2|56FFFF,-6|1|56FDFF,-6|-5|57FFFF,-51|-5|2CADF9'),
)


def bar_feats():
    """部署栏里 App 侧已验证会命中的泛化特征（用于判断"已进入部署阶段"）。"""
    feats = parse_file(os.path.join(PKG, r'mainbase\MainBaseAttackColors.kt'))
    want = {'TroopColorAtDeploymentBar', 'SpellColorAtDeploymentBar', 'SpecialTroopColorAtDeploymentBar',
            'EndBattle', 'BattlePage', 'WaitForBattle'}
    return {f[0]: f for f in feats if f[0] in want}
