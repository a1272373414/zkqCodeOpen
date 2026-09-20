# -*- coding: utf-8 -*-
"""把原脚本某段区间导出为 txt（便于用编辑器/读取工具正常查看）。用法：python echo_lua_region.py 224600 226300"""
import sys

LUA = r'D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua'
a, b = int(sys.argv[1]), int(sys.argv[2])
t = open(LUA, encoding='utf-8', errors='ignore').read()
out = '_lua_region_%d_%d.txt' % (a, b)
with open(out, 'w', encoding='utf-8') as f:
    f.write(t[a:b])
print('wrote', out, 'len', b - a)
