# -*- coding: utf-8 -*-
"""都城（Clan Capital）非战斗功能的真机校验脚本（emulator-5556）。

校验对象 = M4-③ 新增的 `CapitalRaidColors.kt` 特征在真实画面上的命中率，以及
B2/B3 两个非战斗块（打开都城地图 / 九宫格选目标）的落点是否可用。

与 app 内 Kotlin 的关系：本脚本**复刻** Kotlin 的匹配逻辑（同 game_state.collect：主色 + 偏移点，
阈值 25），所以它验证的是"特征/坐标本身对不对"，不需要把 jar 推上去。

两种用法：
    python tools/live_capital_verify.py            # 只扫描当前画面，打印每个特征的命中情况
    python tools/live_capital_verify.py --map      # 额外执行 B2（点 (1165,595) 开都城地图）并复刻 B3 九宫格扫描

注意：
1. `game_state.parse_file` 的主色正则不接受 `-101010` 后缀，而本模块大量特征带该后缀，
   所以这里自带一个兼容的解析器（不改动共享模块，避免影响其它工具）。
2. 九宫格扫描的"数星星"在本项目里是近似实现（把子城矩形沿 y 均分 3 段各查一次），
   脚本用同样的口径，保证与 Kotlin 结果一致。
"""
import argparse
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cv2                                                      # noqa: E402
import game_state as gs                                         # noqa: E402
from game_state import collect                                  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CAP_DIR = os.path.join(ROOT, 'tools', 'captures', 'capital')
os.makedirs(CAP_DIR, exist_ok=True)

CAPITAL_KT = os.path.join(gs.PKG, r'clancapital\CapitalRaidColors.kt')

# 主色允许带 `-101010` 这类后缀（ColorSchema.parseBgrToRgb 会忽略它）
FEAT_RE = re.compile(
    r'(\w+)\s*=\s*ColorSchema\.parse\(\s*'
    r'(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'
    r'"([0-9A-Fa-f]{6})(?:-[0-9A-Fa-f]{6})?",\s*"([^"]*)"', re.DOTALL)
# 动态区域工厂：override fun raidStar(x1: Int, ...) : ColorSchema = ColorSchema.parse(x1, y1, x2, y2, "...", "..."
DYN_RE = re.compile(
    r'override fun (\w+)\(x1: Int, y1: Int, x2: Int, y2: Int\): ColorSchema\s*=\s*'
    r'ColorSchema\.parse\(\s*x1,\s*y1,\s*x2,\s*y2,\s*'
    r'"([0-9A-Fa-f]{6})(?:-[0-9A-Fa-f]{6})?",\s*"([^"]*)"', re.DOTALL)


def cap(name):
    p = os.path.join(CAP_DIR, name)
    with open(p, 'wb') as f:
        subprocess.run([gs.ADB, '-s', gs.SERIAL, 'exec-out', 'screencap', '-p'],
                       stdout=f, check=True)
    time.sleep(0.4)
    return p


def parse_capital_file():
    """-> ({name: feat}, {name: (anchor, offs)})，feat 与 game_state 的 4 元组一致。"""
    txt = open(CAPITAL_KT, encoding='utf-8').read()
    feats, dyns = {}, {}
    for m in FEAT_RE.finditer(txt):
        name = m.group(1)
        rect = tuple(int(m.group(i)) for i in (2, 3, 4, 5))
        anchor = gs.hex_arr(m.group(6))
        offs = []
        for tok in m.group(7).split(','):
            if not tok:
                continue
            dx, dy, h = tok.split('|')
            offs.append((int(dx), int(dy), gs.hex_arr(h)))
        feats[name] = (name, rect, anchor, offs)
    for m in DYN_RE.finditer(txt):
        anchor = gs.hex_arr(m.group(2))
        offs = []
        for tok in m.group(3).split(','):
            if not tok:
                continue
            dx, dy, h = tok.split('|')
            offs.append((int(dx), int(dy), gs.hex_arr(h)))
        dyns[m.group(1)] = (anchor, offs)
    return feats, dyns


FEATS, DYNS = parse_capital_file()


def hit(img, name, rect=None, anchor=None, offs=None):
    if name in FEATS and rect is None:
        _, r, a, o = FEATS[name]
    else:
        r, a, o = rect, anchor, offs
    x1, y1, x2, y2 = r
    got = collect(img[y1:y2, x1:x2], a, o)
    return None if not got else (got[0] + x1, got[1] + y1)


def page_of_capital(img):
    """在 game_state.page_of 之上补一条"是否在都城界面"。"""
    if hit(img, 'CapitalRaidMapBoat'):
        return 'capital_map'
    # 左下角回营 / 都城界面（FeatureColors 里已存在的同一特征）
    camp = gs.parse_file(gs.PKG + r'\FeatureColors.kt', {'BottomLeftReturnToCamp'})
    if gs.find_first(img, camp):
        return 'capital'
    return gs.page_of(img)


def scan(img, title):
    print('--- %s ---' % title)
    print('  page = %s（村庄 = %s）' % (page_of_capital(img), gs.village_of(img)))
    missed = []
    for name in sorted(FEATS):
        got = hit(img, name)
        if got:
            print('  命中 %-28s @(%d,%d)  区域=%s' % (name, got[0], got[1], FEATS[name][1]))
        else:
            missed.append(name)
    print('  未命中 %d/%d：%s' % (len(missed), len(FEATS), ', '.join(missed)))
    return missed


# --- B3：九宫格选目标（与 CapitalRaid.kt pickCapitalTarget 同口径） ---------------------------
DX = [24, -117, -190, -322, -355, -286, -439, -480, -477]
TAP_X = [607, 781, 613, 470, 702, 909, 311, 564, 847]
STAR_X1, STAR_X2, SPAN = 520, 725, 40
LAND_H = 720


def district_state(img, x1, y1, x2, y2):
    star_id, star_off = DYNS['raidStar']
    lock_id, lock_off = DYNS['raidLocked']
    cnt_id, cnt_off = DYNS['raidStarCount']
    occ_id, occ_off = DYNS['raidOccupied']
    occ2_id, occ2_off = DYNS['raidOccupied2']
    if collect(img[y1:y2, x1:x2], lock_id, lock_off):
        return -1
    if not collect(img[y1:y2, x1:x2], star_id, star_off):
        return -1
    stars = 0
    h = y2 - y1
    for band in range(3):
        by1 = y1 + h * band // 3
        by2 = y1 + h * (band + 1) // 3
        if collect(img[by1:by2, x1:x2], cnt_id, cnt_off):
            stars += 1
    if stars == 0:
        if collect(img[y1:y2, x1:x2], occ_id, occ_off):
            return -1
        if collect(img[y1:y2, x1:x2], occ2_id, occ2_off):
            return -1
    return min(stars, 2)


def scan_districts(img):
    print('--- B3 九宫格扫描（函数269a + 函数270a） ---')
    anchor = hit(img, 'CapitalRaidMapAnchor')
    if not anchor:
        print('  未命中都城地图锚点 → 无法扫描子城（当前不在突袭地图？）')
        return
    print('  锚点 @(%d,%d)' % anchor)
    states = []
    for i, dx in enumerate(DX):
        tap_y = anchor[1] - dx
        y1 = tap_y - SPAN
        if y1 < 0 or tap_y > LAND_H - 1:
            states.append(-1)
            continue
        states.append(district_state(img, STAR_X1, y1, STAR_X2, tap_y))
    for i, st in enumerate(states):
        label = '不可打（未解锁/有人进攻）' if st < 0 else '%d 星' % st
        print('  子城%d: %s  落点=(%d,%d)' % (i + 1, label, TAP_X[i], anchor[1] - DX[i]))
    best = None
    for want in (2, 1, 0):
        for i, st in enumerate(states):
            if st == want:
                best = (i + 1, want)
                break
        if best:
            break
    print('  选中：%s' % ('无' if not best else '子城%d（%d星）' % best))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--map', action='store_true', help='额外点开都城地图并扫描九宫格')
    args = ap.parse_args()

    gs.ensure_online()
    gs.close_dialogs()
    img = cv2.imread(cap('cap0.png'))
    scan(img, '当前画面')

    if args.map:
        page = page_of_capital(img)
        if page in ('capital', 'capital_map'):
            if page == 'capital':
                print('点「都城地图」按钮 (1165,595)（源 L94457 taps(124,1165) 的横屏换算）')
                gs.tap(1165, 595, dt=2.5)
                img = cv2.imread(cap('cap1.png'))
                boat = hit(img, 'CapitalRaidMapBoat')
                print('  「都城地图小船」%s' % ('命中 @%s' % (boat,) if boat else '未命中 → B2 落点需要校准'))
            scan_districts(img)
        else:
            print('当前不在都城界面（page=%s），先手动/脚本切到都城再跑 --map' % page)


if __name__ == '__main__':
    main()
