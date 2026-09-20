# -*- coding: utf-8 -*-
"""造兵测试前置：把训练队列清空（避免队列满→卡片变灰影响定位测试）。

用法：python clear_queues.py
说明：复用 MainBaseTraining.kt 的 DeleteAll1/2/3（各队列的"删除全部"按钮）与 MiddleGreenYes（确认弹窗），
      在训练页反复找"删除全部"→点→确认，直到找不到为止。
"""
import cv2

from game_state import (cap, tap, parse_file, find_first, page_of, panel_open, OUT,
                        _feat_train_troops, F_TRAINING, ensure_online)


FEATS = parse_file(F_TRAINING, {"DeleteAll1", "DeleteAll2", "DeleteAll3", "MiddleGreenYes"})
DELETES = [f for f in FEATS if f[0].startswith("DeleteAll")]
CONFIRM = next(f for f in FEATS if f[0] == "MiddleGreenYes")


def clear(img):
    """返回是否清掉了一个队列。"""
    hit = find_first(img, DELETES)
    if not hit:
        return False
    print('  找到 %s @ (%d,%d) → 点击' % (hit[0], hit[1], hit[2]))
    tap(hit[1], hit[2], dt=0.9)
    img = cv2.imread(cap('cq_confirm.png'))
    yes = find_first(img, [CONFIRM])
    if yes:
        tap(yes[1], yes[2], dt=0.9)
        print('  确认清空（绿色确定）')
    return True


def main():
    ensure_online()
    img = cv2.imread(cap('cq0.png'))
    if page_of(img) == 'main_village':
        h = find_first(img, _feat_train_troops)
        if h:
            print('主村庄 → 打开训练页 (TrainTroops@%d,%d)' % (h[1], h[2]))
            tap(h[1], h[2], dt=2.5)
            img = cv2.imread(cap('cq1.png'))
    if panel_open(img):
        print('选兵面板打开着 → 先关闭 (219,139)')
        tap(219, 139, dt=1.0)
    cleared = 0
    for _ in range(6):
        img = cv2.imread(cap('cq%d.png' % (cleared + 2)))
        if not clear(img):
            break
        cleared += 1
    print('已清空 %d 个队列' % cleared)


if __name__ == '__main__':
    main()
