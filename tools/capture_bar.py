# -*- coding: utf-8 -*-
"""自动「进攻 → 搜索对手 → 部署阶段」截取部署栏，并放弃战斗回到主村庄。

用法：python capture_bar.py <批次名>
产出：captures/attack/live_bar_<批次名>.png
"""
import os
import sys
import time
import cv2

from game_state import (cap, tap, swipe, pinch_in, find_first, page_of, keyevent,
                        F_FEATURE, ensure_online, close_dialogs, parse_file,
                        ensure_main_village)
from attack_feats import bar_feats

PKG = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage")
U = {f[0]: f for f in parse_file(os.path.join(PKG, 'UIColors.kt'))}
ATTACK_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'captures', 'attack')
B = bar_feats()

# `python capture_bar.py <label> pan` -> additionally move the map to the fixed extreme edge with
# the SAME gesture as ZoomSmallMainBase.kt (`isForAttack = true`), so the deploy boundary line lands
# where the fixed deploy coordinates in DeployGeometry expect it. Without the normalisation the
# captured frame is in an arbitrary pan/zoom and cannot be used to validate those coordinates.
NORMALIZE_PAN = len(sys.argv) > 2 and sys.argv[2].lower() in ('pan', 'norm', 'normalize')

# 与 Kotlin 侧 searchOpponentsAndDeployTroops() 完全同源的特征（MainBaseAttackColors）：
#   SearchOpponents = 联机模式页的「搜索对手」；AttackButton = 练兵页右下绿色「进攻！」
#   TrainTroops     = 主村庄 HUD 的「训练部队」，出现即说明在主村庄（Kotlin 点死坐标 83,631）
ATK = {f[0]: f for f in parse_file(os.path.join(PKG, r'mainbase\MainBaseAttackColors.kt'))
       if f[0] in ('SearchOpponents', 'AttackButton')}
ATTACK_IN_VILLAGE = (83, 631)   # 主村庄左下「进攻！」（与 Kotlin 一致）
_train_troops = parse_file(F_FEATURE, {'TrainTroops'})


def in_battle(img):
    return (find_first(img, [B['TroopColorAtDeploymentBar']]) is not None
            or find_first(img, [B['SpellColorAtDeploymentBar']]) is not None
            or find_first(img, [B['SpecialTroopColorAtDeploymentBar']]) is not None)


def ensure_search_opponents(max_rounds=20):
    """复用打鱼逻辑推进到「联机模式」页，返回 (img, 搜索对手按钮位置)。

    每轮：① 已出现「搜索对手」→ 返回；② 练兵页点绿色「进攻！」（特征定位）；
    ③ 主村庄点左下「进攻！」(83,631)；④ 其它页面关弹窗 / BACK 回退。

    与 App 的 searchOpponentsAndDeployTroops 对齐：
      - 本轮只要有锚点命中（acted=True，例如刚点了「进攻！」），就 **不按返回键**，
        而是给游戏留过渡时间。否则会把刚打开的进攻/搜索过渡页用 BACK 关掉，卡在练兵页。
      - 只有当所有锚点都未命中（未知弹窗）时才按返回键，并做 8s 限流，避免连锁误关。
    """
    last_back = 0.0
    for i in range(max_rounds):
        img = cv2.imread(cap('so%d.png' % i))
        # 练兵页的绿色「进攻！」点一次就直接开始搜索/进战斗，此时无需再点"搜索对手"；
        # 必须在这里早退，否则会把刚进入的对战当成"未知页面"按返回键退掉。
        if in_battle(img):
            print('  第%d轮 已进入对战/搜索' % (i + 1))
            return img, None
        h = find_first(img, [ATK['SearchOpponents']])
        if h:
            print('  第%d轮 找到「搜索对手」@(%d,%d)' % (i + 1, h[1], h[2]))
            return img, h
        acted = False
        ab = find_first(img, [ATK['AttackButton']])
        if ab:
            print('  第%d轮 练兵页「进攻！」@(%d,%d)' % (i + 1, ab[1], ab[2]))
            tap(ab[1], ab[2], dt=2.5)
            acted = True
        elif find_first(img, _train_troops):
            print('  第%d轮 主村庄「进攻！」@(%d,%d)' % (i + 1, ATTACK_IN_VILLAGE[0], ATTACK_IN_VILLAGE[1]))
            tap(ATTACK_IN_VILLAGE[0], ATTACK_IN_VILLAGE[1], dt=2.5)
            acted = True
        page = page_of(img)
        print('  第%d轮 page=%s acted=%s' % (i + 1, page, acted))
        if page == 'night_village':
            # 打鱼流程只走主世界；在夜世界时按返回键会弹"退出游戏"，必须先切回主世界。
            print('  第%d轮 在夜世界 → 切回主世界' % (i + 1))
            ensure_main_village()
            continue
        if acted:
            # 刚点了锚点：给游戏留过渡时间，不要按返回键（关键修复）
            time.sleep(1.5)
            continue
        # 所有锚点都未命中：很可能是"既没有红 x、也不是通用对话框"的未知弹窗
        now = time.time()
        if page == 'popup':
            close_dialogs()
        elif now - last_back >= 8.0:
            last_back = now
            print('  第%d轮 页面无法识别 → 按返回键尝试关闭未知弹窗' % (i + 1))
            keyevent(4, dt=1.5)
        else:
            time.sleep(1.0)
    return None, None


def main():
    label = sys.argv[1] if len(sys.argv) > 1 else 'batch'
    os.makedirs(ATTACK_DIR, exist_ok=True)
    for attempt in range(3):
        ensure_online()
        close_dialogs()
        img = cv2.imread(cap('cb0.png'))
        # 主村庄可能弹"欢迎回来/被袭击"→ 绿色确定
        for _ in range(2):
            g = find_first(img, [U['GreenConfirm']])
            if not g:
                break
            tap(g[1], g[2], dt=2.0)
            img = cv2.imread(cap('cb0b.png'))
        print('起始 page =', page_of(img), ' in_battle =', in_battle(img))
        if in_battle(img):
            print('已在战斗中 → 直接截图')
            return save(label)
        img, h = ensure_search_opponents()
        print('搜索对手 =', h)
        if h is None and in_battle(img):
            print('练兵页「进攻！」已直接进入搜索/对战')
            return save(label)
        if not h:
            print('未找到「搜索对手」，重试')
            continue
        tap(h[1], h[2], dt=2.0)
        t0 = time.time()
        while time.time() - t0 < 60:
            time.sleep(2)
            img = cv2.imread(cap('cb2.png'))
            if in_battle(img):
                print('已进入部署阶段（%.0f 秒）' % (time.time() - t0))
                return save(label)
        print('等待进入战斗超时')
    print('流程失败')


def save(label):
    if NORMALIZE_PAN:
        # 完整复现 Kotlin ZoomSmallMainBase.kt (isForAttack=true) 的归一化手势：先两次
        # clickRightBottom + 一次平移，再用双指 pinchIn 把缩放固定到归一化级别（此前 adb 的
        # `input` 只能单指，做不了 pinchIn，导致战斗缩放未归一化；现在走 adb_multitouch 的
        # 双指注入即可对齐），最后把地图拖到固定边缘（同 Kotlin 的 repeat(4)）。
        print('归一化：复现 ZoomSmallMainBase（含双指缩放）...')
        tap(1279, 100, dt=0.6)                       # clickRightBottom(1)
        swipe(200, 500, 950, -500, dur=500, dt=1.0)
        tap(1279, 100, dt=0.6)                       # clickRightBottom(1)
        pinch_in(141, 423, 1052, 352, 638, 365, 638, 365)  # 双指收拢固定缩放级别
        for _ in range(4):
            swipe(911, 134, 0, 720)
        time.sleep(1.0)
    # Wait before capturing the deploy frame: when the account has the "hide obstacles" option,
    # obstacles/decorations disappear ~5s after entering the battle, which makes the deploy
    # boundary line (thin red line) and terrain much easier to recognise.
    print('等待 5 秒（障碍物隐藏后再截图）...')
    time.sleep(5)
    p = cap('live_bar_%s.png' % label)
    dst = os.path.join(ATTACK_DIR, 'live_bar_%s.png' % label)
    cv2.imwrite(dst, cv2.imread(p))
    print('已截图:', dst)
    img = cv2.imread(p)
    end = find_first(img, [B['EndBattle']])
    print('放弃按钮 =', end)
    if end:
        tap(end[1], end[2], dt=1.5)
        time.sleep(1.0)
        img = cv2.imread(cap('cb_end.png'))
        for key in ('GreenConfirm', 'MiddleGreenButton'):
            c = find_first(img, [U[key]])
            if c:
                print('确认放弃 via', key)
                tap(c[1], c[2], dt=2.5)
                break
    time.sleep(2)
    print('结束后 page =', page_of(cv2.imread(cap('cb_after.png'))))


if __name__ == '__main__':
    main()
