# -*- coding: utf-8 -*-
"""
将原 cocfz-apk-test 脚本（doc/decrypt/awcocx_main.lua）中的多点找色特征
迁移为 zkqCodeOpen 的 ColorSchema.parse(...) Kotlin 代码。

关键差异（已用真实截图逐像素验证，见 docs/复用优化方案20260919.md 第五节）：

1. 颜色字节序：两边都是 BGR(0xBBGGRR)，parseBgrToRgb 会转成 RGB。无需改色值。
2. 偏移点分隔符：原脚本全部字段用 "|"，zkqCodeOpen 点内 "|"、点间 ","。
3. 坐标系：原脚本是设备竖屏原始缓冲区 720x1280（屏幕宽=720/屏幕高=1280），
   而 zkqCodeOpen 是横屏 1280x720。必须做 90° 旋转映射：

       lx = py
       ly = 719 - px

   偏移向量同样要旋转：

       (dx, dy) -> (dy, -dx)

   矩形 (x1,y1,x2,y2) 转换后要重新取 min/max 并 clamp：
       rect = (y1, 719-x2, y2, 719-x1) ∩ [0,1279]x[0,719]

用法：
    python tools/migrate_lua_colors.py                # 输出到 stdout
    python tools/migrate_lua_colors.py -o out.txt     # 输出到文件
"""

import argparse
import re
import sys

LUA_PATH = r'D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua'

PORTRAIT_W = 720
PORTRAIT_H = 1280
LAND_W = 1280
LAND_H = 720

# 函数275a 里的全屏搜索区域局部变量
ROW_VARS = {'zbx1': 26, 'zby1': 23, 'zbx2': 290, 'zby2': 1279}

# 需要迁移的界面 -> (Kotlin 属性名, 目标文件, English comment)
INTERFACE_TARGETS = {
    '放弃按钮':    ('GiveUpButton', 'mainbase/MainBaseAttackColors.kt',
                'Give up / surrender button shown during a battle'),
    '竞赛界面':    ('ClanGamesEntry', 'mainbase/MainBaseAttackColors.kt',
                'Clan games (weekend event) entry'),
    '胜利之星':    ('VictoryStar', 'mainbase/MainBaseAttackColors.kt',
                'Victory star on the battle result screen'),
    '英雄殿堂界面': ('HeroHallEntry', 'mainbase/MainBaseHeroHallColors.kt',
                'Hero hall entry'),
    '装备界面':    ('EquipmentButton', 'mainbase/MainBaseHeroHallColors.kt',
                'Equipment panel entry'),
    '通用对话框':   ('CommonDialog', 'FeatureColors.kt',
                'Generic popup dialog'),
    '左下角回营':   ('BottomLeftReturnToCamp', 'FeatureColors.kt',
                'Return-to-camp button at the lower left'),
    '回营主':     ('ReturnToCampMain', 'FeatureColors.kt',
                'Main village return-to-camp confirmation'),
    '回营夜':     ('ReturnToCampBuilder', 'FeatureColors.kt',
                'Builder base return-to-camp confirmation'),
    '都城界面':    ('ClanCapitalEntry', 'clancapital/ClanCapitalTutorialColors.kt',
                'Clan capital entry'),
    '退出部落':    ('LeaveClanButton', 'mainbase/MainBaseClanColors.kt',
                'Leave clan button'),
    '彩部落图标':   ('ColorfulClanIcon', 'mainbase/MainBaseClanColors.kt',
                'Colorful clan banner icon (player is in a clan)'),
    '灰部落图标':   ('GrayClanIcon', 'mainbase/MainBaseClanColors.kt',
                'Grayed-out clan banner icon (player is not in a clan)'),
    '个人信息':    ('PlayerProfileButton', 'UIColors.kt',
                'Player profile entry'),
    '军队界面':    ('ArmyButton', 'mainbase/MainBaseTraining.kt',
                'Army panel entry'),
    '红x':      ('RedX', 'UIColors.kt',
                'Red X close button (9 variants in the legacy script)'),
}

# 都城军队/法术特征 -> Kotlin 属性名
CAPITAL_TARGETS = {
    '都城军队_超级巨人': 'CapitalSuperGiant',
    '都城军队_超级法师': 'CapitalSuperWizard',
    '都城军队_超级野蛮人': 'CapitalSuperBarbarian',
    '都城军队_隐秘弓箭手': 'CapitalSneakyArcher',
    '都城军队_野蛮人攻城槌': 'CapitalBattleRam',
    '都城军队_超级矿工': 'CapitalSuperMiner',
    '都城军队_野猪偷袭队': 'CapitalHogRaider',
    '都城军队_疗伤法术': 'CapitalHealingSpell',
    '都城军队_雷电法术': 'CapitalLightningSpell',
    '都城军队_冰冻法术': 'CapitalFreezeSpell',
    '都城军队_急速法术': 'CapitalHasteSpell',
    '都城军队_弹跳法术': 'CapitalJumpSpell',
    '都城军队_骷髅法术': 'CapitalSkeletonSpell',
}


def portrait_to_landscape_rect(x1, y1, x2, y2):
    """竖屏原始缓冲矩形 -> 横屏游戏矩形。"""
    lx1, lx2 = y1, y2
    ly1, ly2 = LAND_H - 1 - x2, LAND_H - 1 - x1
    lx1, lx2 = min(lx1, lx2), max(lx1, lx2)
    ly1, ly2 = min(ly1, ly2), max(ly1, ly2)
    lx1 = max(0, lx1)
    ly1 = max(0, ly1)
    lx2 = min(LAND_W - 1, lx2)
    ly2 = min(LAND_H - 1, ly2)
    return lx1, ly1, lx2, ly2


def convert_offsets(raw):
    """原脚本 "dx|dy|c|dx|dy|c|..." -> 横屏 "dx|dy|c,dx|dy|c,..."。"""
    parts = [p for p in raw.split('|') if p != '']
    if len(parts) % 3 != 0:
        return None
    out = []
    for i in range(0, len(parts), 3):
        dx, dy, color = int(parts[i]), int(parts[i + 1]), parts[i + 2]
        out.append('%d|%d|%s' % (dy, -dx, color))
    return ','.join(out)


def parse_function_275a(text):
    start = text.index('_ENV["函数275a"] = function')
    rest = text[start + 10:]
    nxt = re.search(r'\n  _ENV\["函数\d+a"\] = function', rest)
    body = rest[:nxt.start()] if nxt else rest

    def parse_args(args):
        parts = [p.strip() for p in args.split(',') if p.strip()]
        if len(parts) < 8:
            return None
        try:
            rect = tuple(ROW_VARS.get(p, int(p)) for p in parts[:4])
            return {
                'rect': rect,
                'main': parts[4].strip().strip('"'),
                'offsets': parts[5].strip().strip('"'),
                'direction': int(parts[6]),
                'similarity': float(parts[7]),
            }
        except (ValueError, KeyError):
            return None

    # 每个分支: 主特征 + 后续 if intX == -1 then 的兜底特征
    header_re = re.compile(
        r'(?:if|elseif) 多点找色界面 == "([^"]+)"(?: or 多点找色界面 == "([^"]+)")? then')
    call_re = re.compile(r'findMultiColor\(([^)]*)\)', re.S)
    fallback_re = re.compile(
        r'if\s+intX\s*==\s*-1[^\n]*then\s*\n\s*(?:intX,\s*intY\s*=\s*)?'
        r'findMultiColor\(([^)]*)\)', re.S)

    headers = list(header_re.finditer(body))
    result = {}
    stats = []
    for i, m in enumerate(headers):
        name = m.group(1)
        seg_end = headers[i + 1].start() if i + 1 < len(headers) else len(body)
        seg = body[m.end():seg_end]
        all_calls = call_re.findall(seg)
        fallbacks = fallback_re.findall(seg)
        primary = all_calls[0] if all_calls else None
        calls = []
        if primary is not None:
            calls.append(primary)
            # 主特征之后的兜底（按出现顺序）
            calls.extend(fallbacks)
        parsed = [p for p in (parse_args(c) for c in calls) if p]
        if parsed and name not in result:
            result[name] = parsed
            stats.append((name, len(all_calls), len(fallbacks), len(parsed)))
    return result, stats


def parse_capital_features(text):
    marker = '_ENV["特征"] = {'
    idx = text.index(marker)
    # 从表头之后开始，避免把 _ENV["特征"] 自身当成第一个条目
    seg = text[idx + len(marker):idx + len(marker) + 400000]
    # 表结束：第一个 "\n  }" 顶格缩进
    end = re.search(r'\n  \}', seg)
    if end:
        seg = seg[:end.start()]
    # 键有两种写法：["xxx"] = {...}  或  xxx = {...}（后者需在行首，避免把表头当成条目）
    entry_re = re.compile(
        r'(?:\s*\["([^"]+)"\]|\n\s*([^\s\[\]{}",=]+))\s*=\s*\{([^}]*)\}')
    out = {}
    for m in entry_re.finditer(seg):
        key = m.group(1) or m.group(2)
        vals = [v.strip().strip('"') for v in m.group(3).split(',') if v.strip()]
        if len(vals) < 8:
            continue
        try:
            out[key] = {
                'rect': (int(vals[0]), int(vals[1]), int(vals[2]), int(vals[3])),
                'main': vals[4],
                'offsets': vals[5],
                'direction': int(vals[6]),
                'similarity': float(vals[7]),
            }
        except ValueError:
            continue
    return out


def emit_kotlin(prop, feat, comment=None, indent='    '):
    x1, y1, x2, y2 = portrait_to_landscape_rect(*feat['rect'])
    offsets = convert_offsets(feat['offsets'])
    sim = ('%g' % feat['similarity']) if feat['similarity'] != int(feat['similarity']) \
        else '%d.0' % int(feat['similarity'])
    lines = []
    if comment:
        lines.append('%s// %s' % (indent, comment))
    lines.append('%soverride val %s = ColorSchema.parse(' % (indent, prop))
    lines.append('%s    %d, %d, %d, %d, "%s",' % (indent, x1, y1, x2, y2, feat['main']))
    lines.append('%s    "%s",' % (indent, offsets))
    lines.append('%s    %d, %s, "%s"' % (indent, feat['direction'], sim,
                                         feat['name'] if 'name' in feat else prop))
    lines.append('%s)' % indent)
    return '\n'.join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('-o', '--out', default=None)
    args = ap.parse_args()

    with open(LUA_PATH, 'r', encoding='utf-8') as f:
        text = f.read()

    features, stats = parse_function_275a(text)
    print('函数275a 解析到 %d 个界面' % len(features), file=sys.stderr)
    for name, total, fb, parsed in stats:
        if name in INTERFACE_TARGETS and (total != parsed or fb):
            print('  %s: findMultiColor 调用 %d 个（兜底 %d），采用 %d 个'
                  % (name, total, fb, parsed), file=sys.stderr)

    buf = []

    def w(s=''):
        buf.append(s)

    w('# === 函数275a 界面查找表（已转为 1280x720 横屏坐标） ===')
    for name, (prop, target, comment) in INTERFACE_TARGETS.items():
        if name not in features:
            print('!! 未找到界面: %s' % name, file=sys.stderr)
            continue
        variants = features[name]
        w('')
        w('## %s  ->  %s  (%d 个变体)' % (name, target, len(variants)))
        for i, feat in enumerate(variants):
            feat = dict(feat, name=name)
            p = prop if i == 0 else '%s%d' % (prop, i + 1)
            if i == 0:
                w(emit_kotlin(p, feat, comment))
            else:
                w(emit_kotlin(p, feat, 'Fallback variant %d (the legacy script tries '
                                       'variants in order)' % (i + 1)))

    capital = parse_capital_features(text)
    w('')
    w('# === _ENV["特征"] 都城军队/法术（已转为 1280x720 横屏坐标） ===')
    w('')
    w('## -> clancapital/CapitalTroopColors.kt')
    for key, prop in CAPITAL_TARGETS.items():
        if key not in capital:
            print('!! 未找到特征: %s' % key, file=sys.stderr)
            continue
        feat = dict(capital[key], name=key.split('_', 1)[1])
        w(emit_kotlin(prop, feat, 'Capital army feature "%s"' % feat['name']))

    text_out = '\n'.join(buf) + '\n'
    if args.out:
        with open(args.out, 'w', encoding='utf-8') as f:
            f.write(text_out)
        print('written to %s' % args.out, file=sys.stderr)
    else:
        sys.stdout.write(text_out)


if __name__ == '__main__':
    main()
