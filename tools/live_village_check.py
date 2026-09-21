# -*- coding: utf-8 -*-
"""Live：验证①主世界/夜世界识别与切换 ②"清弹窗"不误关训练页 ③未知弹窗的返回键兜底。

用法：python live_village_check.py
产出：tools/captures/scene/*.png
"""
import os
import cv2

from game_state import (cap, tap, keyevent, find_first, page_of, village_of, panel_open,
                        close_dialogs, ensure_night_village, ensure_main_village,
                        _feat_train_troops, _feat_redx, _feat_dialog, _feat_training,
                        _feat_worker_main, _feat_worker_night)

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'captures', 'scene')
os.makedirs(OUT, exist_ok=True)


def shot(name):
    img = cv2.imread(cap(name))
    cv2.imwrite(os.path.join(OUT, name), img)
    return img


def report(tag, img):
    print('--- %s ---' % tag)
    print('  村庄=%-8s 页面=%-13s panel_open=%s'
          % (village_of(img), page_of(img), panel_open(img)))
    for label, feats in (('主世界工人', _feat_worker_main), ('夜世界工人', _feat_worker_night),
                         ('训练部队', _feat_train_troops), ('红x', _feat_redx),
                         ('通用对话框', _feat_dialog), ('训练页', _feat_training)):
        h = find_first(img, feats)
        print('  %-10s %s' % (label, '-' if h is None else '%s@%d,%d' % (h[0], h[1], h[2])))


def main():
    print('===== ① 起始画面 =====')
    report('起始', shot('v_start.png'))

    print('\n===== ② 切到夜世界（木船）=====')
    print('ensure_night_village =', ensure_night_village())
    report('夜世界', shot('v_night.png'))

    print('\n===== ③ 切回主世界 =====')
    print('ensure_main_village =', ensure_main_village())
    report('主世界', shot('v_main.png'))

    print('\n===== ④ 清弹窗不误关训练页 =====')
    img = shot('v_t0.png')
    h = find_first(img, _feat_train_troops)
    if h:
        tap(h[1], h[2], dt=2.5)
    img = shot('v_t1.png')
    report('打开练兵页', img)
    close_dialogs()                       # 期望：识别到训练页 → 跳过 RedX，不关掉训练页
    img = shot('v_t2.png')
    report('close_dialogs() 之后', img)
    tap(1232, 65, dt=2.0)                 # 手动退出练兵页，回到主村庄
    report('退出练兵页', shot('v_t3.png'))

    print('\n===== ⑤ 未知弹窗兜底（返回键）=====')
    keyevent(4, dt=1.8)                   # 主村庄按返回键 → 弹出"退出游戏"确认框（认不出的弹窗）
    img = shot('v_unknown.png')
    report('按返回键后的"认不出的弹窗"', img)
    keyevent(4, dt=1.8)                   # 再按一次返回键 → 期望恢复
    report('再次按返回键之后', shot('v_recovered.png'))


if __name__ == '__main__':
    main()
