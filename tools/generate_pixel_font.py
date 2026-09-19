# -*- coding: utf-8 -*-
"""
把原脚本的图灵六十四进制点阵数字字库
（cocfz-apk-test/doc/decrypt/awcocx_main.lua 的 识别库，已由源项目导出为
 cocfz-apk-test/code/coc-assist/app/src/main/assets/lua/font_digits.lua）
内嵌成 Kotlin 常量，供 zkqCodeOpen 的 PixelFontOcr 使用。

字库条目格式（注意：不是文件头注释里写的"宽,高"，实测是"高,宽"）：

    字符|高,宽|64进制点阵串

- 64 进制字符表：0-9 A-Z a-z &#（每字符 6 bit）
- 点阵串按行优先、每字节高位在前展开成 高*宽 个 bit
- 原版 TURING 只取最后 高*宽 个 bit（前面多余的 bit 是填充）
- bit=1 表示笔画（前景）

用法：
    python tools/generate_pixel_font.py
"""

import io
import os
import re

SRC = r'D:\work\my\cocfz-apk-test\code\coc-assist\app\src\main\assets\lua\font_digits.lua'
OUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'java',
    'com', 'coc', 'zkqcode', 'jar', 'code', 'universal', 'recognizer',
    'PixelFontDigits.kt')

KEEP = set('0123456789./')

with io.open(SRC, 'r', encoding='utf-8') as f:
    text = f.read()

entries = []
for m in re.finditer(r'"([^"]+)"', text):
    e = m.group(1)
    parts = e.split('|')
    if len(parts) < 3:
        continue
    ch, wh, data = parts[0], parts[1], '|'.join(parts[2:])
    if ch not in KEEP:
        continue
    try:
        a, b = [int(v) for v in wh.split(',')]
    except ValueError:
        continue  # the file header comment line
    entries.append(e)

print('font entries kept:', len(entries))
for ch in entries:
    assert '"' not in ch and '\\' not in ch and '$' not in ch, ch

body = '\n'.join(entries)
assert '"""' not in body

content = u'''package com.coc.zkqcode.jar.code.universal.recognizer

/**
 * Pixel font library for digits, extracted from the legacy freescript 图灵 OCR library.
 *
 * Entry format (note: the legacy header comment says "宽,高" but the data is "高,宽"):
 *
 *     字符|高,宽|64进制点阵串
 *
 * - 64-base alphabet: 0-9 A-Z a-z &# (6 bits per char)
 * - The bitmap is stored row-major, most significant bit first, 高*宽 bits in total
 * - The legacy TURING loader only keeps the LAST 高*宽 bits (leading bits are padding)
 * - bit = 1 means a foreground (ink) pixel
 *
 * Contains the characters: 0-9, '.' and '/' (1891 variants), used by [PixelFontOcr] as an
 * offline fallback when ML Kit text recognition fails.
 */
internal const val PIXEL_FONT_DIGITS: String = """
%s
"""
''' % body

out = os.path.abspath(OUT)
with io.open(out, 'w', encoding='utf-8') as f:
    f.write(content)
print('written:', out, os.path.getsize(out), 'bytes')
