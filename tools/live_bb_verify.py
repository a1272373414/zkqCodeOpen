# -*- coding: utf-8 -*-
"""Live check of the builder-base (夜世界) deploy on emulator-5556.

1. Dismiss dialogs and switch to the night village.
2. Tap 进攻！, wait for the battle, and verify the deploy-bar features
   (BuilderBaseMachine / TroopsWithSkills / ExitBattleButton).
3. Give up (放弃) so the account is left back in the night village.

Usage: python tools/live_bb_verify.py
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cv2                                                      # noqa: E402
import game_state as gs                                         # noqa: E402
from game_state import parse_file, find_first, tap, cap, page_of, village_of  # noqa: E402

UI = {f[0]: f for f in parse_file(gs.F_UI)}
BB = {f[0]: f for f in parse_file(os.path.join(gs.PKG, r'builderbase\BuilderBaseAttackColors.kt'))}
ATTACK_BTN = (75, 590)          # 夜世界 HUD 左下「进攻！」


def shot(name):
    return cv2.imread(cap(name))


def features_in(img):
    out = []
    for name in ('ExitBattleButton', 'AttackNow', 'BuilderBaseMachine',
                 'TroopsWithSkills', 'TroopsWithOutSkills', 'NightWitch', 'BuilderBaseBarbarian'):
        feat = BB.get(name)
        if feat and find_first(img, [feat]):
            hit = find_first(img, [feat])
            out.append('%s(%d,%d)' % (name, hit[1], hit[2]))
    return out


def main():
    gs.ensure_online()
    img = shot('bb0.png')
    print('start: page=%s village=%s' % (page_of(img), village_of(img)))
    for _ in range(3):
        g = find_first(img, [UI['GreenConfirm']]) if 'GreenConfirm' in UI else None
        if not g:
            break
        tap(g[1], g[2], dt=2.0)
        img = shot('bb1.png')
    if village_of(img) != 'night':
        print('switch to night village ->', gs.ensure_night_village())
        img = shot('bb2.png')
    print('night village =', village_of(img))

    print('tap 进攻！ @', ATTACK_BTN)
    tap(ATTACK_BTN[0], ATTACK_BTN[1], dt=3.0)
    for i in range(10):
        img = shot('bb_atk%d.png' % i)
        feats = features_in(img)
        print('  round %d: %s' % (i, feats))
        if any(f.startswith('AttackNow') for f in feats):
            hit = find_first(img, [BB['AttackNow']])
            print('  tap AttackNow @(%d,%d)' % (hit[1], hit[2]))
            tap(hit[1], hit[2], dt=2.5)
            continue
        if any(f.startswith(('ExitBattleButton', 'TroopsWithSkills', 'BuilderBaseMachine')) for f in feats):
            print('  -> in battle')
            print('  battle screenshot:', gs.cap('bb_battle.png'))
            break
        time.sleep(1.5)

    # Give up (no troops deployed) and return to the night village.
    img = shot('bb_giveup.png')
    ex = find_first(img, [BB['ExitBattleButton']])
    if ex:
        print('give up @(%d,%d)' % (ex[1], ex[2]))
        tap(ex[1], ex[2], dt=1.5)
        tap(775, 470, dt=2.0)   # confirm exit (see deployAndExit)
    print('after: page=%s village=%s' % (page_of(shot('bb_after.png')), village_of(shot('bb_after.png'))))


if __name__ == '__main__':
    main()
