# -*- coding: utf-8 -*-
"""Generate discriminative ColorSchema features for the main-base army screen troop slots,
auto-naming each slot with the trophy icons (masked NCC) so no manual labelling is needed.

Adapted from gen_capital_troop_colors.py: fixed 7x2 grid instead of auto-detected cells.
Writes app/.../colorpackage/mainbase/MainBaseTroopColors.kt and validates.
"""
import os
import cv2
import numpy as np
from PIL import Image

ICON_ROOT = r'D:\work\my\codebuddy_test\coc_manager_test01\src\main\resources\static\icon'
SHOT_DIR = r'I:\coc\游戏截图\补充1'
OUT_KT = r'app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseTroopColors.kt'

COLS = [295, 425, 556, 686, 817, 947, 1078]
ROWS = [433, 568]
CW, CH = 121, 120
GRID_REGION = (COLS[0] - 5, ROWS[0] - 5, COLS[-1] + CW + 5, ROWS[-1] + CH + 5)
T, CELL = 88, 104
SCORE_TH = 0.42


def load_icon(name, color):
    im = Image.open(os.path.join(ICON_ROOT, color, name + '.png')).convert('RGBA')
    a = np.asarray(im)[:, :, 3]; rgb = np.asarray(im)[:, :, :3]
    ys, xs = np.where(a > 128)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    art = rgb[y0:y1 + 1, x0:x1 + 1]; am = a[y0:y1 + 1, x0:x1 + 1]
    ah, aw = art.shape[:2]; side = max(ah, aw)
    cv = np.zeros((side, side, 3), np.uint8); cm = np.zeros((side, side), np.uint8)
    cv[(side - ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = art
    cm[(side - ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = am
    g = cv2.cvtColor(cv2.resize(cv, (T, T), cv2.INTER_AREA), cv2.COLOR_RGB2GRAY)
    m = cv2.resize(cm, (T, T), cv2.INTER_NEAREST); _, m = cv2.threshold(m, 128, 255, cv2.THRESH_BINARY)
    return g, m


def label_cell(cell_gray):
    best, bs = '空', -1.0
    for name, tg, tm in ICONS:
        res = cv2.matchTemplate(cell_gray, tg, cv2.TM_CCOEFF_NORMED, mask=tm)
        _, mx, _, _ = cv2.minMaxLoc(res)
        if mx > bs:
            bs, best = mx, name
    return best if bs >= SCORE_TH else '空', bs


ICONS = []
for color in sorted(os.listdir(ICON_ROOT)):
    d = os.path.join(ICON_ROOT, color)
    if not os.path.isdir(d):
        continue
    for f in sorted(os.listdir(d)):
        if f.endswith('.png'):
            g, m = load_icon(f[:-4], color)
            ICONS.append((f[:-4], g, m))

shots = sorted(f for f in os.listdir(SHOT_DIR) if '兵种配置' in f)
# build cells: list of (label, crop_rgb) per shot
all_cells = []  # (shot, slot_idx, col, row, label, crop)
crops_by_label = {}
for sf in shots:
    img = np.asarray(Image.open(os.path.join(SHOT_DIR, sf)).convert('RGB'))[:, :, ::-1].copy()
    gimg = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gi = 0
    for ri, ry in enumerate(ROWS):
        for ci, cx in enumerate(COLS):
            cell_g = gimg[ry:ry + CH, cx:cx + CW]
            cell_g = cv2.resize(cell_g, (CELL, CELL), cv2.INTER_AREA)
            label, sc = label_cell(cell_g)
            cell_rgb = img[ry:ry + CH, cx:cx + CW]
            cell_rgb = cv2.resize(cell_rgb, (CELL, CELL), cv2.INTER_AREA)
            all_cells.append((sf, gi, ci, ri, label, cell_rgb, sc))
            if label != '空':
                crops_by_label.setdefault(label, []).append(cell_rgb)
            gi += 1

labels = sorted(crops_by_label.keys())
N = len(labels)
print('distinct troops (auto-labeled by icons): %d' % N)
for l in labels:
    print('  %s (%d samples)' % (l, len(crops_by_label[l])))

# mean crop per troop (samples identical across shots, but average anyway)
means = []
for l in labels:
    st = np.stack([c.astype(np.float64) for c in crops_by_label[l]])
    means.append(st.mean(axis=0))
means = np.stack(means)

# discriminative score per pixel: min L1 dist to every other troop's colour there
H, W = CELL, CELL
disc = np.full((N, H, W), 1e9)
for i in range(N):
    d = np.full((H, W), 1e9)
    for j in range(N):
        if i == j:
            continue
        diff = np.abs(means[i] - means[j]).sum(axis=2)
        d = np.minimum(d, diff)
    disc[i] = d

print('\n%-12s  anchor     main     d   noff' % 'name')
feats = {}
for i, l in enumerate(labels):
    order = np.dstack(np.unravel_index(np.argsort(-disc[i], axis=None), (H, W)))[0]
    chosen = None
    for (ay, ax) in order[:600]:
        if disc[i][ay, ax] < 80:
            break
        main = means[i][ay, ax]
        offs = []
        cand = [(ay + dy, ax + dx) for dy in range(-22, 23, 3) for dx in range(-22, 23, 3)]
        cand.sort(key=lambda p: -disc[i][p[0], p[1]] if 0 <= p[0] < H and 0 <= p[1] < W else 0)
        for (y, x) in cand:
            if not (0 <= y < H and 0 <= x < W):
                continue
            if x == ax and y == ay:
                continue
            c = means[i][y, x]
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
    if chosen is None:
        print('  %s  FAILED to find anchor' % l)
        continue
    feats[l] = chosen
    main, offs, ax, ay = chosen
    print('%-12s  (%3d,%3d)  %s  %5d  %d' %
          (l, ax, ay, '%02X%02X%02X' % (int(main[2]), int(main[1]), int(main[0])),
           int(disc[i][ay, ax]), len(offs)))


# validation: each feature must match only its own slot across all shots
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


print('\nvalidation:')
bad = 0
for l in labels:
    if l not in feats:
        continue
    main, offs, ax, ay = feats[l]
    foreign = 0
    ownmiss = 0
    for (sf, gi, ci, ri, label, cell, sc) in all_cells:
        if label == l:
            if not matches(cell, main, offs):
                ownmiss += 1
        elif label != '空':
            if matches(cell, main, offs):
                foreign += 1
    status = 'OK' if foreign == 0 and ownmiss == 0 else 'BAD(f=%d m=%d)' % (foreign, ownmiss)
    if status != 'OK':
        bad += 1
    print('  %-12s %s' % (l, status))
print('\nRESULT:', 'ALL OK' if bad == 0 else '%d need work' % bad)


# Chinese troop name -> ASCII identifier (matches COC naming in the codebase)
NAME_MAP = {
    '哥布林': 'Goblin', '大雪怪': 'Yeti', '天使': 'Healer', '巨人': 'Giant',
    '弓箭手': 'Archer', '掘地矿工': 'Miner', '法师': 'Wizard', '炸弹人': 'WallBreaker',
    '皮卡超人': 'Pekka', '野蛮人': 'Barbarian', '雷电飞龙': 'ElectroDragon',
    '飞龙': 'Dragon', '飞龙宝宝': 'BabyDragon',
}


def to_name(l):
    if l in NAME_MAP:
        return NAME_MAP[l]
    return ''.join(ch if (ch.isalnum() or ch == '_') else '_' for ch in l)

if feats:
    lines = []
    lines.append('@file:Suppress("PropertyName")')
    lines.append('')
    lines.append('package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase')
    lines.append('')
    lines.append('import com.coc.zkqcode.jar.code.colorschema.ColorSchema')
    lines.append('')
    lines.append('/**')
    lines.append(' * Main-base army-screen troop recognition features.')
    lines.append(' * Auto-named from the trophy icons (masked NCC) and derived from real')
    lines.append(' * 1280x720 screenshots of the "部队配置" screen (7x2 slot grid).')
    lines.append(' * Region shared by all features: ' + str(GRID_REGION) + '.')
    lines.append(' * Regenerate with tools/gen_mainbase_troop_colors.py.')
    lines.append(' */')
    lines.append('interface IMainBaseTroopColors {')
    for l in labels:
        if l in feats:
            lines.append('    val MainBase%s: ColorSchema' % to_name(l))
    lines.append('}')
    lines.append('')
    lines.append('object MainBaseTroopColors : IMainBaseTroopColors {')
    for l in labels:
        if l not in feats:
            continue
        main, offs, ax, ay = feats[l]
        off_str = ','.join('%d|%d|%02X%02X%02X' % (dx, dy, int(c[2]), int(c[1]), int(c[0])) for dx, dy, c in offs)
        main_hex = '%02X%02X%02X' % (int(main[2]), int(main[1]), int(main[0]))
        lines.append('    override val MainBase%s = ColorSchema.parse(' % to_name(l))
        lines.append('        %d, %d, %d, %d, "%s",' % (GRID_REGION[0], GRID_REGION[1], GRID_REGION[2], GRID_REGION[3], main_hex))
        lines.append('        "%s",' % off_str)
        lines.append('        0, 0.9, "%s"' % l)
        lines.append('    )')
    lines.append('}')
    with open(OUT_KT, 'w', encoding='utf-8') as fp:
        fp.write('\n'.join(lines) + '\n')
    print('\nwrote', OUT_KT)
