# -*- coding: utf-8 -*-
"""
Pixel-level verification of the migrated ColorSchema entries (optimization items 1 & 2)
against the real screenshots at I:\\coc\\游戏截图.

The matching reproduces the runtime exactly (see rust_logic/src/color/mod.rs):
    is_color_match: per-channel |image - target| <= threshold
    threshold = int(255 * (1 - similarity))
    find_multi_colors: scan region x1..x2, y1..y2 (top-left to bottom-right for direction 0/2),
                       for each pixel matching mainColor, ALL offset points must match.

Usage:
    python tools/verify_migrated_colors.py
"""
import io
import os
import re
import glob
import numpy as np
from PIL import Image

SHOT_DIR = r'I:\coc\游戏截图'
KT_ROOT = r'd:\work\my\zkqfz\zkqCodeOpen\app\src\main\java\com\coc\zkqcode\jar\code\colorschema'

# The 33 entries migrated from the legacy freescript (函数275a + 特征表), grouped by file.
MIGRATED = {
    'UIColors': ['PlayerProfileButton', 'RedX', 'RedX2', 'RedX3', 'RedX4', 'RedX5',
                 'RedX6', 'RedX7', 'RedX8', 'RedX9'],
    # CommonDialog2 removed: its light-blue colors no longer exist in the current game and
    # the re-derived CommonDialog (gold edge + brown body) already matches the real popups.
    'FeatureColors': ['CommonDialog', 'BottomLeftReturnToCamp',
                      'ReturnToCampMain', 'ReturnToCampMain2', 'ReturnToCampMain3',
                      'ReturnToCampMain4', 'ReturnToCampMain5', 'ReturnToCampBuilder'],
    'MainBaseAttackColors': ['GiveUpButton', 'ClanGamesEntry', 'VictoryStar',
                             'VictoryStar2', 'VictoryStar3'],
    # HeroHallEntry removed: the hero hall building moves with the base layout and can be
    # occluded, so it is located by YOLO (HeroHallHelper) instead of a fixed-region schema.
    'MainBaseHeroHallColors': ['EquipmentButton'],
    'MainBaseClanColors': ['LeaveClanButton', 'LeaveClanButton2', 'ColorfulClanIcon',
                           'ColorfulClanIcon2', 'GrayClanIcon', 'GrayClanIcon2'],
    'MainBaseTraining': ['ArmyButton'],
    'ClanCapitalTutorialColors': ['ClanCapitalEntry'],
}

PARSE_RE = re.compile(
    r'override val (\w+) = ColorSchema\.parse\(\s*'
    r'(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*'
    r'"([^"]*)",\s*"([^"]*)",\s*(-?\d+),\s*([\d.]+),\s*"([^"]*)"\s*\)'
)


def hex_to_rgb(color_str):
    """Replicate ColorSchema.parseBgrToRgb: take part before '-', treat as 0xBBGGRR."""
    hexpart = color_str.split('-')[0]
    v = int(hexpart, 16)
    b = (v >> 16) & 0xFF
    g = (v >> 8) & 0xFF
    r = v & 0xFF
    return (r, g, b)


def parse_entries():
    out = {}
    for fname, names in MIGRATED.items():
        matches = glob.glob(os.path.join(KT_ROOT, '**', fname + '.kt'), recursive=True)
        if not matches:
            print('WARN: file not found for', fname)
            continue
        path = matches[0]
        text = io.open(path, 'r', encoding='utf-8').read()
        for m in PARSE_RE.finditer(text):
            name = m.group(1)
            if name not in names:
                continue
            x1, y1, x2, y2 = (int(m.group(i)) for i in (2, 3, 4, 5))
            main = hex_to_rgb(m.group(6))
            offs = []
            for p in m.group(7).split(','):
                parts = p.split('|')
                if len(parts) >= 3:
                    offs.append((int(parts[0]), int(parts[1]), hex_to_rgb(parts[2])))
            direction = int(m.group(8))
            sim = float(m.group(9))
            th = int(round(255 * (1 - sim)))
            out.setdefault(name, []).append(
                dict(file=fname, x1=x1, y1=y1, x2=x2, y2=y2,
                     main=main, offs=offs, direction=direction, sim=sim, th=th,
                     display=m.group(10)))
    # keep declaration order
    ordered = []
    for fname, names in MIGRATED.items():
        for n in names:
            if n in out:
                ordered.append((n, out[n][0]))
    return ordered


def match_in_image(img, e):
    """Return (x, y) of first hit in img (np.uint8 HxWx3 RGB), or None."""
    h, w = img.shape[:2]
    x1, y1, x2, y2 = e['x1'], e['y1'], e['x2'], e['y2']
    x1 = max(0, x1); y1 = max(0, y1)
    x2 = min(w - 1, x2); y2 = min(h - 1, y2)
    if x1 > x2 or y1 > y2:
        return None
    main = np.array(e['main'], dtype=np.int16)
    th = e['th']
    # boolean mask of pixels matching the main color (whole image, vectorized)
    diff = np.abs(img.astype(np.int16) - main)
    mask = (diff <= th).all(axis=2)
    sub = mask[y1:y2 + 1, x1:x2 + 1]
    ys, xs = np.where(sub)
    if ys.size == 0:
        return None
    # offset check
    offs = e['offs']
    offc = np.array([o[2] for o in offs], dtype=np.int16)
    odx = np.array([o[0] for o in offs])
    ody = np.array([o[1] for o in offs])
    for iy, ix in zip(ys, xs):
        gx, gy = x1 + ix, y1 + iy
        ok = True
        for k in range(len(offs)):
            tx, ty = gx + odx[k], gy + ody[k]
            if tx < 0 or tx >= w or ty < 0 or ty >= h:
                ok = False
                break
            pdiff = np.abs(img[ty, tx].astype(np.int16) - offc[k])
            if (pdiff > th).any():
                ok = False
                break
        if ok:
            return (gx, gy)
    return None


def main():
    entries = parse_entries()
    print('parsed %d migrated entries' % len(entries))
    shots = sorted(glob.glob(os.path.join(SHOT_DIR, '*.png')))
    print('screenshots: %d' % len(shots))

    results = {}  # name -> (found_bool, shot_file, coords)
    for name, e in entries:
        results[name] = (False, None, None)

    # For each screenshot, test all entries (load once, reuse)
    for sp in shots:
        img = np.asarray(Image.open(sp).convert('RGB'))
        base = os.path.basename(sp)
        for name, e in entries:
            if results[name][0]:
                continue
            hit = match_in_image(img, e)
            if hit is not None:
                results[name] = (True, base, hit)

    # Report
    ok = sum(1 for v in results.values() if v[0])
    print('\n=== RESULT: %d/%d migrated entries matched on at least one screenshot ===\n'
          % (ok, len(entries)))
    for fname in MIGRATED:
        for n in MIGRATED[fname]:
            found, base, coord = results.get(n, (False, None, None))
            if found:
                print('  [OK ] %-22s (%-18s) @ %s %s' % (n, fname, base, coord))
            else:
                print('  [MISS] %-22s (%-18s)' % (n, fname))

    # Interfaces clearly not captured at all (no screenshot probably contains them)
    print('\n=== entries with NO match (interface may be missing from screenshots or feature moved) ===')
    for n in [n for n, e in entries if not results[n][0]]:
        print('   -', n)

    # Second pass: for the missing entries, relax the search region to the WHOLE image
    # to tell "feature moved" (found elsewhere) from "colors stale" (not found anywhere).
    print('\n=== full-image relaxation for MISS entries (found coords => feature moved; none => colors stale) ===')
    for n, e in entries:
        if results[n][0]:
            continue
        full = dict(e)
        full['x1'], full['y1'], full['x2'], full['y2'] = 0, 0, 1279, 719
        found_any = None
        for sp in shots:
            img = np.asarray(Image.open(sp).convert('RGB'))
            hit = match_in_image(img, full)
            if hit is not None:
                found_any = (os.path.basename(sp), hit)
                break
        if found_any:
            print('   MOVED  %-22s -> %s %s' % (n, found_any[0], found_any[1]))
        else:
            print('   STALE  %-22s (no pixel match anywhere in any screenshot)' % n)


if __name__ == '__main__':
    main()
