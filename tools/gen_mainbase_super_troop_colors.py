# -*- coding: utf-8 -*-
"""Generate discriminative ColorSchema features for the 14 main-base SUPER troops
(超级兵) from the 补充4 army-comp screenshots.

Layout (recovered from the screenshots): the training/army panel is a 9-column x 2-row
grid; the rightmost column (cx~1193) holds the two super troops, one per row. The
screenshot filename names them as "A-B.png" where A = top-row slot, B = bottom-row slot
(verified by masked-NCC against the 超级兵 icons). We crop those two slots per shot,
derive discriminative anchor+offsets among the 14 super troops, validate, and emit
MainBaseSuperTroopColors.kt.
"""
import os
import cv2
import numpy as np
from PIL import Image

SHOT_DIR = r'I:\coc\游戏截图\补充4'
OUT_KT = r'app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseSuperTroopColors.kt'

# rightmost-column super-troop slot centres (screenshot coords, 1280x720)
TOP = (1193, 492)
BOT = (1193, 627)
CW, CH = 121, 120
CELL = 104  # feature resolution (matches gen_mainbase_troop_colors.py)
# shared search region: covers the rightmost two columns of the panel
REGION = (999, 427, 1258, 692)

# display name -> ASCII identifier (matches COC naming in the codebase)
NAME_MAP = {
    '寒冰猎犬': 'SuperFrostHound',
    '超级巨石投手': 'SuperBowler',
    '超级弓箭手': 'SuperArcher',
    '超级巨人': 'SuperGiant',
    '超级法师': 'SuperWizard',
    '超级亡灵': 'SuperMinion',
    '超级炸弹人': 'SuperWallBreaker',
    '火箭气球兵': 'RocketBalloon',
    '超级瓦基丽武神': 'SuperValkyrie',
    '超级女巫': 'SuperWitch',
    '超级矿工': 'SuperMiner',
    '超级野猪骑士': 'SuperHogRider',
    '隐秘哥布林': 'SneakyGoblin',
    '超级大雪怪': 'SuperYeti',
}


def load_shot(sf):
    return np.asarray(Image.open(os.path.join(SHOT_DIR, sf)).convert('RGB'))[:, :, ::-1].copy()


def crop_slot(img, center):
    cx, cy = center
    x1 = max(0, cx - CW // 2); y1 = max(0, cy - CH // 2)
    x2 = min(img.shape[1], cx + CW // 2); y2 = min(img.shape[0], cy + CH // 2)
    crop = img[y1:y2, x1:x2].copy()
    if crop.shape[0] != CH or crop.shape[1] != CW:
        pad = np.zeros((CH, CW, 3), np.uint8)
        pad[:crop.shape[0], :crop.shape[1]] = crop
        crop = pad
    return crop.astype(np.float64)


shots = sorted(f for f in os.listdir(SHOT_DIR) if f.endswith('.png') and '配兵' in f)
crops_by_label = {}
labels_order = []
for sf in shots:
    base = sf[:-4]  # strip .png
    parts = base.split('-')
    # parts like ['主世界','配兵','超级兵','超级法师','超级亡灵'] -> last two are the troop names
    names = [p for p in parts if p not in ('主世界', '配兵', '超级兵')]
    assert len(names) == 2, 'unexpected filename %s -> %r' % (sf, names)
    img = load_shot(sf)
    top_crop = crop_slot(img, TOP)
    bot_crop = crop_slot(img, BOT)
    crops_by_label[names[0]] = top_crop
    crops_by_label[names[1]] = bot_crop
    labels_order.append(names[0]); labels_order.append(names[1])

labels = labels_order
N = len(labels)
print('super troops (%d):' % N)
for l in labels:
    print('  %s' % l)

means = np.stack([crops_by_label[l] for l in labels])
H = W = CELL
# resize each crop to CELL for feature derivation
means_resized = np.stack([cv2.resize(means[i], (CELL, CELL), cv2.INTER_AREA) for i in range(N)])

# discriminative score per pixel: min L1 dist to every other super troop's colour
disc = np.full((N, H, W), 1e9)
for i in range(N):
    d = np.full((H, W), 1e9)
    for j in range(N):
        if i == j:
            continue
        d = np.minimum(d, np.abs(means_resized[i] - means_resized[j]).sum(axis=2))
    disc[i] = d

print('\n%-12s  anchor     main     d   noff' % 'name')
feats = {}
for i, l in enumerate(labels):
    order = np.dstack(np.unravel_index(np.argsort(-disc[i], axis=None), (H, W)))[0]
    chosen = None
    for (ay, ax) in order[:600]:
        if disc[i][ay, ax] < 80:
            break
        main = means_resized[i][ay, ax]
        offs = []
        cand = [(ay + dy, ax + dx) for dy in range(-22, 23, 3) for dx in range(-22, 23, 3)]
        cand.sort(key=lambda p: -disc[i][p[0], p[1]] if 0 <= p[0] < H and 0 <= p[1] < W else 0)
        for (y, x) in cand:
            if not (0 <= y < H and 0 <= x < W) or (x == ax and y == ay):
                continue
            c = means_resized[i][y, x]
            if float(np.abs(c - main).sum()) < 70 or disc[i][y, x] < 70:
                continue
            offs.append((x - ax, y - ay, c))
            if len(offs) >= 10:
                break
        if len(offs) >= 6:
            chosen = (main, offs, ax, ay)
            break
    if chosen is None:
        print('  %-12s FAILED to find anchor' % l)
        continue
    feats[l] = chosen
    main, offs, ax, ay = chosen
    print('  %-12s  (%3d,%3d)  %s  %5d  %d' %
          (l, ax, ay, '%02X%02X%02X' % (int(main[2]), int(main[1]), int(main[0])),
           int(disc[i][ay, ax]), len(offs)))


def matches(cell, main, offs, th=25):
    mask = (np.abs(cell.astype(np.int16) - main.astype(np.int16)) <= th).all(axis=2)
    ys, xs = np.where(mask)
    for iy, ix in zip(ys, xs):
        ok = True
        for dx, dy, c in offs:
            tx, ty = ix + dx, iy + dy
            if tx < 0 or tx >= W or ty < 0 or ty >= H:
                ok = False; break
            if (np.abs(cell[ty, tx].astype(np.int16) - c.astype(np.int16)) > th).any():
                ok = False; break
        if ok:
            return True
    return False


# validation: each feature must match only its own slot crop (others are distinct troops)
print('\nvalidation:')
bad = 0
for i, l in enumerate(labels):
    if l not in feats:
        continue
    main, offs, ax, ay = feats[l]
    foreign = 0
    ownmiss = 0
    for j, lj in enumerate(labels):
        cell = means_resized[j]
        hit = matches(cell, main, offs)
        if j == i:
            if not hit:
                ownmiss += 1
        elif hit:
            foreign += 1
    status = 'OK' if foreign == 0 and ownmiss == 0 else 'BAD(f=%d m=%d)' % (foreign, ownmiss)
    if status != 'OK':
        bad += 1
    print('  %-12s %s' % (l, status))
print('\nRESULT:', 'ALL OK' if bad == 0 else '%d need work' % bad)

if feats and bad == 0 and all(l in feats for l in labels):
    lines = ['@file:Suppress("PropertyName")', '',
             'package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase', '',
             'import com.coc.zkqcode.jar.code.colorschema.ColorSchema', '',
             '/**', ' * Main-base SUPER troop (超级兵) recognition features.',
             ' * Derived from the 1280x720 screenshots in "补充4" (配兵 screen): the two',
             ' * rightmost-column slots hold the super troops. Filename "A-B" = top,bottom.',
             ' * Shared search region: ' + str(REGION) + '.',
             ' * Regenerate with tools/gen_mainbase_super_troop_colors.py.', ' */',
             'interface IMainBaseSuperTroopColors {']
    for l in labels:
        if l in feats:
            lines.append('    val MainBase%s: ColorSchema' % NAME_MAP.get(l, l))
    lines.append('}')
    lines.append('')
    lines.append('object MainBaseSuperTroopColors : IMainBaseSuperTroopColors {')
    for l in labels:
        if l not in feats:
            continue
        main, offs, ax, ay = feats[l]
        main_hex = '%02X%02X%02X' % (int(main[2]), int(main[1]), int(main[0]))
        off_str = ','.join('%d|%d|%02X%02X%02X' % (dx, dy, int(c[2]), int(c[1]), int(c[0])) for dx, dy, c in offs)
        lines.append('    override val MainBase%s = ColorSchema.parse(' % NAME_MAP.get(l, l))
        lines.append('        %d, %d, %d, %d, "%s",' % (REGION[0], REGION[1], REGION[2], REGION[3], main_hex))
        lines.append('        "%s",' % off_str)
        lines.append('        0, 0.9, "%s"' % l)
        lines.append('    )')
    lines.append('}')
    with open(OUT_KT, 'w', encoding='utf-8') as fp:
        fp.write('\n'.join(lines) + '\n')
    print('\nwrote', OUT_KT)
