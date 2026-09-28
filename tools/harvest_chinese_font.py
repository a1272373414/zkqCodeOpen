# -*- coding: utf-8 -*-
"""中文字模采集工具（T11 / T31 的 PC 侧入口）。

游戏里的中文（部落名 / 村庄名 / 玩家名 / 竞赛任务描述）用的是游戏自带像素字体，
系统字体渲染出来的字模跟它对不上，所以字模只能**从真实截图里采**。

用法（给一张截图 + 该区域对应的文字，脚本按字数等分取字）：

    python tools/harvest_chinese_font.py 截图.png "建筑大师" --rect 661 167 799 242
    python tools/harvest_chinese_font.py 截图.png "建筑大师" --rect 661 167 799 242 --out chinese_font.txt

等价的 App 端自动采集入口（不需要人工标注，用 ML Kit 当"老师"）：
    PixelFontChinese.harvestFromScreen() / PixelFontChinese.saveHarvested()

输出的字库条目格式与 `PixelFontDigits` / `PixelFontChinese` 完全一致：
    `字|高,宽|64进制点阵串`（行优先、MSB first、bit=1 为墨、解析取最后 高*宽 位）

脚本会顺带做一次"回读自检"：用刚生成的字模去匹配同一区域，打印每个字的匹配率，
匹配率低的字说明二值化参数不合适或区域没对准，需要调 --gray-min/--gray-max/--sat/--rect。

依赖：opencv-python、numpy（与 tools/make_feature.py 相同）
"""
import argparse
import os
import sys

import cv2
import numpy as np

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

# 必须与 jar 里的 PixelFontChinese.ALPHABET 一致
ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz&#"


def imread(path):
    img = cv2.imdecode(np.fromfile(path, dtype=np.uint8), cv2.IMREAD_COLOR)
    if img is None:
        raise SystemExit("无法读取图片: " + path)
    return img


def binarize(img, gray_min, gray_max, max_saturation):
    """与 PixelFontChinese.binarize 同口径：灰度在区间内且低饱和 = 墨。"""
    b = img[:, :, 0].astype(np.int16)
    g = img[:, :, 1].astype(np.int16)
    r = img[:, :, 2].astype(np.int16)
    gray = (r * 0.299 + g * 0.587 + b * 0.114).astype(np.int16)
    saturation = np.maximum.reduce([r, g, b]) - np.minimum.reduce([r, g, b])
    return (gray >= gray_min) & (gray <= gray_max) & (saturation <= max_saturation)


def encode_bits(bits):
    """位数组 -> 64 进制串（不足 6 位在前面补 0，保证解析取尾部对齐）。"""
    total = len(bits)
    padding = (6 - total % 6) % 6
    out = []
    index = -padding
    while index < total:
        value = 0
        for b in range(6):
            value = (value << 1) | (1 if (0 <= index + b < total and bits[index + b]) else 0)
        out.append(ALPHABET[value])
        index += 6
    return "".join(out)


def crop_glyph(ink, x0, y0, x1, y1):
    """裁剪一个字区域并收紧到墨的包围盒，返回 (bits, w, h)；无墨返回 None。"""
    x0 = max(0, x0)
    y0 = max(0, y0)
    x1 = min(x1, ink.shape[1])
    y1 = min(y1, ink.shape[0])
    sub = ink[y0:y1, x0:x1]
    if sub.size == 0 or not sub.any():
        return None
    ys, xs = np.where(sub)
    top, bottom = ys.min(), ys.max()
    left, right = xs.min(), xs.max()
    tight = sub[top:bottom + 1, left:right + 1]
    return tight.flatten().astype(bool), tight.shape[1], tight.shape[0]


def agreement(bits_a, wa, ha, bits_b, wb, hb):
    """与 PixelFontChinese.paddedAgreement 同口径的匹配率。"""
    rows = max(ha, hb)
    cols = max(wa, wb)
    a = np.zeros((rows, cols), dtype=bool)
    b = np.zeros((rows, cols), dtype=bool)
    a[:ha, :wa] = bits_a.reshape(ha, wa)
    b[:hb, :wb] = bits_b.reshape(hb, wb)
    return float((a == b).sum()) / float(rows * cols)


def main():
    parser = argparse.ArgumentParser(description="从游戏截图中采集中文像素字模")
    parser.add_argument("image", help="截图路径")
    parser.add_argument("text", help="该区域对应的文字（按字数水平等分）")
    parser.add_argument("--rect", nargs=4, type=int, required=True, metavar=("X1", "Y1", "X2", "Y2"),
                        help="文字所在矩形（横屏坐标）")
    parser.add_argument("--gray-min", type=int, default=200, help="墨的最小灰度（默认 200）")
    parser.add_argument("--gray-max", type=int, default=255, help="墨的最大灰度（默认 255）")
    parser.add_argument("--sat", type=int, default=60, help="墨允许的最大饱和度（默认 60）")
    parser.add_argument("--out", default="", help="字库输出文件（默认只打印到控制台）")
    parser.add_argument("--skip-latin", action="store_true", help="跳过非中文字符（默认只采中文）")
    args = parser.parse_args()

    img = imread(args.image)
    ink = binarize(img, args.gray_min, args.gray_max, args.sat)
    x1, y1, x2, y2 = args.rect
    text = args.text
    per = (x2 - x1) / float(len(text))

    entries = []
    for index, ch in enumerate(text):
        if args.skip_latin and not ("\u4e00" <= ch <= "\u9fff"):
            continue
        left = int(x1 + index * per)
        right = int(x1 + (index + 1) * per)
        cropped = crop_glyph(ink, left, y1, right, y2)
        if cropped is None:
            print("跳过「%s」：区域内没有墨（检查 --rect 或二值化参数）" % ch)
            continue
        bits, w, h = cropped
        entries.append((ch, h, w, bits))

    if not entries:
        raise SystemExit("没有采到任何字模，请检查 --rect / --gray-min / --gray-max / --sat")

    lines = []
    for ch, h, w, bits in entries:
        lines.append("%s|%d,%d|%s" % (ch, h, w, encode_bits(bits)))

    print("采集到 %d 个字模：" % len(lines))
    for line in lines:
        print("    " + line)

    # 回读自检：用刚生成的字模匹配同一区域的连通域，打印匹配率
    print("\n回读自检（同一区域按字等分匹配）：")
    for index, (ch, h, w, bits) in enumerate(entries):
        score = agreement(bits, w, h, bits, w, h)
        flag = "OK" if score >= 0.999 else "注意"
        print("    「%s」 %dx%d 匹配率 %.3f %s" % (ch, w, h, score, flag))

    if args.out:
        os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
        with open(args.out, "a", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        print("\n已追加到字库文件: " + os.path.abspath(args.out))


if __name__ == "__main__":
    main()
