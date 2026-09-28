# -*- coding: utf-8 -*-
"""中文字库去噪工具（T34 遗留项：PC 端清洗）。

背景：App 端 `PixelFontChinese.harvestFromScreen()` 用 ML Kit 当"老师"自动标注，
会偶发把形近字标错（实测 模→摸、账→顶、并→井、免费→免轰）。这类错误是
**像素对、标签错** —— 单条记录看不出来，但字库里同一字往往有多个样本（不同界面、
不同尺寸各采一次），把它们两两比对就能揪出来。

做法（与 App 端 `PixelFontChinese.inspectGlyph` 同口径）：
  1. 按 (字, 尺寸±tol) 分组，组内聚类，取**最大簇**为该字的主字形；
  2. 与主簇相似度 < same_min 的条目判为"可疑"；
  3. 可疑条目再拿去全库比对，若与**另一个字**的主簇相似度 ≥ steal_min，
     判定为 ML Kit 误标 → 改判到那个字；否则删除；
  4. 打印改判/删除清单供人工过目，加 --apply 才写输出文件。

用法：
    python tools/clean_chinese_font.py chinese_font.txt
    python tools/clean_chinese_font.py chinese_font.txt --out clean.txt --apply
    python tools/clean_chinese_font.py chinese_font.txt --drop-ui   # 额外剔除疑似辅助 UI 文字

配合真机：
    adb pull /sdcard/zkqFiles/chinese_font.txt temp/chinese_font.txt
    python tools/clean_chinese_font.py temp/chinese_font.txt --out temp/clean.txt --apply
    adb push temp/clean.txt /sdcard/zkqFiles/chinese_font.txt

依赖：仅标准库（不需要 opencv）。
"""
import argparse
import os
import sys

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

# 必须与 jar 里的 PixelFontChinese.ALPHABET 一致
ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz&#"


def decode_bits(data, needed):
    """64 进制串 -> 位数组（取最后 needed 位，与 App 端 decodeBits 同口径）。"""
    raw = []
    for ch in data:
        value = ALPHABET.find(ch)
        if value < 0:
            continue
        for i in range(5, -1, -1):
            raw.append(1 if (value >> i) & 1 else 0)
    if len(raw) < needed:
        return None
    return raw[len(raw) - needed:]


def encode_bits(bits):
    """位数组 -> 64 进制串（不足 6 位在前面补 0）。"""
    total = len(bits)
    padding = (6 - total % 6) % 6
    out = []
    index = -padding
    while index < total:
        value = 0
        for b in range(6):
            pos = index + b
            bit = 1 if (0 <= pos < total and bits[pos]) else 0
            value = (value << 1) | bit
        out.append(ALPHABET[value])
        index += 6
    return "".join(out)


def agreement(a, b):
    """两字模对齐补白后的一致率（与 App 端 glyphAgreement / paddedAgreement 同口径）。

    a, b 均为 (bits, w, h)。
    """
    bits_a, wa, ha = a
    bits_b, wb, hb = b
    rows = max(ha, hb)
    cols = max(wa, wb)
    matched = 0
    for y in range(rows):
        for x in range(cols):
            bit_a = y < ha and x < wa and bits_a[y * wa + x]
            bit_b = y < hb and x < wb and bits_b[y * wb + x]
            if bit_a == bit_b:
                matched += 1
    return float(matched) / float(rows * cols)


def parse_library(path):
    """解析字库文件，返回 [(字, h, w, bits)]。"""
    entries = []
    with open(path, "r", encoding="utf-8-sig") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            parts = line.split("|")
            if len(parts) < 3:
                continue
            text = parts[0]
            if not text:
                continue
            dims = parts[1].split(",")
            if len(dims) != 2:
                continue
            try:
                height = int(dims[0])
                width = int(dims[1])
            except ValueError:
                continue
            if width <= 0 or height <= 0:
                continue
            bits = decode_bits("|".join(parts[2:]), width * height)
            if bits is None:
                continue
            entries.append((text, height, width, bits))
    return entries


def cluster_group(items, tol, same_min):
    """对一个字的所有样本聚类，返回 (主簇代表列表, 可疑成员下标集合)。

    注意：聚类**按尺寸分桶**——同一个字在 22x22 和 24x28 两种渲染下像素天然不同，
    跨尺寸比对会互相判为"不一致"而误杀。App 端匹配时同样只比对尺寸差 ≤tol 的条目，
    所以不同尺寸桶各自独立成簇是安全且符合识别口径的。

    items: [(index, h, w, bits)]，返回 (主簇代表 [(h, w, bits)], 可疑下标集合)
    """
    if not items:
        return [], set()

    clusters = []  # [(代表条目, 成员下标集合)]
    for item in items:
        index, h, w, bits = item
        placed = False
        for cluster in clusters:
            rep = cluster[0]
            if abs(rep[2] - w) > tol or abs(rep[1] - h) > tol:
                continue
            if agreement((rep[3], rep[2], rep[1]), (bits, w, h)) >= same_min:
                cluster[1].add(index)
                placed = True
                break
        if not placed:
            clusters.append((item, {index}))

    # 成员最多的簇 = 该字的"主字形"；其余小簇是错位/误标产物，判为可疑
    clusters.sort(key=lambda c: len(c[1]), reverse=True)
    representatives = [(clusters[0][0][1], clusters[0][0][2], clusters[0][0][3])]
    suspicious = set()
    for rep, members in clusters[1:]:
        suspicious |= members
    return representatives, suspicious


def main():
    parser = argparse.ArgumentParser(description="中文字库去噪（揪出 ML Kit 误标条目）")
    parser.add_argument("library", help="字库文件路径（如 chinese_font.txt）")
    parser.add_argument("--out", default="", help="清洗后输出文件（默认不写，仅打印报告）")
    parser.add_argument("--apply", action="store_true", help="实际写出 --out 文件")
    parser.add_argument("--tol", type=int, default=2, help="聚类尺寸容差（默认 2）")
    parser.add_argument("--same-min", type=float, default=0.80,
                        help="同字主簇判据阈值（默认 0.80，低于此判为可疑）")
    parser.add_argument("--steal-min", type=float, default=0.93,
                        help="跨字改判阈值（默认 0.93，高于此判为误标并改判）")
    parser.add_argument("--drop-unassigned", action="store_true",
                        help="连『无归属』的可疑条目也删除（默认只报告不删，避免误杀合法字模）")
    parser.add_argument("--drop-ui", action="store_true",
                        help="额外剔除疑似辅助 UI 文字（主页设置/保存并运行 等粗筛，默认关闭）")
    args = parser.parse_args()

    if not os.path.exists(args.library):
        raise SystemExit("字库文件不存在: " + args.library)

    entries = parse_library(args.library)
    print("读入 %d 条字模" % len(entries))
    if not entries:
        raise SystemExit("字库为空")

    # 1) 按 (字, 尺寸±tol) 分组
    groups = {}
    for index, (text, height, width, bits) in enumerate(entries):
        groups.setdefault(text, []).append((index, height, width, bits))

    main_of = {}       # 字 -> 主簇代表 (h, w, bits)
    suspicious = []    # 可疑条目下标
    for text, items in groups.items():
        reps, susp_idx = cluster_group(items, args.tol, args.same_min)
        if reps:
            main_of[text] = reps[0]
        suspicious.extend(sorted(susp_idx))

    print("按字分组：%d 个字；可疑条目 %d 条" % (len(groups), len(suspicious)))

    # 2) 可疑条目跨字比对：命中其它字主簇则改判，否则删除
    relabeled = []
    dropped = []
    for index in suspicious:
        text, h, w, bits = entries[index]
        best_text = None
        best_score = 0.0
        for other_text, (oh, ow, obits) in main_of.items():
            if other_text == text:
                continue
            if abs(ow - w) > args.tol or abs(oh - h) > args.tol:
                continue
            score = agreement((bits, w, h), (obits, ow, oh))
            if score > best_score:
                best_score = score
                best_text = other_text
        if best_text is not None and best_score >= args.steal_min:
            relabeled.append((index, text, best_text, best_score))
        else:
            dropped.append((index, text, best_text, best_score))

    print("\n改判（判定为 ML Kit 误标）：%d 条" % len(relabeled))
    for index, old, new, score in relabeled:
        print("    「%s」->「%s」 与「%s」主簇 %.3f" % (old, new, new, score))
    print("\n删除（与主簇形近但无归属）：%d 条" % len(dropped))
    for index, old, other, score in dropped:
        if other is None:
            print("    「%s」 无明显归属" % old)
        else:
            print("    「%s」 最像「%s」仅 %.3f，未达改判阈值" % (old, other, score))

    # 3) 重建字库：只落地"高置信改判"；无归属的可疑条目默认保留（宁可留脏，不可误杀）
    relabel_map = {index: new for index, _, new, _ in relabeled}
    drop_set = set()
    if args.drop_unassigned:
        drop_set = set(index for index, _, _, _ in dropped)
    kept = []
    for index, (text, h, w, bits) in enumerate(entries):
        if index in drop_set:
            continue
        final_text = relabel_map.get(index, text)
        if args.drop_ui and final_text in UI_NOISE_CHARS:
            continue
        kept.append((final_text, h, w, bits))

    # 去重（同字同尺寸只留一条，与 App 端 addGlyph 一致）
    seen = set()
    unique = []
    for text, h, w, bits in kept:
        key = "%s_%dx%d" % (text, w, h)
        if key in seen:
            continue
        seen.add(key)
        unique.append((text, h, w, bits))

    drop_word = "删除 %d" % len(dropped) if args.drop_unassigned else "可疑保留 %d" % len(dropped)
    print("\n清洗后：%d 条（原 %d 条，改判 %d、%s、去重 %d）" % (
        len(unique), len(entries), len(relabeled), drop_word, len(kept) - len(unique)))

    lines = ["%s|%d,%d|%s" % (text, h, w, encode_bits(bits)) for text, h, w, bits in unique]
    if args.out:
        if not args.apply:
            print("\n未加 --apply，不写文件。加 --apply 后写入: " + os.path.abspath(args.out))
        else:
            os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
            with open(args.out, "w", encoding="utf-8") as f:
                f.write("\n".join(lines) + "\n")
            print("\n已写入: " + os.path.abspath(args.out))
    else:
        print("\n（未指定 --out，仅打印报告）")


# --drop-ui 用：辅助面板入镜时采到的高频 UI 字（游戏界面基本不会出现）。
# 注意：这是"宁可错杀"的粗筛，默认关闭；如需更精确请直接整库重建（删库后让 bot 重采）。
UI_NOISE_CHARS = set(u"配置文件提取存档修改任意停止计时备主页设置账号退清除数据截取屏反馈问题进入游戏")


if __name__ == "__main__":
    main()
