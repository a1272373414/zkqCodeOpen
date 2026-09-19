# -*- coding: utf-8 -*-
"""Generate discriminative ColorSchema features for the main-base BLACK-ELIXIR troop panel
from a single screenshot (I:\\coc\\游戏截图\\主世界-配兵-兵种2.png).

The panel is NOT the 7x2 army grid; troops are spread over several rows. We:
  1. scan all 61 icons over the whole screenshot (masked NCC) to locate each troop,
  2. keep 黑油兵 hits above threshold and cluster by location into distinct slots,
  3. crop a 120x120 box around each slot, derive discriminative anchor+offsets (each
     troop is one instance; disc = min colour distance to every other troop),
  4. validate each feature matches only its own slot over the shared panel region,
  5. emit MainBaseBlackElixirTroopColors.kt.
"""
import os
import cv2
import numpy as np
from PIL import Image

ICON_ROOT = r'D:\work\my\codebuddy_test\coc_manager_test01\src\main\resources\static\icon'
SF = r'I:\coc\游戏截图\补充3\主世界-配兵-黑油兵2.png'
OUT_KT = r'app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase\MainBaseBlackElixirTroopColors.kt'

T = 100          # template size for full-image scan
CROP = 96        # per-slot crop size (~100px icons with a little margin)
SCORE_TH = 0.20  # floor to drop pure-garbage hits; 烈焰熔炉 best ~0.30 still kept
CLUSTER = 15     # px: only true overlapping hits (<this) are de-duplicated

NAME_MAP = {
    '亡灵': 'Minion', '女巫': 'Witch', '守护者学徒': 'ApprenticeWarden',
    '巨石投手': 'Bowler', '废墟女巫': 'RuinWitch', '德鲁伊': 'Druid',
    '戈仑冰人': 'IceGolem', '戈仑石人': 'Golem', '熔岩猎犬': 'LavaHound',
    '瓦基丽武神': 'Valkyrie', '英雄猎手': 'Hunter', '野猪骑士': 'HogRider',
    '烈焰熔炉': 'InfernoDragon',
}


def load_icon(name, color):
    im = Image.open(os.path.join(ICON_ROOT, color, name + '.png')).convert('RGBA')
    a = np.asarray(im)[:, :, 3]; rgb = np.asarray(im)[:, :, :3]
    ys, xs = np.where(a > 128)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    art = rgb[y0:y1 + 1, x0:x1 + 1]; am = a[y0:y1 + 1, x0:x1 + 1]
    ah, aw = art.shape[:2]; side = max(ah, aw)
    cv = np.zeros((side, side, 3), np.uint8); cm = np.zeros((side, side), np.uint8)
    cv[(side - ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = art
    cm[(side-ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = am
    g = cv2.cvtColor(cv2.resize(cv, (T, T), cv2.INTER_AREA), cv2.COLOR_RGB2GRAY)
    m = cv2.resize(cm, (T, T), cv2.INTER_NEAREST); _, m = cv2.threshold(m, 128, 255, cv2.THRESH_BINARY)
    return g, m


# 1) scan
icons = []
for color in sorted(os.listdir(ICON_ROOT)):
    d = os.path.join(ICON_ROOT, color)
    if not os.path.isdir(d):
        continue
    for f in sorted(os.listdir(d)):
        if f.endswith('.png'):
            g, m = load_icon(f[:-4], color)
            icons.append((f[:-4], color, g, m))

img = np.asarray(Image.open(SF).convert('RGB'))[:, :, ::-1].copy()
gimg = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
print('scanning %dx%d' % (gimg.shape[1], gimg.shape[0]))

hits = []
for name, color, tg, tm in icons:
    res = cv2.matchTemplate(gimg, tg, cv2.TM_CCOEFF_NORMED, mask=tm)
    _, mx, _, loc = cv2.minMaxLoc(res)
    hits.append((name, color, mx, loc[0] + T // 2, loc[1] + T // 2))

# 2) one best hit per 黑油兵 name (don't merge close-but-distinct troops like
#    戈仑冰人/戈仑石人 which sit ~35px apart). Only de-duplicate true overlaps (<CLUSTER).
best = {}
for name, color, mx, lx, ly in hits:
    if color != '黑油兵':
        continue
    if name not in best or mx > best[name][0]:
        best[name] = (mx, lx, ly)
cand = [(name, '黑油兵', mx, lx, ly) for name, (mx, lx, ly) in best.items() if mx >= SCORE_TH]
slots = []
for h in sorted(cand, key=lambda x: -x[2]):
    if all(abs(h[3] - s[3]) > CLUSTER or abs(h[4] - s[4]) > CLUSTER for s in slots):
        slots.append(h)

# ground-truth positions from the user's blue-box annotation
# (主世界-配兵-黑油兵2-标注框选2.png). These override NCC for the troops that
# NCC mis-locates / drops. left->right boxes = 戈仑石人 / 戈仑冰人 / 烈焰熔炉.
OVERRIDE = {
    '戈仑石人': (548, 496),
    '戈仑冰人': (803, 491),
    '烈焰熔炉': (1063, 490),
}
slots = [s for s in slots if s[0] not in OVERRIDE]
for name, (ox, oy) in OVERRIDE.items():
    slots.append((name, '黑油兵', 1.0, ox, oy))

print('\n黑油兵 slots (%d):' % len(slots))
for h in slots:
    print('  %-8s score=%.3f at (%d,%d)' % (h[0], h[2], h[3], h[4]))

# 3) crop 120x120 RGB around each slot
labels = []; crops = []
for h in slots:
    cx, cy = h[3], h[4]
    x1 = max(0, cx - CROP // 2); y1 = max(0, cy - CROP // 2)
    x2 = min(gimg.shape[1], cx + CROP // 2); y2 = min(gimg.shape[0], cy + CROP // 2)
    crop = img[y1:y2, x1:x2].copy()
    if crop.shape[0] != CROP or crop.shape[1] != CROP:
        # pad if at edge
        pad = np.zeros((CROP, CROP, 3), np.uint8)
        pad[:crop.shape[0], :crop.shape[1]] = crop
        crop = pad
    labels.append(h[0]); crops.append(crop.astype(np.float64))

N = len(labels)
means = np.stack(crops)
H = W = CROP

# 4) discriminative anchor + offsets (each troop = one instance)
disc = np.full((N, H, W), 1e9)
for i in range(N):
    d = np.full((H, W), 1e9)
    for j in range(N):
        if i == j:
            continue
        d = np.minimum(d, np.abs(means[i] - means[j]).sum(axis=2))
    disc[i] = d

feats = {}
print('\n%-10s anchor     main     d   noff' % 'name')
# radius restriction: an anchor/offset pixel must lie within R_i of the slot centre,
# where R_i = min(half the distance to the nearest OTHER slot, crop half). This keeps
# adjacent similar troops (e.g. 戈仑冰人/戈仑石人, ~35px apart) from sharing pixels.
CEN = CROP // 2
centres = [(h[3], h[4]) for h in slots]
RAD = []
for (cx, cy) in centres:
    dmin = min(((cx - ox) ** 2 + (cy - oy) ** 2) ** 0.5 for (ox, oy) in centres if (ox, oy) != (cx, cy))
    RAD.append(min(dmin / 2.0, CEN))
# three passes: strict, then relaxed for dark/low-contrast troops
PARAMS = [(80, 70, 6, 10), (45, 40, 4, 8), (25, 25, 3, 5)]
for i, l in enumerate(labels):
    order = np.dstack(np.unravel_index(np.argsort(-disc[i], axis=None), (H, W)))[0]
    Ri = RAD[i]
    chosen = None
    for (D_ANCHOR, C_OFF, N_MIN, N_MAX) in PARAMS:
        for (ay, ax) in order[:800]:
            if ((ax - CEN) ** 2 + (ay - CEN) ** 2) ** 0.5 > Ri:
                continue  # outside the slot's own core radius
            if disc[i][ay, ax] < D_ANCHOR:
                break
            main = means[i][ay, ax]
            offs = []
            cand2 = [(ay + dy, ax + dx) for dy in range(-15, 16, 3) for dx in range(-15, 16, 3)]
            cand2.sort(key=lambda p: -disc[i][p[0], p[1]] if 0 <= p[0] < H and 0 <= p[1] < W else 0)
            for (y, x) in cand2:
                if not (0 <= y < H and 0 <= x < W) or (x == ax and y == ay):
                    continue
                if ((x - CEN) ** 2 + (y - CEN) ** 2) ** 0.5 > Ri:
                    continue  # offset must also stay inside the core radius
                c = means[i][y, x]
                if float(np.abs(c - main).sum()) < C_OFF or disc[i][y, x] < C_OFF:
                    continue
                offs.append((x - ax, y - ay, c))
                if len(offs) >= N_MAX:
                    break
            if len(offs) >= N_MIN:
                chosen = (main, offs, ax, ay); break
        if chosen is not None:
            break
    if chosen is None:
        print('  %-8s FAILED' % l); continue
    feats[l] = chosen
    main, offs, ax, ay = chosen
    print('  %-8s (%3d,%3d) %s %5d %d' % (l, ax, ay, '%02X%02X%02X' % (int(main[2]), int(main[1]), int(main[0])), int(disc[i][ay, ax]), len(offs)))

# shared panel region (in screenshot coords) for the emitted features
rx1 = max(0, min(h[3] for h in slots) - CROP // 2)
ry1 = max(0, min(h[4] for h in slots) - CROP // 2)
rx2 = min(gimg.shape[1], max(h[3] for h in slots) + CROP // 2)
ry2 = min(gimg.shape[0], max(h[4] for h in slots) + CROP // 2)
print('\npanel region: (%d,%d,%d,%d)' % (rx1, ry1, rx2, ry2))


# 5) validation: each feature (searched over shared region) must match only its own slot
def matches(cell, main, offs, th=25):
    mask = (np.abs(cell.astype(np.int16) - main.astype(np.int16)) <= th).all(axis=2)
    ys, xs = np.where(mask)
    for iy, ix in zip(ys, xs):
        ok = True
        for dx, dy, c in offs:
            tx, ty = ix + dx, iy + dy
            if tx < 0 or tx >= W or ty < 0 or ty >= H or (np.abs(cell[ty, tx].astype(np.int16) - c.astype(np.int16)) > th).any():
                ok = False; break
        if ok:
            return True
    return False


# build region-relative crops for validation (search within shared region)
region_crop = img[ry1:ry2, rx1:rx2].astype(np.float64)
# each troop's slot center in region-relative coords:
rel_centers = [(h[3] - rx1, h[4] - ry1) for h in slots]

print('\nvalidation (search over shared panel region):')
bad = 0
for i, l in enumerate(labels):
    if l not in feats:
        continue
    main, offs, ax, ay = feats[l]
    # self-test directly on the source crop (rules out coordinate mapping)
    self_hit = matches(crops[i], main, offs)
    # anchor in region-relative coords:
    rax = rel_centers[i][0] + (ax - CROP // 2)
    ray = rel_centers[i][1] + (ay - CROP // 2)
    main_c = region_crop[ray, rax]
    offs_r = [(dx, dy, c) for dx, dy, c in offs]
    # search this feature everywhere in region_crop
    foreign = 0
    own = False
    RHr, RWr = region_crop.shape[0], region_crop.shape[1]
    for j, _ in enumerate(labels):
        rcx, rcy = rel_centers[j]
        # crop around slot j in region-relative space
        jx1 = rcx - CROP // 2; jy1 = rcy - CROP // 2
        if jx1 < 0 or jy1 < 0 or jx1 + CROP > RWr or jy1 + CROP > RHr:
            continue
        cell = region_crop[jy1:jy1 + CROP, jx1:jx1 + CROP]
        hit = matches(cell, main_c, offs_r)
        if j == i:
            own = hit
        elif hit:
            foreign += 1
    status = 'OK' if (foreign == 0 and own) else 'BAD(f=%d o=%s self=%s)' % (foreign, own, self_hit)
    if status != 'OK':
        bad += 1
    print('  %-8s %s' % (l, status))
print('\nRESULT:', 'ALL OK' if bad == 0 else '%d need work' % bad)

# emit Kotlin (only when every troop has a validated feature)
if feats and bad == 0 and all(l in feats for l in labels):
    lines = ['@file:Suppress("PropertyName")', '',
             'package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase', '',
             'import com.coc.zkqcode.jar.code.colorschema.ColorSchema', '',
             '/**', ' * Black-elixir troop recognition features for the main-base training panel.',
             ' * Auto-named from the trophy icons (masked NCC) and derived from the real',
             ' * 1280x720 screenshot "主世界-配兵-兵种2.png" (黑油兵 selection/training panel).',
             ' * Shared search region: ' + str((rx1, ry1, rx2, ry2)) + '.',
             ' * Regenerate with tools/gen_mainbase_black_elixir.py.', ' */',
             'interface IMainBaseBlackElixirTroopColors {']
    for l in labels:
        if l in feats:
            lines.append('    val MainBase%s: ColorSchema' % NAME_MAP.get(l, l))
    lines.append('}')
    lines.append('')
    lines.append('object MainBaseBlackElixirTroopColors : IMainBaseBlackElixirTroopColors {')
    for i, l in enumerate(labels):
        if l not in feats:
            continue
        main, offs, ax, ay = feats[l]
        rax = rel_centers[i][0] + (ax - CROP // 2)
        ray = rel_centers[i][1] + (ay - CROP // 2)
        main_c = region_crop[ray, rax]
        main_hex = '%02X%02X%02X' % (int(main_c[2]), int(main_c[1]), int(main_c[0]))
        off_str = ','.join('%d|%d|%02X%02X%02X' % (dx, dy, int(c[2]), int(c[1]), int(c[0])) for dx, dy, c in offs)
        lines.append('    override val MainBase%s = ColorSchema.parse(' % NAME_MAP.get(l, l))
        lines.append('        %d, %d, %d, %d, "%s",' % (rx1, ry1, rx2, ry2, main_hex))
        lines.append('        "%s",' % off_str)
        lines.append('        0, 0.9, "%s"' % l)
        lines.append('    )')
    lines.append('}')
    with open(OUT_KT, 'w', encoding='utf-8') as fp:
        fp.write('\n'.join(lines) + '\n')
    print('\nwrote', OUT_KT)
