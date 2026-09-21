# -*- coding: utf-8 -*-
"""验证来源 cocfz-apk-test 的"下兵逻辑"部署栏兵种色特征能否迁移到本项目。

做法：
1. 从 awcocx_main.lua 抽取部署栏兵种识别特征（主世界区域 (9,1,132,1279)、
   夜世界区域 (43,15,108,1279)），这些就是文档三份 md 里描述的"兵种色头"。
2. 用 docs/复用优化方案20260919.md 11.3 已验证的 90° 旋转映射转换到 1280x720 横屏：
     区域 (x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1)
     偏移 (dx,dy) -> (dy, -dx)
3. 用与 App 同源的逐字节匹配（threshold = 255*(1-similarity)，与 game_state.TH 一致）
   在真实部署栏截图上核对命中率。

用法：python validate_legacy_deploy_colors.py [shot1 shot2 ...]
  （默认检查 tools/captures/attack/live_bar_b1.png, ready1.png, ready2.png）
"""
import os
import re
import sys

import cv2
import numpy as np

import game_state  # 复用 hex_arr / ColorSchema 解析口径

LUA_PATH = r'D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua'

PORTRAIT_W, PORTRAIT_H = 720, 1280
LAND_W, LAND_H = 1280, 720

# 部署栏（战斗左下兵种槽）区域：主世界 / 夜世界
DEPLOY_REGIONS = {
    (9, 1, 132, 1279): 'MainDeploy',
    (43, 15, 108, 1279): 'BuilderDeploy',
}


def portrait_to_landscape_rect(x1, y1, x2, y2):
    lx1, lx2 = y1, y2
    ly1, ly2 = LAND_H - 1 - x2, LAND_H - 1 - x1
    lx1, lx2 = min(lx1, lx2), max(lx1, lx2)
    ly1, ly2 = min(ly1, ly2), max(ly1, ly2)
    return max(0, lx1), max(0, ly1), min(LAND_W - 1, lx2), min(LAND_H - 1, ly2)


def convert_offsets(raw):
    parts = [p for p in raw.split('|') if p != '']
    if len(parts) % 3 != 0:
        return None
    out = []
    for i in range(0, len(parts), 3):
        dx, dy, color = int(parts[i]), int(parts[i + 1]), parts[i + 2]
        out.append('%d|%d|%s' % (dy, -dx, color))
    return ','.join(out)


def extract_deploy_features():
    """抽取并去重部署栏兵种色特征，返回 game_state.parse_file 同格式的列表。"""
    text = open(LUA_PATH, encoding='utf-8').read()
    pat = re.compile(
        r'findMultiColor\((\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'
        r'"([0-9A-Fa-f]{6})-101010",\s*"([^"]*)",\s*(\d),\s*([\d.]+)\)')
    feats = []
    seen = set()
    for m in pat.finditer(text):
        x1, y1, x2, y2 = (int(m.group(i)) for i in (1, 2, 3, 4))
        if (x1, y1, x2, y2) not in DEPLOY_REGIONS:
            continue
        mainc = m.group(5)
        offsets = m.group(6)
        direction = int(m.group(7))
        sim = float(m.group(8))
        key = (mainc, offsets)
        if key in seen:
            continue
        seen.add(key)
        rx1, ry1, rx2, ry2 = portrait_to_landscape_rect(x1, y1, x2, y2)
        offs = convert_offsets(offsets)
        if offs is None:
            continue
        anchor = game_state.hex_arr(mainc)
        offlist = []
        for tok in offs.split(','):
            if not tok:
                continue
            dx, dy, h = tok.split('|')
            offlist.append((int(dx), int(dy), game_state.hex_arr(h)))
        tag = DEPLOY_REGIONS[(x1, y1, x2, y2)]
        name = '%s_%s' % (tag, mainc)
        feats.append((name, (rx1, ry1, rx2, ry2), anchor, offlist, sim))
    return feats


def collect_th(scan, anchor, offs, th):
    """与 App 同源：区域内找 anchor（逐通道 abs<=th），再校验全部偏移点。"""
    if scan is None or scan.size == 0:
        return None
    mask = (np.abs(scan.astype(np.int16) - anchor) <= th).all(axis=2)
    ys, xs = np.where(mask)
    for y, x in zip(ys, xs):
        ok = True
        for dx, dy, c in offs:
            tx, ty = x + dx, y + dy
            if tx < 0 or tx >= scan.shape[1] or ty < 0 or ty >= scan.shape[0]:
                ok = False
                break
            if (np.abs(scan[ty, tx].astype(np.int16) - c) > th).any():
                ok = False
                break
        if ok:
            return int(x), int(y)
    return None


# 命中色 -> (Kotlin 属性名, 中文注释)。其余命中色用 Legacy<hex> 兜底。
# 来源：awcocx_main.lua 部署栏识别函数（函数140a / 函数141a / 函数162a~166a / 公爵）。
NAME_MAP = {
    '809bb3': ('ReviveSpellAtDeploymentBar', '复苏法术（legacy 函数141a）'),
    '903858': ('QueenArcherLegacy', '女皇（legacy 函数163a，项目已有 QueenArcher，作补充 variant）'),
    '986e1f': ('PrinceAtDeploymentBar', '王子（legacy 函数164a）'),
    'bca584': ('TotemAtDeploymentBar', '图腾（legacy 函数141a）'),
    'cde4ea': ('DragonRiderAtDeploymentBar', '龙骑（legacy 函数140a）'),
    '6036e7': ('DragonAtDeploymentBar3', '飞龙（legacy 函数140a，项目已有 DragonAtDeploymentBar，作补充 variant）'),
    '4b0d3d': ('GrandWardenLegacy', '守卫（legacy 函数165a，项目已有 GrandWarden，作补充 variant）'),
    '1b1760': ('MinionPrinceLegacy', '公爵（legacy 函数，项目已有 MinionPrince/DragonDuke，作补充 variant）'),
}


def emit_kotlin(feat):
    name, (rx1, ry1, rx2, ry2), anchor, offlist, sim = feat
    mainc = '%02X%02X%02X' % (anchor[0], anchor[1], anchor[2])  # BGR hex（同 lua 书写顺序）
    offs = convert_offsets_from_list(offlist)
    prop, comment = NAME_MAP.get(mainc.lower(), ('Legacy%s' % mainc, 'legacy deploy bar (未命名)'))
    return (
        '    override val %s = ColorSchema.parse(\n'
        '        %d, %d, %d, %d, "%s", "%s", 0, %g, "%s"\n'
        '    )' % (prop, rx1, ry1, rx2, ry2, mainc, offs, sim, comment)
    )


def convert_offsets_from_list(offlist):
    out = []
    for dx, dy, c in offlist:
        out.append('%d|%d|%02X%02X%02X' % (dx, dy, c[0], c[1], c[2]))
    return ','.join(out)


def main():
    feats = extract_deploy_features()
    by_tag = {}
    for f in feats:
        by_tag.setdefault(f[0].split('_')[0], 0)
        by_tag[f[0].split('_')[0]] += 1
    print('抽取部署栏特征（去重后）共 %d 个：%s'
          % (len(feats), by_tag))

    shots = sys.argv[1:] or ['live_bar_b1', 'ready1', 'ready2']
    hit_feats = {}  # name -> (feat, first_hit_coord)
    for shot in shots:
        img = cv2.imread(os.path.join(os.path.dirname(__file__), 'captures', 'attack', '%s.png' % shot))
        if img is None:
            print('== %s: 截图缺失 ==' % shot)
            continue
        h, w = img.shape[:2]
        print('== %s (尺寸 %dx%d) ==' % (shot, w, h))
        hits = []
        for feat in feats:
            name, (rx1, ry1, rx2, ry2), a, o, sim = feat
            th = int(255 * (1 - sim) + 1e-6)
            rx1c, ry1c = min(rx1, w - 1), min(ry1, h - 1)
            rx2c, ry2c = min(rx2, w), min(ry2, h)
            if rx2c <= rx1c or ry2c <= ry1c:
                continue
            hit = collect_th(img[ry1c:ry2c, rx1c:rx2c], a, o, th)
            if hit:
                coord = '%d,%d' % (hit[0] + rx1c, hit[1] + ry1c)
                hits.append('%s@%s' % (name, coord))
                if name not in hit_feats:
                    hit_feats[name] = (feat, coord)
        print('  命中 %d/%d' % (len(hits), len(feats)))
        print('   ' + ' '.join(hits))

    print('\n===== 逐特征命中清单（去重后，跨所有截图）=====')
    for feat in feats:
        name = feat[0]
        mark = 'HIT ' + hit_feats[name][1] if name in hit_feats else 'miss'
        print('  %-22s %s' % (name, mark))

    print('\n===== 仍有效（HIT）特征的 ColorSchema.parse Kotlin =====')
    for name in sorted(hit_feats):
        feat, coord = hit_feats[name]
        print('// 命中于 %s' % coord)
        print(emit_kotlin(feat))
        print()


if __name__ == '__main__':
    main()
