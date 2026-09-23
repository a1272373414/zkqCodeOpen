# -*- coding: utf-8 -*-
"""自动生成 ColorSchema 特征（替代"手工逐点量颜色"）。

用法示例：
    # 从一张截图 + 矩形区域生成特征（自动选主色与偏移点）
    python tools/make_feature.py 截图.png 1080 42 1210 70 --name BuilderBaseEditMode

    # 同一按钮的多张截图（提高鲁棒性）+ 负样本目录（自检误命中）
    python tools/make_feature.py pos1.png pos2.png 1080 42 1210 70 \
        --name Foo --neg-dir tools/captures --similar 0.9 --points 6

输出：可直接粘贴进 `*Colors.kt` 的 ColorSchema.parse(...) 代码，并打印正/负样本命中自检结果。

算法与 App 的 findMultiColors 对齐：
  主色 = 矩形内出现最多的颜色（量化后众数）；
  锚点 = 矩形内从左上扫描到的第一个主色点（App 同序）；
  偏移点 = 矩形内跨正样本都稳定的采样点（相对锚点），贪心选分散的 N 个；
  颜色串按项目约定写成 **BGR** 六位十六进制。
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


def imread_u(path):
    return cv2.imdecode(np.fromfile(path, dtype=np.uint8), cv2.IMREAD_COLOR)


def bgr_hex(px):
    return "%02X%02X%02X" % (int(px[0]), int(px[1]), int(px[2]))


def hex_arr(h):
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], dtype=np.int16)


def collect(scan, anchor, offs, th):
    """与 App 同口径：区域内从左到右、从上到下找第一个"主色+全部偏移色"命中点。"""
    if scan is None or scan.size == 0:
        return None
    mask = (np.abs(scan.astype(np.int16) - anchor) <= th).all(axis=2)
    ys, xs = np.where(mask)
    for y, x in zip(ys, xs):
        ok = True
        for dx, dy, c in offs:
            tx, ty = x + dx, y + dy
            if tx < 0 or ty < 0 or tx >= scan.shape[1] or ty >= scan.shape[0]:
                ok = False
                break
            if (np.abs(scan[ty, tx].astype(np.int16) - c) > th).any():
                ok = False
                break
        if ok:
            return (int(x), int(y))
    return None


def main_color_of(region, quant=4):
    q = (region.reshape(-1, 3).astype(np.int32) // quant * quant)
    colors, counts = np.unique(q, axis=0, return_counts=True)
    main = colors[int(np.argmax(counts))]
    # 用真实像素（不量化）的平均值作为主色，减少量化误差
    mask = (np.abs(region.astype(np.int16) - main.astype(np.int16)) <= quant).all(axis=2)
    if mask.any():
        main = region[mask].mean(axis=0).round().astype(np.int16)
    return main


def anchor_of(region, main, th):
    mask = (np.abs(region.astype(np.int16) - main) <= th).all(axis=2)
    ys, xs = np.where(mask)
    if len(xs) == 0:
        return None
    return int(xs[0]), int(ys[0])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("images", nargs="+", help="正样本截图路径（同一区域/同一按钮）")
    ap.add_argument("x1", type=int)
    ap.add_argument("y1", type=int)
    ap.add_argument("x2", type=int)
    ap.add_argument("y2", type=int)
    ap.add_argument("--name", default="NewFeature", help="生成的 Kotlin 属性名")
    ap.add_argument("--label", default="", help="ColorSchema 的显示名（默认同 --name）")
    ap.add_argument("--similar", type=float, default=0.9, help="相似度（0.9 -> 阈值 25）")
    ap.add_argument("--points", type=int, default=6, help="偏移点数量")
    ap.add_argument("--neg-dir", default="", help="负样本目录（递归找 *.png 自检误命中）")
    ap.add_argument("--neg-limit", type=int, default=400, help="负样本最多检查张数")
    args = ap.parse_args()

    th = int(round(255 * (1.0 - args.similar)))
    x1, y1, x2, y2 = args.x1, args.y1, args.x2, args.y2

    positives = []
    for pat in args.images:
        # 支持通配符（避免在命令行里直接传中文文件名）
        matched = sorted(glob.glob(pat)) or [pat]
        for p in matched:
            img = imread_u(p)
            if img is None:
                print("!! 读不到图片:", p)
                return 1
            positives.append((p, img))
    if not positives:
        print("!! 没有可用正样本")
        return 1

    regions = [img[y1:y2, x1:x2] for _, img in positives]
    main = main_color_of(regions[0])

    anchors = [anchor_of(r, main, th) for r in regions]
    if anchors[0] is None:
        print("!! 矩形内没有主色 %s，请换区域或降低 --similar" % bgr_hex(main))
        return 1

    # 候选偏移：网格采样，保留跨正样本都稳定的点
    h, w = regions[0].shape[:2]
    step = max(2, min(w, h) // 8)
    candidates = [(dx, dy) for dy in range(0, h, step) for dx in range(0, w, step)]
    stable = []
    for dx, dy in candidates:
        colors = []
        ok = True
        for reg, (ax, ay) in zip(regions, anchors):
            if ax is None:
                ok = False
                break
            tx, ty = ax + dx, ay + dy
            if tx < 0 or ty < 0 or tx >= reg.shape[1] or ty >= reg.shape[0]:
                ok = False
                break
            colors.append(reg[ty, tx])
        if not ok:
            continue
        # 所有正样本上颜色一致（互差 <= th）
        base = colors[0].astype(np.int16)
        if all((np.abs(c.astype(np.int16) - base) <= th).all() for c in colors):
            stable.append((dx, dy, base.astype(np.int16)))

    if not stable:
        print("!! 没有找到稳定偏移点，请缩小矩形或降低 --similar")
        return 1

    # 贪心挑分散的点（离已选点越远越好），第一个点取离锚点最近的稳定点
    chosen = []
    remaining = list(stable)
    remaining.sort(key=lambda t: t[0] * t[0] + t[1] * t[1])
    chosen.append(remaining.pop(0))
    while remaining and len(chosen) < args.points:
        best = None
        best_d = -1
        for cand in remaining:
            d = min((cand[0] - c[0]) ** 2 + (cand[1] - c[1]) ** 2 for c in chosen)
            if d > best_d:
                best_d, best = d, cand
        chosen.append(best)
        remaining.remove(best)

    offs = [(dx, dy, c) for dx, dy, c in chosen]
    off_str = ",".join("%d|%d|%s" % (dx, dy, bgr_hex(c)) for dx, dy, c in offs)
    label = args.label or args.name

    print("\n=== 生成结果（主色 BGR %s，阈值 %d，锚点 %s）" % (bgr_hex(main), th, anchors[0]))
    print("override val %s = ColorSchema.parse(" % args.name)
    print("    %d, %d, %d, %d, \"%s\"," % (x1, y1, x2, y2, bgr_hex(main)))
    print("    \"%s\"," % off_str)
    print("    0, %s, \"%s\"" % (args.similar, label))
    print(")\n")
    print("偏移点：", ", ".join("(%d,%d)%s" % (dx, dy, bgr_hex(c)) for dx, dy, c in offs))

    # 正样本自检
    print("\n=== 正样本自检")
    for (path, img) in positives:
        reg = img[y1:y2, x1:x2]
        hit = collect(reg, main, offs, th)
        print("  %-70s %s" % (os.path.basename(path), "命中 %s" % (hit,) if hit else "**未命中**"))

    # 负样本自检
    if args.neg_dir:
        print("\n=== 负样本自检（命中即为误判）")
        neg_files = sorted(glob.glob(os.path.join(args.neg_dir, "**", "*.png"), recursive=True))
        pos_set = {os.path.abspath(p) for p, _ in positives}
        checked = 0
        bad = 0
        for nf in neg_files:
            if os.path.abspath(nf) in pos_set:
                continue
            if checked >= args.neg_limit:
                break
            img = imread_u(nf)
            if img is None or img.shape[0] <= y2 or img.shape[1] <= x2:
                continue
            checked += 1
            reg = img[y1:y2, x1:x2]
            if collect(reg, main, offs, th):
                bad += 1
                print("  误命中:", os.path.basename(nf))
        print("  检查 %d 张负样本，误命中 %d 张" % (checked, bad))
    return 0


if __name__ == "__main__":
    sys.exit(main())
