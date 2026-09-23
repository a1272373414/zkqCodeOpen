# -*- coding: utf-8 -*-
"""从截图裁剪一张按钮/图标模板，供 App 端 `TemplateMatcher`（NCC 模板匹配）使用。

用法：
    python tools/make_template.py 截图.png 1080 42 1210 70 btn_bb_edit

输出：app/src/main/assets/templates/<name>.png
      （NCC 用灰度匹配，模板应贴紧按钮本体，避免带入大片背景）

可选：--verify 负样本目录，用 OpenCV 的同口径 NCC 立刻自检该模板在该目录的误命中。
"""
import argparse
import glob
import os
import sys

import cv2
import numpy as np

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "templates")


def imread_u(path):
    return cv2.imdecode(np.fromfile(path, dtype=np.uint8), cv2.IMREAD_COLOR)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("image")
    ap.add_argument("x1", type=int)
    ap.add_argument("y1", type=int)
    ap.add_argument("x2", type=int)
    ap.add_argument("y2", type=int)
    ap.add_argument("name", help="模板名（不含扩展名），运行时按此名加载")
    ap.add_argument("--threshold", type=float, default=0.7, help="NCC 判定阈值（自检用）")
    ap.add_argument("--verify", default="", help="负样本目录（递归 *.png），自检误命中")
    ap.add_argument("--verify-region", nargs=4, type=int, default=None,
                    metavar=("X1", "Y1", "X2", "Y2"),
                    help="自检搜索区域（默认整图；运行时也会限区域，区域越准误命中越少）")
    args = ap.parse_args()

    matches = sorted(glob.glob(args.image)) or [args.image]
    img = imread_u(matches[0])
    if img is None:
        print("!! 读不到图片:", matches[0])
        return 1
    x1, y1, x2, y2 = args.x1, args.y1, args.x2, args.y2
    if not (0 <= x1 < x2 <= img.shape[1] and 0 <= y1 < y2 <= img.shape[0]):
        print("!! 矩形越界:", (x1, y1, x2, y2), "图片尺寸", img.shape[1], "x", img.shape[0])
        return 1

    template = img[y1:y2, x1:x2]
    os.makedirs(OUT_DIR, exist_ok=True)
    out = os.path.join(OUT_DIR, args.name + ".png")
    ok, buf = cv2.imencode(".png", template)
    if not ok:
        print("!! PNG 编码失败")
        return 1
    with open(out, "wb") as f:
        f.write(buf.tobytes())
    print("模板已保存:", out, "尺寸", template.shape[1], "x", template.shape[0])

    # 自检：同一图上应能匹配回原位置
    res = cv2.matchTemplate(img, template, cv2.TM_CCOEFF_NORMED)
    _, max_val, _, max_loc = cv2.minMaxLoc(res)
    print("自检(原图): score=%.3f center=(%d,%d)" % (
        max_val, max_loc[0] + template.shape[1] // 2, max_loc[1] + template.shape[0] // 2))

    if args.verify:
        vx = args.verify_region
        pos_set = {os.path.abspath(p) for p in matches}
        files = sorted(glob.glob(os.path.join(args.verify, "**", "*.png"), recursive=True))
        bad = 0
        checked = 0
        for f in files:
            if os.path.abspath(f) in pos_set:
                continue
            im = imread_u(f)
            if im is None:
                continue
            if vx:
                sxx1, syy1, sxx2, syy2 = vx
                if im.shape[1] < sxx2 or im.shape[0] < syy2:
                    continue
                search = im[syy1:syy2, sxx1:sxx2]
            else:
                search = im
            th, tw = template.shape[:2]
            if search.shape[0] < th or search.shape[1] < tw:
                continue
            checked += 1
            r = cv2.matchTemplate(search, template, cv2.TM_CCOEFF_NORMED)
            _, mv, _, ml = cv2.minMaxLoc(r)
            if mv >= args.threshold:
                bad += 1
                ox, oy = (vx[0], vx[1]) if vx else (0, 0)
                print("  误命中(>=%.2f, %.3f) @(%d,%d): %s" % (
                    args.threshold, mv, ml[0] + ox + tw // 2, ml[1] + oy + th // 2, os.path.basename(f)))
        print("负样本自检：检查 %d 张，误命中 %d 张（区域=%s）" % (checked, bad, vx or "整图"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
