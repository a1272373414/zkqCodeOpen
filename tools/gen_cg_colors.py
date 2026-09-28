# -*- coding: utf-8 -*-
"""把源 lua 的 findMultiColor 调用批量转成 Kotlin ColorSchema 定义。

源脚本坐标系：竖屏 720x1280（findMultiColor(x1,y1,x2,y2, 主色, 偏移串, dir, sim)）
本项目坐标系：横屏 1280x720，90° 映射 x' = y, y' = 719 - x
  - 区域 (x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1)
  - 偏移 (dx,dy)       -> (dy, -dx)
颜色串本身是 BGR，与项目 ColorSchema.parse 一致，无需转换。

用法: python gen_cg_colors.py 起始行 结束行 名称前缀
"""
import re
import sys

SRC = r"D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua"
CALL = re.compile(r"findMultiColor(?:All)?\(\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*\"([0-9A-Fa-f]+)(-[0-9A-Fa-f]+)?\"")
# 形参版（如 函数232a 里的 findMultiColor(x1,y1,x2,y2,...)）：用 --region 覆盖
CALL_VAR = re.compile(r"findMultiColor(?:All)?\(\s*[A-Za-z_][A-Za-z0-9_]*\s*,\s*[A-Za-z_][A-Za-z0-9_]*\s*,\s*[A-Za-z_][A-Za-z0-9_]*\s*,\s*[A-Za-z_][A-Za-z0-9_]*\s*,\s*\"([0-9A-Fa-f]+)(-[0-9A-Fa-f]+)?\"")
OFF = re.compile(r"(-?\d+)\|(-?\d+)\|([0-9A-Fa-f]+)(?:-[0-9A-Fa-f]+)?")
OFF_STR = re.compile(r"\"((?:-?\d+\|-?\d+\|[0-9A-Fa-f]+(?:-[0-9A-Fa-f]+)?\|?)+)\"")


def main():
    start, end, prefix = int(sys.argv[1]), int(sys.argv[2]), sys.argv[3]
    with open(SRC, "rb") as f:
        raw = f.read()
    lines = raw.replace(b"\r", b"").split(b"\n")
    out = []
    idx = 0
    for i in range(start, min(end, len(lines)) + 1):
        text = lines[i - 1].decode("utf-8", "ignore")
        m = CALL.search(text)
        already_mapped = False
        if m:
            x1, y1, x2, y2 = (int(m.group(k)) for k in (1, 2, 3, 4))
            main_color = m.group(5)
        else:
            mv = CALL_VAR.search(text)
            if not mv:
                continue
            override = [int(v) for v in sys.argv[4].split(",")]
            # 传入的是竖屏区域，这里直接换算成横屏区域
            x1, y1, x2, y2 = override[1], 719 - override[2], override[3], 719 - override[0]
            main_color = mv.group(1)
            m = mv
            already_mapped = True
        # 相似度：取调用末尾的 0.9 / 0.94 / 0.96 / 0.98
        sim_m = re.findall(r",\s*(0\.\d+)\s*\)", text)
        sim = sim_m[-1] if sim_m else "0.9"
        off_m = OFF_STR.search(text[m.end():])
        offsets = []
        if off_m:
            for om in OFF.finditer(off_m.group(1)):
                dx, dy, col = int(om.group(1)), int(om.group(2)), om.group(3)
                offsets.append("%d|%d|%s" % (dy, -dx, col))
        idx += 1
        name = "%s%02d" % (prefix, idx)
        nx1, ny1, nx2, ny2 = (x1, y1, x2, y2) if already_mapped else (y1, 719 - x2, y2, 719 - x1)
        line = '    val %s = ColorSchema.parse(\n        %d, %d, %d, %d, "%s",\n        "%s",\n        0, %s, "%s"\n    )' % (
            name, nx1, ny1, nx2, ny2, main_color, ",".join(offsets), sim, name
        )
        out.append("    // 源 L%d\n%s" % (i, line))
    sys.stdout.write("\n\n".join(out) + "\n")


if __name__ == "__main__":
    main()
