# -*- coding: utf-8 -*-
"""Generate discriminative ColorSchema features for the 24 clan-capital icons.

Key idea: a feature may only use pixels whose colour is UNIQUE to the icon among all 24
(the cell border / shared decorations are excluded because every icon has them).
"""
import io
import os
import numpy as np
from PIL import Image
from scipy import ndimage

D1 = r'I:\coc\游戏截图\补充1'
CELL_W, CELL_H = 130, 112
BAND = (400, 720, 140, 1160)

NAMES = [
    '超级野蛮人', '超级巨人', '亡灵大军', '火箭气球兵', '飞行堡垒', '雷霆皮卡',
    '超级飞龙', '隐秘弓箭手', '野蛮人攻城槌', '超级法师', '骷髅飞桶', '突袭炮车',
    '野猪突袭队', '高山戈仑', '地狱飞龙', '超级电磁炮', '疗伤法术', '雷电法术',
    '狂暴法术', '永恒急速法术', '超级矿工', '弹跳法术', '冰霜法术', '骷髅召唤法术',
]
shots = sorted(f for f in os.listdir(D1) if '配兵' in f)


def detect_cells(img):
    sat = img.max(axis=2) - img.min(axis=2)
    y1, y2, x1, x2 = BAND
    band = (sat > 55)[y1:y2, x1:x2]
    lbl, _ = ndimage.label(band)
    out = []
    for sl in ndimage.find_objects(lbl):
        ys, xs = sl
        h, w = ys.stop - ys.start, xs.stop - xs.start
        if not (95 <= w <= 165 and 85 <= h <= 155):
            continue
        gx, gy = x1 + xs.start, y1 + ys.start
        crop = img[gy:gy + CELL_H, gx:gx + CELL_W]
        if crop.shape[:2] == (CELL_H, CELL_W):
            out.append((gx, gy, crop))
    out.sort(key=lambda t: (round(t[1] / 100), t[0]))
    return out


def sig(crop):
    im = Image.fromarray(crop.astype(np.uint8)).resize((24, 24), Image.BILINEAR)
    return np.asarray(im).astype(np.int16).reshape(-1)


# ---- inventory ----------------------------------------------------------------
refs, instances = [], [[] for _ in range(24)]
shots_cells = {}
for f in shots:
    img = np.asarray(Image.open(os.path.join(D1, f)).convert('RGB')).astype(np.int16)
    cells = []
    for gx, gy, crop in detect_cells(img):
        s = sig(crop)
        best, bd = None, 1e9
        for i, r in enumerate(refs):
            d = float(np.abs(r - s).mean())
            if d < bd:
                bd, best = d, i
        if best is None or bd > 18.0:
            best = len(refs)
            refs.append(s)
            instances.append([])
        instances[best].append(crop)
        cells.append((best, gx, gy))
    shots_cells[f] = (img, cells)

N = len(refs)
print('distinct icons: %d' % N)

# mean / std per icon
means, stds = [], []
for i in range(N):
    st = np.stack([c.astype(np.float64) for c in instances[i]])
    means.append(st.mean(axis=0))
    stds.append(st.std(axis=0))

# discriminative score per pixel: min L1 distance to every other icon's colour there
disc = np.full((N, CELL_H, CELL_W), 1e9)
for i in range(N):
    d = np.full((CELL_H, CELL_W), 1e9)
    for j in range(N):
        if i == j:
            continue
        diff = np.abs(means[i] - means[j]).sum(axis=2)
        d = np.minimum(d, diff)
    d[stds[i].max(axis=2) > 10] = -1          # unstable pixels are useless
    disc[i] = d

print('\n%-3s %-12s  anchor        main     d     offs' % ('#', 'name'))
feats = {}
for i in range(N):
    order = np.dstack(np.unravel_index(np.argsort(-disc[i], axis=None), (CELL_H, CELL_W)))[0]
    chosen = None
    for (ay, ax) in order[:600]:
        if disc[i][ay, ax] < 100:
            break
        main = means[i][ay, ax]
        offs = []
        cand = [(ay + dy, ax + dx) for dy in range(-22, 23, 4) for dx in range(-22, 23, 4)]
        cand.sort(key=lambda p: -disc[i][p[0], p[1]] if 0 <= p[0] < CELL_H and 0 <= p[1] < CELL_W else 0)
        for (y, x) in cand:
            if not (0 <= y < CELL_H and 0 <= x < CELL_W):
                continue
            if x == ax and y == ay:
                continue
            if stds[i][y, x].max() > 8:
                continue
            c = means[i][y, x]
            # the offset must add real information: far from the anchor colour and
            # discriminative enough on its own
            if float(np.abs(c - main).sum()) < 70:
                continue
            if disc[i][y, x] < 70:
                continue
            offs.append((x - ax, y - ay, c))
            if len(offs) >= 10:
                break
        if len(offs) >= 6:
            chosen = (main, offs, ax, ay)
            break
    feats[i] = chosen
    main, offs, ax, ay = chosen
    print('%-3d %-12s  (%3d,%3d)  %s  %5d  %d'
          % (i, NAMES[i], ax, ay, '%02X%02X%02X' % (int(main[2]), int(main[1]), int(main[0])),
             int(disc[i][ay, ax]), len(offs)))


# ---- validation ---------------------------------------------------------------
def matches(img, main, offs, th=25, region=(131, 396, 1140, 679)):
    x1, y1, x2, y2 = region
    h, w = img.shape[:2]
    sub = img[y1:y2, x1:x2]
    mask = (np.abs(sub.astype(np.int16) - main.astype(np.int16)) <= th).all(axis=2)
    ys, xs = np.where(mask)
    hits = []
    for iy, ix in zip(ys, xs):
        gx, gy = x1 + ix, y1 + iy
        ok = True
        for dx, dy, c in offs:
            tx, ty = gx + dx, gy + dy
            if tx < 0 or tx >= w or ty < 0 or ty >= h:
                ok = False; break
            if (np.abs(img[ty, tx].astype(np.int16) - c.astype(np.int16)) > th).any():
                ok = False; break
        if ok:
            hits.append((gx, gy, dx, dy))
    return hits


print('\nvalidation:')
bad = 0
for i in range(N):
    main, offs, ax, ay = feats[i]
    foreign = 0
    ownmiss = 0
    for f in shots:
        img, cells = shots_cells[f]
        hits = matches(img, main, offs)
        own = [(gx, gy) for (ci, gx, gy) in cells if ci == i]
        got = set()
        for hx, hy, _, _ in hits:
            owner = None
            for (ci, gx, gy) in cells:
                if gx - 6 <= hx <= gx + CELL_W and gy - 6 <= hy <= gy + CELL_H:
                    owner = ci
            if owner == i:
                got.add((owner,))
            else:
                foreign += 1
        if own and not got:
            ownmiss += 1
    status = 'OK' if foreign == 0 and ownmiss == 0 else 'BAD(foreign=%d miss=%d)' % (foreign, ownmiss)
    if status != 'OK':
        bad += 1
    print('  #%-2d %-12s %s' % (i, NAMES[i], status))
print('\nRESULT:', 'ALL OK' if bad == 0 else '%d features need work' % bad)

print('\n--- generated entries (index | name | mainBGR | offsets) ---')
for i in range(N):
    main, offs, ax, ay = feats[i]
    off_str = ','.join('%d|%d|%02X%02X%02X' % (dx, dy, int(c[2]), int(c[1]), int(c[0]))
                       for dx, dy, c in offs)
    print('%d\t%s\t%02X%02X%02X\t%s' % (i, NAMES[i], int(main[2]), int(main[1]), int(main[0]), off_str))
