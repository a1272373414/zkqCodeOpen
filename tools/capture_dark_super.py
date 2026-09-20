# -*- coding: utf-8 -*-
"""From the 圣水兵 tab, swipe the troop carousel left step by step and capture
each step, reporting how many 圣水兵/黑油/超级 training-button features are visible.
We use these captures to locate the 黑油兵 group and 超级兵 group for feature generation.
"""
import cv2
from game_state import (recover_to_training, tap, cap, parse_file, count_hits,
                        F_TRAIN_BTN, F_BLACK, F_SUPER, swipe)

elixir = parse_file(F_TRAIN_BTN)
black = parse_file(F_BLACK)
super_ = parse_file(F_SUPER)

SWIPE = (1120, 545, 180, 545, 420)  # drag right->left ~940px to move carousel left


def report(name):
    img = cv2.imread(cap(name))
    print('%-22s  elixir=%2d  black=%2d  super=%2d' %
          (name, count_hits(img, elixir), count_hits(img, black), count_hits(img, super_)))


if __name__ == "__main__":
    assert recover_to_training(), "could not reach training page"
    tap(891, 234)                          # ensure 圣水兵 tab
    report("c0_elixir.png")
    for i in range(5):
        swipe(*SWIPE)
        report("c%d.png" % (i + 1))
