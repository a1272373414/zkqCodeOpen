# -*- coding: utf-8 -*-
"""Migrate the original script's training-card features (`造XX` branches of 函数275a) to Kotlin.

Original script: cocfz-apk-test/doc/decrypt/awcocx_main.lua
Coordinate mapping (verified, see docs/复用优化方案20260919.md 11.3):
    region  (x1, y1, x2, y2) -> (y1, 719-x2, y2, 719-x1)
    offset  (dx, dy)         -> (dy, -dx)
Branch shape:
    多点找色界面 == "造亡灵" then
      intX, intY = findMultiColor(zbx1, zby1, zbx2, zby2, "699AC3-101010",
                                  "7|0|58DDFF-101010|0|-8|7BA1BB-101010|...", 0, 0.9)
with zbx1..zby2 = 26, 23, 290, 1279 (portrait 720x1280).

Emit: app/.../colorschema/colorpackage/mainbase/MainBaseTrainCardColors.kt
"""
import os
import re

LUA = r'D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, r"app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTrainCardColors.kt")

ZB = (26, 23, 290, 1279)  # portrait region used by the 造XX branches
W, H = 720, 1280
MAX_OFF = 12


def conv_region(r):
    x1, y1, x2, y2 = r
    nx1, ny1 = y1, 719 - x2
    nx2, ny2 = y2, 719 - x1
    nx1, nx2 = max(0, min(nx1, nx2)), min(1279, max(nx1, nx2))
    ny1, ny2 = max(0, min(ny1, ny2)), min(719, max(ny1, ny2))
    return nx1, ny1, nx2, ny2


def conv_offsets(s):
    toks = [t for t in s.split('|') if t]
    pts = []
    for i in range(0, len(toks) - 2, 3):
        dx, dy, col = toks[i], toks[i + 1], toks[i + 2].split('-')[0]
        if not re.fullmatch(r'-?\d+', dx) or not re.fullmatch(r'-?\d+', dy):
            break
        if not re.fullmatch(r'[0-9A-Fa-f]{6}', col):
            break
        pts.append((int(dy), -int(dx), col.upper()))
    return pts[:MAX_OFF]


def main():
    t = open(LUA, encoding='utf-8', errors='ignore').read()
    pat = re.compile(r'多点找色界面 == "(造[^"]+)"(.{0,300}?)findMultiColor\(\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*"([0-9A-Fa-f]{6})[^"]*"\s*,\s*"([^"]*)"', re.S)
    feats, bad = {}, []
    for m in pat.finditer(t):
        key = m.group(1)
        vals = [m.group(i).strip() for i in (3, 4, 5, 6)]
        if all(v.isdigit() for v in vals):
            reg = tuple(int(v) for v in vals)
        elif vals == ['zbx1', 'zby1', 'zbx2', 'zby2']:
            reg = ZB
        else:
            bad.append((key, vals))
            continue
        offs = conv_offsets(m.group(8))
        if not offs:
            bad.append((key, 'no offsets'))
            continue
        feats[key] = (conv_region(reg), m.group(7).upper(), offs)
    print('migrated %d features, skipped %d' % (len(feats), len(bad)))
    for k, v in bad:
        print('  SKIP', k, v)

    def prop(k):
        return k[1:] if k.startswith('造') else k

    lines = ['@file:Suppress("PropertyName")', '',
             'package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase', '',
             'import com.coc.zkqcode.jar.code.colorschema.ColorSchema', '',
             '/**',
             ' * Main-base TRAINING-CARD features migrated from the original script',
             ' * (cocfz-apk-test doc/decrypt/awcocx_main.lua, 函数275a branches "造XX").',
             ' * Region/offset conversion: see docs/复用优化方案20260919.md 11.3',
             ' *   region (x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1); offset (dx,dy) -> (dy, -dx)',
             ' * The original flow locates a troop by its own feature on the current picker page and',
             ' * flips the page (左/中/右) when not found, so no troop order/position is assumed.',
             ' */',
             'interface IMainBaseTrainCardColors {']
    for k in sorted(feats):
        lines.append('    val TrainCard%s: ColorSchema' % prop(k))
    lines += ['}', '', 'object MainBaseTrainCardColors : IMainBaseTrainCardColors {']
    for k in sorted(feats):
        (x1, y1, x2, y2), main, offs = feats[k]
        off_str = ','.join('%d|%d|%s' % o for o in offs)
        lines.append('    override val TrainCard%s = ColorSchema.parse(' % prop(k))
        lines.append('        %d, %d, %d, %d, "%s",' % (x1, y1, x2, y2, main))
        lines.append('        "%s",' % off_str)
        lines.append('        0, 0.9, "%s训练卡片(%s)"' % (prop(k), k))
        lines.append('    )')
    lines += ['}', '']
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
    print('wrote', OUT)


if __name__ == '__main__':
    main()
