# -*- coding: utf-8 -*-
"""掉线弹窗长测：挂机等待"还在吗？…重新载入游戏"弹窗（超时约 10 分钟），验证特征识别 + 自动重连。

用法：python wait_disconnect_test.py [最长等待分钟数=12]
说明：期间不要操作模拟器（避免刷新游戏的活动计时）。
"""
import sys
import time
import cv2

from game_state import (cap, find_first, page_of, ensure_online, _feat_reload, disconnect_dialog)


def main():
    minutes = float(sys.argv[1]) if len(sys.argv) > 1 else 12.0
    deadline = time.time() + minutes * 60
    print('开始等待掉线弹窗，最多 %.0f 分钟（期间请勿操作模拟器）…' % minutes)
    i = 0
    found = False
    while time.time() < deadline:
        i += 1
        img = cv2.imread(cap('wd_%03d.png' % (i % 30)))
        hit = find_first(img, _feat_reload)
        gray = disconnect_dialog(img)
        print('  第%3d次(%s): 特征=%s 灰底判定=%s page=%s'
              % (i, time.strftime('%H:%M:%S'), hit, gray, page_of(img)))
        if hit or gray:
            found = True
            break
        time.sleep(20)
    if not found:
        print('%.0f 分钟内未出现掉线弹窗（可加大等待或先手动操作一次再等）' % minutes)
        return
    t0 = time.time()
    print('已发现掉线弹窗 → 调用 ensure_online() 自动重连 …')
    ensure_online()
    for _ in range(12):
        time.sleep(10)
        img = cv2.imread(cap('wd_after.png'))
        p = page_of(img)
        print('  重连 %3d 秒后 page=%s' % (int(time.time() - t0), p))
        if p in ('main_village', 'training'):
            print('重连成功（%.0f 秒）' % (time.time() - t0))
            return
    print('重连后仍未回到已知界面，请检查')


if __name__ == '__main__':
    main()
