# -*- coding: utf-8 -*-
"""
源脚本结构扫描器（awcocx 系列 lua）

用途：把 doc/decrypt 下的 lua 脚本按「命名块」切分成树，输出每个块的
      起止行 / 嵌套层级 / 名称 / 关键 API 计数 / 调用的全局函数 / 中文串样例 /
      纯数据表内部键名，用于生成「全量行号 ↔ 功能」对照文档。

行号口径（重要）：
      源文件每行以 "\r\r\n" 结尾，编辑器 / 通用换行解析会把一行拆成
      「代码行 + 空行」两行，行号翻倍。这里统一按 **单个 \n 切分**
      （awcocx_main.lua = 56062 行），与既有 doc/main_features.md 一致。
      换算：编辑器显示行号 = 2N - 1。

切分规则：识别任意缩进层级上的命名块
      _ENV["X"] = function / function X( / local function X( / local X = function
      / _ENV["X"] = { / local X = {（多行表）
      块结束 = 缩进 <= 块起始缩进的 end / } / until 行
"""

import re
import os
import sys

SRC_DIR = r"D:\work\my\cocfz-apk-test\doc\decrypt"
OUT_DIR = r"d:\work\my\zkqfz\zkqCodeOpen\tools\_lua_scan_out"

API_KEYS = [
    "findMultiColor", "findColor", "findMultiColorAll", "findImage",
    "httpGet", "httpPost", "httpDownload",
    "taps", "touchDown", "touchMove", "touchUp", "swipe",
    "toast", "showHUD",
    "exec", "writeFile", "readFile", "delfile", "fileExist", "copy_folder",
    "ui.newLayout", "TURING", "require",
    "tickCount", "mSleep", "sleep", "os.time", "os.date",
]

CN_RE = re.compile(r"[\u4e00-\u9fa5][\u4e00-\u9fa5A-Za-z0-9_（）()【】\[\]\-·/ ]{1,26}")
CALLEE_RE = re.compile(r'_ENV\["([^"]{1,24})"\]\s*\(')
KEY_RE = re.compile(r'^\s*\[?\s*"([^"]{1,30})"\s*\]?\s*=')
CLOSE_RE = re.compile(r"^(end\b|until\b|\}|\}|end\)|end,|end;)")

NAME_PATTERNS = [
    (re.compile(r'^\s*_ENV\["([^"]+)"\]\s*=\s*function'), 1),
    (re.compile(r"^\s*function\s+([\w\u4e00-\u9fa5_.:\[\]\"]+)"), 1),
    (re.compile(r"^\s*local\s+function\s+([\w\u4e00-\u9fa5_]+)"), 1),
    (re.compile(r"^\s*local\s+([\w\u4e00-\u9fa5_]+)\s*=\s*function"), 1),
    (re.compile(r'^\s*_ENV\["([^"]+)"\]\s*=\s*\{\s*$'), 1),
    (re.compile(r"^\s*local\s+([\w\u4e00-\u9fa5_]+)\s*=\s*\{\s*$"), 1),
]


def read_lines(path):
    with open(path, "rb") as f:
        raw = f.read().decode("utf-8", errors="replace")
    lines = raw.split("\n")
    while lines and lines[-1].strip("\r") == "":
        lines.pop()
    return [l.rstrip("\r").replace("\r", " ") for l in lines]


def match_name(line):
    for pat, gi in NAME_PATTERNS:
        m = pat.match(line)
        if m:
            return m.group(gi)
    return None


def scan(path):
    lines = read_lines(path)
    items = []
    stack = []
    for idx, line in enumerate(lines, start=1):
        stripped = line.strip()
        if stripped == "":
            continue
        indent = len(line) - len(line.lstrip())
        if CLOSE_RE.match(stripped):
            while stack and stack[-1]["indent"] >= indent:
                it = stack.pop()
                it["e"] = idx
                items.append(it)
            continue
        name = match_name(line)
        if name:
            while stack and stack[-1]["indent"] >= indent:
                it = stack.pop()
                it["e"] = idx - 1
                items.append(it)
            stack.append({"s": idx, "e": idx, "indent": indent,
                          "name": name, "line": stripped, "depth": len(stack)})
    while stack:
        it = stack.pop()
        it["e"] = len(lines)
        items.append(it)

    items.sort(key=lambda x: x["s"])
    for it in items:
        blob = "\n".join(lines[it["s"] - 1: it["e"]])
        it["size"] = it["e"] - it["s"] + 1
        it["api"] = {k: blob.count(k) for k in API_KEYS if blob.count(k) > 0}
        callees = []
        for c in CALLEE_RE.findall(blob):
            if c not in callees:
                callees.append(c)
        it["callee"] = callees[:10]
        cns = []
        for c in CN_RE.findall(blob):
            c = c.strip()
            if c and c not in cns:
                cns.append(c)
            if len(cns) >= 8:
                break
        it["cn"] = cns
        # 纯大表：抽取内部键名（便于描述特征库 / 字库等数据块）
        keys = []
        if it["size"] >= 60 and not any(k in it["api"] for k in ("taps", "findMultiColor", "httpGet", "exec")):
            for off, ln in enumerate(lines[it["s"] - 1: it["e"]]):
                m = KEY_RE.match(ln)
                if m and m.group(1) not in keys:
                    keys.append(m.group(1) + "@" + str(it["s"] + off))
                if len(keys) >= 14:
                    break
        it["keys"] = keys
    return lines, items


def coverage(lines, items):
    """返回未落在任何命名块内的行区间（顶层零散语句）"""
    mark = [False] * (len(lines) + 2)
    for it in items:
        for i in range(it["s"], it["e"] + 1):
            mark[i] = True
    gaps = []
    i = 1
    n = len(lines)
    while i <= n:
        if not mark[i]:
            j = i
            while j + 1 <= n and not mark[j + 1]:
                j += 1
            # 只统计非空行真正存在的区间
            if any(lines[k - 1].strip() for k in range(i, j + 1)):
                gaps.append((i, j))
            i = j + 1
        else:
            i += 1
    return gaps


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    targets = sys.argv[1:] or ["awcocx_main.lua", "awcocx_bootstrap.lua", "TURING.lua", "dkjson.lua", "hdtyynnn.lua", "layout.lua"]
    for name in targets:
        path = os.path.join(SRC_DIR, name)
        if not os.path.exists(path):
            print("missing:", path)
            continue
        lines, items = scan(path)
        out = os.path.join(OUT_DIR, name.replace(".lua", ".txt"))
        gaps = coverage(lines, items)
        with open(out, "w", encoding="utf-8") as f:
            f.write("### FILE %s 总行数=%d 命名块=%d 顶层零散区间=%d\n" % (name, len(lines), len(items), len(gaps)))
            f.write("### 顶层零散区间: " + "; ".join("L%d-%d" % g for g in gaps[:60]) + "\n")
            for it in items:
                api = ",".join(k + "×" + str(v) for k, v in it["api"].items())
                row = "L" + str(it["s"]) + "-" + str(it["e"]) + "|d" + str(it["depth"]) + "|" + it["name"] + "|" + api + "|" + " / ".join(it["cn"])
                if it["callee"]:
                    row += "|调:" + ",".join(it["callee"])
                if it["keys"]:
                    row += "|键:" + ",".join(it["keys"])
                f.write(row + "\n")
        print("%s: %d lines, %d blocks, 顶层零散 %d 段 -> %s" % (name, len(lines), len(items), len(gaps), out))


if __name__ == "__main__":
    main()
