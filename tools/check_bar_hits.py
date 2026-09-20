# -*- coding: utf-8 -*-
"""检查现有特征在"战斗部署栏"截图上的命中情况（任务 B 基线）。

用法：python check_bar_hits.py [attack1]
"""
import os
import sys
import cv2

from game_state import parse_file, find_first

PKG = r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage"
FILES = {
    '部署栏(现用)': os.path.join(PKG, r"mainbase\MainBaseAttackColors.kt"),
    '圣水兵配置': os.path.join(PKG, r"mainbase\MainBaseTroopColors.kt"),
    '黑油兵配置': os.path.join(PKG, r"mainbase\MainBaseBlackElixirTroopColors.kt"),
    '超级兵配置': os.path.join(PKG, r"mainbase\MainBaseSuperTroopColors.kt"),
}
KEYS = ('AtDeploymentBar', 'King', 'Queen', 'Warden', 'Champion', 'DragonDuke')


def main():
    shot = sys.argv[1] if len(sys.argv) > 1 else 'attack1'
    img = cv2.imread('captures/attack/%s.png' % shot)
    print('== %s ==' % shot)
    for tag, path in FILES.items():
        feats = parse_file(path)
        if tag == '部署栏(现用)':
            feats = [f for f in feats if any(k in f[0] for k in KEYS)]
        else:
            feats = [f for f in feats if f[0].startswith('MainBase')]
        hits = []
        for f in feats:
            h = find_first(img, [f])
            if h:
                hits.append('%s@%d,%d' % (h[0], h[1], h[2]))
        print('  %s: %d/%d  %s' % (tag, len(hits), len(feats), ' '.join(hits)))


if __name__ == '__main__':
    main()
