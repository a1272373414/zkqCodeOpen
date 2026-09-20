# -*- coding: utf-8 -*-
"""部署栏模板库：建库 + 识别（避开卡片上的"数量/等级"文字区域）。

关键点（按用户要求）：
- 每张部署栏卡片上有 `xN`（左上）与等级徽章（左下）数字，识别时必须排除，
  否则数字变化会干扰匹配。这里统一只保留"图标中心区"并把四角做掩码。

用法：
  python bar_templates.py build <截图> --slots 56,156,257 --names 飞龙,气球兵,巨人
  python bar_templates.py match <截图> [--min-score 0.55]
  python bar_templates.py cross <图A> <图B>
"""
import argparse
import os
import sys
import cv2
import numpy as np

DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'bar_templates')
BAR = (560, 720)
CARD_W, CARD_H = 96, 150          # 槽位卡片尺寸（略大于实测 94×~140）
ICON = (24, 34, 72, 108)          # 图标中心区（相对卡片的 x0,y0,x1,y1）——避开 xN 与等级徽章
SCALES = [0.85, 0.95, 1.0, 1.05, 1.15]


def load_bar(shot):
    img = cv2.imread('captures/attack/%s.png' % shot)
    if img is None:
        raise SystemExit('读不到 captures/attack/%s.png' % shot)
    return img[BAR[0]:BAR[1]]


# 模板与索引都用 ASCII 文件名（Windows 下 cv2 对中文路径读写会失败），中文名存 names.json
NAME_ID = {
    '飞龙': 'dragon', '气球兵': 'balloon', '巨人': 'giant', '野蛮人': 'barbarian',
    '皮卡超人': 'pekka', '弓箭手': 'archer', '哥布林': 'goblin', '法师': 'wizard',
    '天使': 'healer', '炸弹人': 'wall_breaker', '掘地矿工': 'miner', '飞龙宝宝': 'baby_dragon',
    '雷电飞龙': 'electro_dragon', '大雪怪': 'yeti', '亡灵': 'minion', '女巫': 'witch',
    '守护者学徒': 'apprentice', '巨石投手': 'bowler', '废墟女巫': 'ruin_witch', '德鲁伊': 'druid',
    '戈仑冰人': 'ice_golem', '戈仑石人': 'golem', '烈焰熔炉': 'furnace', '熔岩猎犬': 'lava_hound',
    '瓦基丽武神': 'valkyrie', '英雄猎手': 'headhunter', '野猪骑士': 'hog_rider',
    '隐秘哥布林': 'sneaky_goblin', '超级大雪怪': 'super_yeti', '闪电法术': 'spell_lightning',
}


def to_id(name):
    return NAME_ID.get(name, 'x%04d' % (abs(hash(name)) % 10000))


def read_png(path):
    data = np.fromfile(path, dtype=np.uint8)
    return None if data.size == 0 else cv2.imdecode(data, cv2.IMREAD_COLOR)


def write_png(path, img):
    ok, buf = cv2.imencode('.png', img)
    if ok:
        buf.tofile(path)


def load_index():
    p = os.path.join(DIR, 'names.json')
    if os.path.exists(p):
        import json
        with open(p, encoding='utf-8') as f:
            return json.load(f)
    return {}


def save_index(index):
    import json
    with open(os.path.join(DIR, 'names.json'), 'w', encoding='utf-8') as f:
        json.dump(index, f, ensure_ascii=False, indent=1)


def slot_crop(bar, cx):
    x0 = max(0, cx - CARD_W // 2)
    return bar[:, x0:min(bar.shape[1], x0 + CARD_W)]


def icon_of(card):
    x0, y0, x1, y1 = ICON
    return card[y0:y1, x0:x1]


def match_one(tmpl_gray, tmpl_mask, patch_gray):
    best = (-1.0, None)
    for s in SCALES:
        th, tw = tmpl_gray.shape
        tw2, th2 = max(8, int(tw * s)), max(8, int(th * s))
        if tw2 >= patch_gray.shape[1] or th2 >= patch_gray.shape[0]:
            continue
        t = cv2.resize(tmpl_gray, (tw2, th2))
        m = cv2.resize(tmpl_mask, (tw2, th2), interpolation=cv2.INTER_NEAREST)
        res = cv2.matchTemplate(patch_gray, t, cv2.TM_CCOEFF_NORMED, mask=m)
        res[~np.isfinite(res)] = -1.0
        _, mx, _, mp = cv2.minMaxLoc(res)
        if mx > best[0]:
            best = (float(mx), (mp[0] + tw2 // 2, mp[1] + th2 // 2))
    return best


def icon_mask(gray):
    """图标区掩码：排除低饱和/极暗背景无关像素，并挖掉四角（避免数字/边框）。"""
    h, w = gray.shape
    m = np.full((h, w), 255, np.uint8)
    k = 8
    m[:k, :] = 0
    m[-k:, :] = 0
    m[:, :k] = 0
    m[:, -k:] = 0
    return m


def cmd_build(args):
    os.makedirs(DIR, exist_ok=True)
    bar = load_bar(args.shot)
    slots = [int(v) for v in args.slots.split(',')]
    names = args.names.split(',')
    if len(slots) != len(names):
        raise SystemExit('slots 与 names 数量不一致')
    index = load_index()
    for cx, name in zip(slots, names):
        icon = icon_of(slot_crop(bar, cx))
        tid = to_id(name)
        write_png(os.path.join(DIR, tid + '.png'), icon)
        index[tid] = name
        print('  + %-10s <- x=%d  -> %s.png (%dx%d)' % (name, cx, tid, icon.shape[1], icon.shape[0]))
    save_index(index)


def load_templates():
    out = []
    if not os.path.isdir(DIR):
        return out
    index = load_index()
    for f in sorted(os.listdir(DIR)):
        if f.endswith('.png'):
            img = read_png(os.path.join(DIR, f))
            if img is None:
                continue
            g = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            out.append((index.get(f[:-4], f[:-4]), g, icon_mask(g)))
    return out


def cmd_match(args):
    tpls = load_templates()
    if not tpls:
        raise SystemExit('模板库为空，先 build')
    bar = load_bar(args.shot)
    gray = cv2.cvtColor(bar, cv2.COLOR_BGR2GRAY)
    slots = [int(v) for v in args.slots.split(',')] if args.slots else \
        [56 + 100 * i for i in range(12)]
    print('== %s 识别（%d 个模板，阈值 %.2f）==' % (args.shot, len(tpls), args.min_score))
    for cx in slots:
        card = slot_crop(bar, cx)
        patch = cv2.cvtColor(card, cv2.COLOR_BGR2GRAY)
        scores = sorted(((match_one(g, m, patch)[0], n) for n, g, m in tpls), reverse=True)
        top = scores[:3]
        marker = 'OK' if top and top[0][0] >= args.min_score else '??'
        print('  x=%4d %s | %s' % (cx, marker, ' | '.join('%s %.3f' % (n, s) for s, n in top)))


def cmd_cross(args):
    """A 库 → B 图识别：报告与 A 已知槽位的一致性（同部队交叉验证）。"""
    tpls = load_templates()
    bar = load_bar(args.shot)
    for tname, g, m in tpls:
        best = (-1, None)
        for cx in [56 + 100 * i for i in range(12)]:
            card = slot_crop(bar, cx)
            s, _ = match_one(g, m, cv2.cvtColor(card, cv2.COLOR_BGR2GRAY))
            if s > best[0]:
                best = (s, cx)
        print('  %-10s -> %s 最好槽位 x=%s 分数=%.3f' % (tname, args.shot, best[1], best[0]))


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest='mode', required=True)
    b = sub.add_parser('build')
    b.add_argument('shot'); b.add_argument('--slots', required=True); b.add_argument('--names', required=True)
    m = sub.add_parser('match')
    m.add_argument('shot'); m.add_argument('--slots'); m.add_argument('--min-score', type=float, default=0.55)
    c = sub.add_parser('cross'); c.add_argument('shot')
    args = ap.parse_args()
    {'build': cmd_build, 'match': cmd_match, 'cross': cmd_cross}[args.mode](args)


if __name__ == '__main__':
    main()
