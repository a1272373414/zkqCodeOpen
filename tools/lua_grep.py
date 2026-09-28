# -*- coding: utf-8 -*-
"""在源 lua 里按关键字查找行号（LF 计）。

用法: python lua_grep.py 关键字 [起始行] [结束行]
"""
import sys

SRC = r"D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua"


def main():
    kw = sys.argv[1]
    start = int(sys.argv[2]) if len(sys.argv) > 2 else 1
    end = int(sys.argv[3]) if len(sys.argv) > 3 else 10 ** 9
    with open(SRC, "rb") as f:
        raw = f.read()
    lines = raw.replace(b"\r", b"").split(b"\n")
    out = []
    for i in range(start, min(end, len(lines)) + 1):
        text = lines[i - 1].decode("utf-8", "ignore")
        if kw in text:
            out.append(str(i) + ": " + text[:200])
    sys.stdout.write("\n".join(out) + "\n")


if __name__ == "__main__":
    main()
