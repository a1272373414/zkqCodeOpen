# -*- coding: utf-8 -*-
"""按 LF 行号打印源脚本片段。

用法: python dump_lua.py 起始行 结束行
说明: awcocx_main.lua 每行以 \\r\\r\\n 结尾，按 \\n 计数才是文档里的行号。
"""
import sys

SRC = r"D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua"


def main():
    start = int(sys.argv[1])
    end = int(sys.argv[2])
    with open(SRC, "rb") as f:
        raw = f.read()
    # 按 \n 切分，兼容 \r\r\n 结尾
    lines = raw.replace(b"\r", b"").split(b"\n")
    out = []
    for i in range(start, min(end, len(lines)) + 1):
        out.append(str(i) + ": " + lines[i - 1].decode("utf-8", "ignore"))
    sys.stdout.write("\n".join(out) + "\n")


if __name__ == "__main__":
    main()
