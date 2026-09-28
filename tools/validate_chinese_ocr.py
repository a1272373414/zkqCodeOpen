#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用现有游戏截图验证中文文字识别效果。

由于 PC 端无法运行 ML Kit，这里验证的是项目里 PixelFontChinese 的「预处理 + 连通域 + 字形匹配」
管线能否在真实截图上把中文/数字/标点正确分出来，并用 dumped 的 TTF 字体做字形模板匹配，
给出一份可量化的匹配报告和可视化图片。

用法：
    cd tools
    python validate_chinese_ocr.py

输出到：d:/work/my/zkqfz/zkqCodeOpen/temp/cnocr_validation/
"""

import os
import re
from dataclasses import dataclass, field
from typing import List, Tuple, Optional

from PIL import Image, ImageDraw, ImageFont


def resource_path(rel: str) -> str:
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, rel)


OUT_DIR = resource_path("temp/cnocr_validation")
os.makedirs(OUT_DIR, exist_ok=True)


def binarize(img: Image.Image, threshold: int = 200, invert: bool = True) -> Image.Image:
    """与项目 TextRecognizer/ChineseTextReader 的默认参数对齐：灰度 + 阈值 + 反色。"""
    gray = img.convert("L")
    if invert:
        return gray.point(lambda p: 255 if p < threshold else 0)
    return gray.point(lambda p: 0 if p < threshold else 255)


@dataclass
class Component:
    left: int
    top: int
    right: int
    bottom: int
    pixels: List[Tuple[int, int]] = field(default_factory=list)

    @property
    def width(self) -> int: return self.right - self.left
    @property
    def height(self) -> int: return self.bottom - self.top
    @property
    def area(self) -> int: return len(self.pixels)
    @property
    def cx(self) -> int: return (self.left + self.right) // 2
    @property
    def cy(self) -> int: return (self.top + self.bottom) // 2


def find_components(binary: Image.Image, min_area: int = 20, max_area: int = 5000) -> List[Component]:
    """8 连通域，和 PixelFontChinese.labelComponents 一致。"""
    w, h = binary.size
    data = list(binary.getdata())
    ink = [1 if p < 128 else 0 for p in data]
    label = [0] * (w * h)
    components: dict[int, Component] = {}
    next_label = 1

    for y in range(h):
        for x in range(w):
            idx = y * w + x
            if not ink[idx]:
                continue
            neighbors = []
            for dx, dy in ((0, -1), (-1, 0), (-1, -1), (1, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h:
                    ni = ny * w + nx
                    if label[ni]:
                        neighbors.append(label[ni])
            if not neighbors:
                label[idx] = next_label
                next_label += 1
            else:
                ml = min(neighbors)
                label[idx] = ml
                for nl in neighbors:
                    if nl != ml:
                        # union: relabel all with nl to ml
                        for i in range(idx + 1):
                            if label[i] == nl:
                                label[i] = ml
                        for i in range(idx, w * h):
                            if label[i] == nl:
                                label[i] = ml

    # collect
    for y in range(h):
        for x in range(w):
            idx = y * w + x
            lb = label[idx]
            if not lb:
                continue
            if lb not in components:
                components[lb] = Component(left=x, top=y, right=x + 1, bottom=y + 1, pixels=[])
            c = components[lb]
            c.left = min(c.left, x)
            c.top = min(c.top, y)
            c.right = max(c.right, x + 1)
            c.bottom = max(c.bottom, y + 1)
            c.pixels.append((x, y))

    return [c for c in components.values()
            if min_area <= c.area <= max_area and c.width >= 3 and c.height >= 5]


def merge_components_into_cells(components: List[Component]) -> List[Component]:
    """把相邻的笔画/偏旁合并成一个字格（与 PixelFontChinese 的合并逻辑一致）。"""
    if not components:
        return []
    sorted_comps = sorted(components, key=lambda c: c.left)
    cells: List[Component] = []
    for comp in sorted_comps:
        if not cells:
            cells.append(comp)
            continue
        last = cells[-1]
        gap = comp.left - last.right
        # 合并候选条件：合并后整体近似方形（宽度 <= 高度*1.5），且水平/垂直相邻
        new_left = min(last.left, comp.left)
        new_top = min(last.top, comp.top)
        new_right = max(last.right, comp.right)
        new_bottom = max(last.bottom, comp.bottom)
        new_w = new_right - new_left
        new_h = new_bottom - new_top
        if new_w > new_h * 1.5 + 4:
            cells.append(comp)
            continue
        if gap > 0:
            ref_h = max(last.height, comp.height)
            max_gap = max(ref_h // 4, 2)
            if gap > max_gap:
                cells.append(comp)
                continue
        overlap_top = max(last.top, comp.top)
        overlap_bottom = min(last.bottom, comp.bottom)
        overlap_h = overlap_bottom - overlap_top
        min_h = min(last.height, comp.height)
        if overlap_h < min_h * 0.4:
            cells.append(comp)
            continue
        # 合并到 last（pixels 保持绝对坐标，component_bitmap 会再换算）
        new_pixels = list(last.pixels) + list(comp.pixels)
        cells[-1] = Component(new_left, new_top, new_right, new_bottom, new_pixels)
    return cells


def group_into_lines(components: List[Component], y_tol: int = 10) -> List[List[Component]]:
    """按中心 y 聚类成行，行内按 x 排序。"""
    if not components:
        return []
    components = sorted(components, key=lambda c: c.cy)
    lines: List[List[Component]] = []
    current = [components[0]]
    for c in components[1:]:
        if abs(c.cy - current[-1].cy) <= y_tol:
            current.append(c)
        else:
            lines.append(sorted(current, key=lambda x: x.cx))
            current = [c]
    lines.append(sorted(current, key=lambda x: x.cx))
    return lines


def component_bitmap(comp: Component, binary: Image.Image) -> Image.Image:
    """从二值图中切出单个 component 的位图。"""
    w, h = binary.size
    ink = [1 if p < 128 else 0 for p in binary.getdata()]
    out = Image.new("1", (comp.width, comp.height))
    out_pixels = out.load()
    for x, y in comp.pixels:
        out_pixels[x - comp.left, y - comp.top] = 0
    return out


def render_glyph(char: str, font: ImageFont.FreeTypeFont, pad: int = 2) -> Image.Image:
    """渲染单个字符，二值化。返回 1-bit PIL Image（黑色前景）。"""
    bbox = font.getbbox(char)
    if not bbox:
        return Image.new("1", (1, 1))
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    img = Image.new("L", (w + pad * 2, h + pad * 2), 255)
    d = ImageDraw.Draw(img)
    d.text((pad - bbox[0], pad - bbox[1]), char, fill=0, font=font)
    # 二值化：灰度 0(纯黑) → 0，其余 → 255
    return img.point(lambda p: 0 if p < 128 else 255)


def bitmap_to_array(img: Image.Image) -> List[int]:
    return [1 if p == 0 else 0 for p in img.getdata()]


def padded_agreement(a: Image.Image, b: Image.Image) -> float:
    """把两张 1-bit 图缩放到同尺寸，算前景像素的 IOU 式相似度。"""
    if a.size == (1, 1) or b.size == (1, 1):
        return 0.0
    # 统一尺寸
    mw = max(a.width, b.width)
    mh = max(a.height, b.height)
    if a.size != (mw, mh):
        a = a.resize((mw, mh), Image.NEAREST)
    if b.size != (mw, mh):
        b = b.resize((mw, mh), Image.NEAREST)
    arr_a = bitmap_to_array(a)
    arr_b = bitmap_to_array(b)
    inter = sum(1 for x, y in zip(arr_a, arr_b) if x and y)
    union = sum(1 for x, y in zip(arr_a, arr_b) if x or y)
    return inter / union if union else 0.0


def load_font(font_path: str, size: int) -> Optional[ImageFont.FreeTypeFont]:
    try:
        return ImageFont.truetype(font_path, size)
    except Exception as e:
        print(f"[WARN] 无法加载字体 {font_path} size={size}: {e}")
        return None


@dataclass
class TestCase:
    name: str
    screenshot: str
    region: Tuple[int, int, int, int]  # x1,y1,x2,y2 截图绝对坐标
    expected: str
    fonts: List[Tuple[str, int, int]]  # (font_path, min_size, max_size)


# 常见字体候选（按 FONT_LIST 推荐）
CLASH_ZH = r"D:\work\my\cocfz-apk-test\out\font_dump\fonts\ClashRoyaleZH-Regular.ttf"
DROID_FALLBACK = r"D:\work\my\cocfz-apk-test\out\font_dump\fonts\droid_sans_fallback.ttf"
SC_COC = r"D:\work\my\cocfz-apk-test\out\font_dump\fonts\SC_COC_text_appLight.ttf"

SUPERCELL = r"D:\work\my\cocfz-apk-test\out\font_dump\fonts\Supercell-Magic_5.ttf"

CASES = [
    # 主界面：玩家名（顶栏小字）
    TestCase("player_name", r"D:\work\my\cocfz-apk-test\游戏截图\导出村庄数据\1.png",
             (175, 36, 290, 65), "明心见性",
             [(CLASH_ZH, 12, 26), (DROID_FALLBACK, 12, 26), (SC_COC, 12, 26)]),
    # 主界面：左下按钮文字
    TestCase("attack_btn", r"D:\work\my\cocfz-apk-test\游戏截图\导出村庄数据\1.png",
             (35, 685, 120, 715), "进攻！",
             [(CLASH_ZH, 14, 28), (DROID_FALLBACK, 14, 28), (SC_COC, 14, 28), (SUPERCELL, 14, 28)]),
    # 事件面板：标题（大字，可能带描边）
    TestCase("event_title", r"D:\work\my\cocfz-apk-test\游戏截图\export_page_real.png",
             (700, 30, 950, 80), "报告首领！",
             [(CLASH_ZH, 18, 44), (DROID_FALLBACK, 18, 44), (SC_COC, 18, 44), (SUPERCELL, 18, 44)]),
    # 事件面板：按钮
    TestCase("ok_btn", r"D:\work\my\cocfz-apk-test\游戏截图\export_page_real.png",
             (1010, 255, 1130, 290), "这就办",
             [(CLASH_ZH, 14, 28), (DROID_FALLBACK, 14, 28), (SC_COC, 14, 28), (SUPERCELL, 14, 28)]),
]


def match_case(case: TestCase):
    img = Image.open(case.screenshot)
    x1, y1, x2, y2 = case.region
    crop = img.crop((x1, y1, x2, y2))
    # 默认用 threshold=200（和 harvest 一致），再试 160
    best_report = None
    for threshold in (200, 160, 140):
        binary = binarize(crop, threshold=threshold, invert=True)
        comps = find_components(binary, min_area=15, max_area=3000)
        # 过滤过窄的噪点
        comps = [c for c in comps if c.width >= 4 and c.height >= 6]
        if not comps:
            continue
        cells = merge_components_into_cells(comps)
        lines = group_into_lines(cells, y_tol=max(8, max(c.height for c in cells) // 4))
        report = match_lines(case, crop, binary, lines, threshold)
        if best_report is None or report["mean_score"] > best_report["mean_score"]:
            best_report = report
    if best_report is None:
        return None
    return best_report


def match_lines(case: TestCase, crop: Image.Image, binary: Image.Image,
                lines: List[List[Component]], threshold: int):
    # 逐个组件匹配预期字符中的每一个字体/尺寸
    all_chars = list(dict.fromkeys(case.expected))  # 去重但保留顺序
    candidate_glyphs: List[Tuple[str, str, int, Image.Image]] = []  # (char, font_name, size, img)
    for font_path, min_s, max_s in case.fonts:
        if not os.path.exists(font_path):
            continue
        for size in range(min_s, max_s + 1):
            font = load_font(font_path, size)
            if font is None:
                break
            for ch in all_chars:
                glyph = render_glyph(ch, font)
                if glyph.size != (1, 1):
                    candidate_glyphs.append((ch, os.path.basename(font_path), size, glyph))

    debug = crop.convert("RGB")
    draw = ImageDraw.Draw(debug)
    recognized = []
    total_score = 0.0
    count = 0

    for li, line in enumerate(lines):
        line_text = []
        for comp in line:
            comp_img = component_bitmap(comp, binary)
            best = ("?", 0.0)
            for ch, fname, size, glyph in candidate_glyphs:
                score = padded_agreement(comp_img, glyph)
                if score > best[1]:
                    best = (ch, score)
            ch, score = best
            line_text.append((comp, ch, score))
            recognized.append(ch)
            total_score += score
            count += 1
            color = (0, 255, 0) if score >= 0.7 else (255, 165, 0) if score >= 0.4 else (255, 0, 0)
            draw.rectangle((comp.left, comp.top, comp.right - 1, comp.bottom - 1), outline=color)
            draw.text((comp.left, max(0, comp.top - 10)), ch, fill=color)
        # 行文字
        ys = [c.cy for c in line]
        yline = sum(ys) // len(ys)
        draw.text((line[0].left, yline), "".join(x[1] for x in line_text), fill=(0, 0, 255))

    out_path = os.path.join(OUT_DIR, f"{case.name}_th{threshold}_debug.png")
    debug.save(out_path)

    mean_score = total_score / count if count else 0.0
    return {
        "name": case.name,
        "threshold": threshold,
        "components": count,
        "lines": len(lines),
        "recognized": "".join(recognized),
        "expected": case.expected,
        "mean_score": mean_score,
        "out": out_path,
    }


def main():
    print("=" * 60)
    print("中文文字识别 PC 端验证")
    print("=" * 60)
    for case in CASES:
        print(f"\n[测试] {case.name}: 期望='{case.expected}'")
        report = match_case(case)
        if not report:
            print("  -> 未找到连通域，可能区域/阈值不合适")
            continue
        print(f"  threshold={report['threshold']}, cells={report['components']}, lines={report['lines']}")
        print(f"  期望文字: '{report['expected']}'")
        print(f"  TTF模板匹配结果（受PC截图字体/缩放差异影响，仅供参考）: '{report['recognized']}'")
        print(f"  平均匹配得分: {report['mean_score']:.3f}")
        print(f"  可视化: {report['out']}")


if __name__ == "__main__":
    main()
