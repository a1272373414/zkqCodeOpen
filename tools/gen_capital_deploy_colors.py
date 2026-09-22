# -*- coding: utf-8 -*-
"""Generate clancapital/CapitalDeployColors.kt from the legacy script's 特征 table.

Migrates the clan-capital BATTLE deploy features (untouched by the project so far):
  - 都城下兵_*  (12 troops/spells -> used to pick a unit in the battle deploy bar)
  - 残血        (list of low-HP enemy markers -> used as deploy points)
  - 偷袭法术下兵点 / 偷袭冰冻法术下兵点 (lists of sneak-spell splash points)

Coordinates are mapped from the legacy portrait buffer (720x1280) to 1280x720 landscape with the
same verified 90° transform used everywhere (docs/复用优化方案20260919.md §11.3).

Usage: python tools/gen_capital_deploy_colors.py
"""
import os
import re
import sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TOOLS)
from migrate_lua_colors import (LUA_PATH, portrait_to_landscape_rect,  # noqa: E402
                                convert_offsets)

ROOT = os.path.dirname(TOOLS)
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'coc', 'zkqcode', 'jar',
                   'code', 'colorschema', 'colorpackage', 'clancapital',
                   'CapitalDeployColors.kt')

DEPLOY = {
    '都城下兵_超级矿工': 'CapitalDeploySuperMiner',
    '都城下兵_野猪偷袭队': 'CapitalDeployHogRaider',
    '都城下兵_超级巨人': 'CapitalDeploySuperGiant',
    '都城下兵_超级法师': 'CapitalDeploySuperWizard',
    '都城下兵_隐秘弓箭手': 'CapitalDeploySneakyArcher',
    '都城下兵_超级野蛮人': 'CapitalDeploySuperBarbarian',
    '都城下兵_野蛮人攻城槌': 'CapitalDeployBattleRam',
    '都城下兵_骷髅法术': 'CapitalDeploySkeletonSpell',
    '都城下兵_疗伤法术': 'CapitalDeployHealingSpell',
    '都城下兵_雷电法术': 'CapitalDeployLightningSpell',
    '都城下兵_弹跳法术': 'CapitalDeployJumpSpell',
    '都城下兵_冰冻法术': 'CapitalDeployFreezeSpell',
}
LISTS = {
    '残血': 'CapitalLowHpTargets',
    '偷袭法术下兵点': 'CapitalSneakSpellPoints',
    '偷袭冰冻法术下兵点': 'CapitalSneakFreezePoints',
}


def balanced(text, start):
    """Index of the '}' matching text[start] == '{'."""
    depth, inq, i = 0, False, start
    while i < len(text):
        c = text[i]
        if inq:
            if c == '"':
                inq = False
        elif c == '"':
            inq = True
        elif c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError('unbalanced braces at %d' % start)


def split_top(s):
    """Split on top-level commas (ignore commas inside quotes/braces)."""
    parts, depth, inq, cur = [], 0, False, ''
    for c in s:
        if inq:
            cur += c
            if c == '"':
                inq = False
        elif c == '"':
            inq = True
            cur += c
        elif c == '{':
            depth += 1
            cur += c
        elif c == '}':
            depth -= 1
            cur += c
        elif c == ',' and depth == 0:
            parts.append(cur)
            cur = ''
        else:
            cur += c
    if cur.strip():
        parts.append(cur)
    return parts


def parse_feature(body):
    vals = [v.strip() for v in split_top(body)]
    if len(vals) < 8:
        return None
    try:
        return {
            'rect': tuple(int(vals[i]) for i in range(4)),
            'main': vals[4].strip('"'),
            'offsets': vals[5].strip('"'),
            'direction': int(vals[6]),
            'similarity': float(vals[7]),
        }
    except ValueError:
        return None


def get_table(text, key):
    m = None
    for mm in re.finditer(r'\["%s"\]\s*=\s*\{' % re.escape(key), text):
        m = mm
    if m is None:
        return None
    start = text.index('{', m.start())
    return text[start + 1:balanced(text, start)]


def parse_expr(feat, name, indent):
    x1, y1, x2, y2 = portrait_to_landscape_rect(*feat['rect'])
    offs = convert_offsets(feat['offsets'])
    sim = '%g' % feat['similarity']
    return ('%sColorSchema.parse(\n'
            '%s    %d, %d, %d, %d, "%s",\n'
            '%s    "%s",\n'
            '%s    %d, %s, "%s"\n'
            '%s)' % (indent, indent, x1, y1, x2, y2, feat['main'],
                     indent, offs, indent, feat['direction'], sim, name, indent))


def member(prop, feat):
    expr = parse_expr(feat, prop, '    ')
    return '    override val %s = %s' % (prop, expr[4:])


def main():
    text = open(LUA_PATH, encoding='utf-8').read()

    tuples, lists = {}, {}
    for key in DEPLOY:
        body = get_table(text, key)
        f = parse_feature(body) if body else None
        if f is None:
            print('!! missing/invalid:', key, file=sys.stderr)
            continue
        tuples[key] = f
    for key in LISTS:
        body = get_table(text, key)
        items = []
        if body:
            for g in split_top(body):
                g = g.strip()
                if g.startswith('{'):
                    f = parse_feature(g[1:-1])
                    if f:
                        items.append(f)
        lists[key] = items
        print('%s -> %d entries' % (key, len(items)), file=sys.stderr)

    iface = ['    val %s: ColorSchema' % p for p in DEPLOY.values()]
    iface += ['    val %s: List<ColorSchema>' % p for p in LISTS.values()]
    obj = [member(DEPLOY[k], tuples[k]) for k in DEPLOY if k in tuples]
    for k in LISTS:
        if k not in lists:
            continue
        items = ',\n'.join(parse_expr(f, LISTS[k], '        ') for f in lists[k])
        obj.append('    override val %s = listOf(\n%s,\n    )' % (LISTS[k], items))

    kt = '''@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Clan capital BATTLE deploy features, migrated from the legacy script
 * (`awcocx_main.lua` 特征: 都城下兵_* / 残血 / 偷袭法术下兵点 / 偷袭冰冻法术下兵点),
 * with the same 90° portrait->landscape mapping as the rest of the project.
 *
 * - `CapitalDeploy*`      : pick a unit/spell in the capital battle deploy bar.
 * - `CapitalLowHpTargets` : low-HP enemy markers; the first hit is used as the deploy point.
 * - `CapitalSneak*Points` : splash points for the sneak spell / freeze spell.
 *
 * Regenerate with tools/gen_capital_deploy_colors.py.
 */
interface ICapitalDeployColors {
%s
}

object CapitalDeployColors : ICapitalDeployColors {
%s
}
''' % ('\n'.join(iface), '\n'.join(obj))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, 'w', encoding='utf-8').write(kt)
    print('wrote', OUT, '(%d tuples, %d lists)' % (len(tuples), len(lists)))


if __name__ == '__main__':
    main()
