#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用 `I:/coc/游戏截图` 里「原生 1280x720 设备截图」验证中文文字识别效果（v2）。

改进点（相对 v1）：
  1) 匹配时按 cell 原始 bbox 尺寸缩放字形（native-scale IoU），保留长宽比，不再
     无脑统一缩到 32x32 丢失形状信息；
  2) 用「面积 + 长宽比」过滤掉 UI 噪声（资源条/图标/底部栏），只保留像汉字的 cell；
  3) 在 x 方向连续、得分>=阈值的 cell 序列里查找期望串，给出命中情况。

用法：cd tools && python validate_ocr_idrive.py
输出：temp/2026-09-28/cnocr_validation_idrive/
"""

import os
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage

SCREEN_DIR = r"I:\coc\游戏截图"
FONT_DIR = r"D:\work\my\cocfz-apk-test\out\font_dump\fonts"
OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       "temp", "2026-09-28", "cnocr_validation_idrive")
os.makedirs(OUT_DIR, exist_ok=True)

FONTS = {
    "ClashRoyaleZH": os.path.join(FONT_DIR, "ClashRoyaleZH-Regular.ttf"),
    "SC_COC": os.path.join(FONT_DIR, "SC_COC_text_appLight.ttf"),
    "DroidFallback": os.path.join(FONT_DIR, "droid_sans_fallback.ttf"),
}

TEST_CASES = [
    ("主世界-部落聊天框-援兵已就绪.png", "援兵已就绪"),
    ("主世界-报告首领-存在已完成的任务.png", "报告首领！"),
    ("每周精选-宝石3.png", "每周精选"),
    ("都城-主界面.png", "都城"),
    ("主世界-打鱼-搜索到1个.png", "搜索到"),
    ("主世界-部落战-准备日1.png", "部落战"),
]

SCORE_THRESH = 0.55  # 单字可信阈值


def binarize_light(rgb):
    arr = np.asarray(rgb)
    r = arr[:, :, 0].astype(int); g = arr[:, :, 1].astype(int); b = arr[:, :, 2].astype(int)
    gray = (r * 299 + g * 587 + b * 114) // 1000
    sat = np.maximum.reduce([r, g, b]) - np.minimum.reduce([r, g, b])
    return (gray >= 200) & (sat <= 70)


def bbox_ink(ink):
    ys, xs = np.where(ink)
    if ys.size == 0:
        return None
    return ys.min(), ys.max(), xs.min(), xs.max()


def render_glyph(char, font_path, size=44):
    try:
        font = ImageFont.truetype(font_path, size)
    except Exception:
        return None
    tmp = Image.new("L", (1, 1), 255)
    d = ImageDraw.Draw(tmp)
    bbox = d.textbbox((0, 0), char, font=font)
    if not bbox or bbox[2] <= bbox[0] or bbox[3] <= bbox[1]:
        return None
    w = bbox[2] - bbox[0] + 4; h = bbox[3] - bbox[1] + 4
    img = Image.new("L", (w, h), 255)
    d = ImageDraw.Draw(img)
    d.text((-bbox[0] + 2, -bbox[1] + 2), char, fill=0, font=font)
    ink = np.array(img) < 128
    if not ink.any():
        return None
    bb = bbox_ink(ink)
    return ink[bb[0]:bb[1] + 1, bb[2]:bb[3] + 1]


def cell_ink_at(ink, y0, y1, x0, x1):
    sub = ink[y0:y1 + 1, x0:x1 + 1]
    bb = bbox_ink(sub)
    if bb is None:
        return None
    return sub[bb[0]:bb[1] + 1, bb[2]:bb[3] + 1]


def resize_ink(ink, h, w):
    img = Image.fromarray((~ink * 255).astype(np.uint8), mode="L")
    img = img.resize((max(1, w), max(1, h)), Image.BILINEAR)
    return ~(np.array(img) > 127)


def iou(a, b):
    inter = np.logical_and(a, b).sum()
    union = np.logical_or(a, b).sum()
    return inter / union if union else 0.0


# 模板库：{char: {font: ink_bool}}
GLYPH_CACHE = {}

def get_glyph(char):
    if char in GLYPH_CACHE:
        return GLYPH_CACHE[char]
    entry = {}
    for fname, fpath in FONTS.items():
        g = render_glyph(char, fpath)
        if g is not None:
            entry[fname] = g
    GLYPH_CACHE[char] = entry
    return entry


def extract_cells(rgb):
    ink = binarize_light(rgb)
    if not ink.any():
        return []
    labeled, n = ndimage.label(ink, structure=np.ones((3, 3)))
    cells = []
    for i in range(1, n + 1):
        ys, xs = np.where(labeled == i)
        if ys.size < 30:  # 太小：噪声/小图标
            continue
        y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
        h, w = y1 - y0 + 1, x1 - x0 + 1
        # 汉字通常近似方形，且不会过分细长/宽扁
        if h < 12 or w < 8 or h > 90 or w > 90:
            continue
        aspect = w / h
        if aspect < 0.45 or aspect > 1.8:
            continue
        sub = cell_ink_at(ink, y0, y1, x0, x1)
        if sub is None or sub.size == 0:
            continue
        cells.append((sub, x0, y0, w, h))
    # 行分组 + 行内按 x 排序
    cells.sort(key=lambda c: (c[2], c[1]))
    lines = []
    for c in cells:
        if lines and abs(c[2] - min(x[2] for x in lines[-1])) < 14:
            lines[-1].append(c)
        else:
            lines.append([c])
    ordered = []
    for line in sorted(lines, key=lambda l: min(c[2] for c in l)):
        for c in sorted(line, key=lambda c: c[1]):
            ordered.append(c)
    return ordered


def match_cell(cell_sub, expected_chars):
    best = (0.0, None, None)
    for ch in expected_chars:
        tmpls = get_glyph(ch)
        for fname, g in tmpls.items():
            gh, gw = g.shape
            # 把字形缩到 cell 的尺寸做 native-scale 比较
            g_resized = resize_ink(g, cell_sub.shape[0], cell_sub.shape[1])
            s = iou(cell_sub, g_resized)
            if s > best[0]:
                best = (s, ch, fname)
    return best


def find_substring(cells, expected):
    """在按 x 排序的 cell 序列里，找期望串作为连续高置信子串的命中情况。"""
    seq = []
    for (sub, x, y, w, h) in cells:
        sc, ch, fnt = match_cell(sub, list(expected))
        seq.append((ch, sc, fnt, (x, y, w, h)))
    # 连续匹配期望串
    exp = list(expected)
    best_run = None
    n = len(exp)
    for i in range(len(seq) - n + 1):
        run = seq[i:i + n]
        if all(run[k][0] == exp[k] and run[k][1] >= SCORE_THRESH for k in range(n)):
            score = sum(r[1] for r in run) / n
            best_run = (i, score, run)
            break
    return seq, best_run


def validate_case(fname, expected):
    path = os.path.join(SCREEN_DIR, fname)
    if not os.path.exists(path):
        print(f"  [跳过] 文件不存在: {path}")
        return
    rgb = Image.open(path).convert("RGB")
    cells = extract_cells(rgb)
    if not cells:
        print(f"  [跳过] 未切出可信文字 cell（可能该图文字非浅色/被过滤）")
        return
    seq, run = find_substring(cells, expected)
    print(f"\n[测试] {fname}")
    print(f"  期望文字: {expected}   切出可信 cell: {len(cells)}")
    if run:
        i, score, rseq = run
        print(f"  >>> 命中！连续子串位置={i}，平均得分={score:.2f}")
        for k, (ch, sc, fnt, (x, y, w, h)) in enumerate(rseq):
            print(f"      [{k}] '{ch}' 得分={sc:.2f} 字体={fnt} @({x},{y},{w}x{h})")
    else:
        print(f"  >>> 未以连续高置信子串命中；逐 cell 最佳识别（前 20 个）：")
        for ch, sc, fnt, (x, y, w, h) in seq[:20]:
            print(f"      '{ch}' 得分={sc:.2f} 字体={fnt} @({x},{y},{w}x{h})")
        # 期望字是否零散出现
        for ch in set(expected):
            cnt = sum(1 for c in seq if c[0] == ch and c[1] >= SCORE_THRESH)
            print(f"      期望字 '{ch}' 出现 {cnt} 次(>=阈值)")


def main():
    print("=" * 64)
    print("中文文字识别验证 v2（I:\\coc\\游戏截图，原生1280x720）")
    print("=" * 64)
    for fname, expected in TEST_CASES:
        sys.stdout.flush()
        validate_case(fname, expected)


if __name__ == "__main__":
    main()
