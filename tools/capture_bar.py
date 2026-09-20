# -*- coding: utf-8 -*-
"""自动「进攻 → 搜索对手 → 部署阶段」截取部署栏，并放弃战斗回到主村庄。

用法：python capture_bar.py <批次名>
产出：captures/attack/live_bar_<批次名>.png
"""
import os
import sys
import time
import cv2

from game_state import (cap, tap, find_first, page_of, keyevent, F_FEATURE,
                        ensure_online, close_dialogs, parse_file)
from attack_feats import bar_feats

PKG = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   r"..\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage")
U = {f[0]: f for f in parse_file(os.path.join(PKG, 'UIColors.kt'))}
ATTACK_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'captures', 'attack')
B = bar_feats()

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


def ensure_search_opponents(max_rounds=12):
    """复用打鱼逻辑推进到「联机模式」页，返回 (img, 搜索对手按钮位置)。

    每轮：① 已出现「搜索对手」→ 返回；② 练兵页点绿色「进攻！」（特征定位）；
    ③ 主村庄点左下「进攻！」(83,631)；④ 其它页面关弹窗 / BACK 回退。
    """
    for i in range(max_rounds):
        img = cv2.imread(cap('so%d.png' % i))
        # 练兵页的绿色「进攻！」点一次就直接开始搜索/进战斗，此时无需再点"搜索对手"；
        # 必须在这里早退，否则会把刚进入的对战当成"未知页面"按返回键退掉。
        if in_battle(img):
            return img, None
        h = find_first(img, [ATK['SearchOpponents']])
        if h:
            return img, h
        ab = find_first(img, [ATK['AttackButton']])
        if ab:
            print('  第%d轮 练兵页「进攻！」@(%d,%d)' % (i + 1, ab[1], ab[2]))
            tap(ab[1], ab[2], dt=2.2)
            continue
        if find_first(img, _train_troops):
            print('  第%d轮 主村庄「进攻！」@(%d,%d)' % (i + 1, ATTACK_IN_VILLAGE[0], ATTACK_IN_VILLAGE[1]))
            tap(ATTACK_IN_VILLAGE[0], ATTACK_IN_VILLAGE[1], dt=2.2)
            continue
        page = page_of(img)
        print('  第%d轮 page=%s → 关弹窗/BACK' % (i + 1, page))
        if page == 'popup':
            close_dialogs()
        else:
            keyevent(4, dt=1.5)
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
