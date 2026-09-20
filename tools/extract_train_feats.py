import re, sys, json

LUA = r'D:\work\my\cocfz-apk-test\doc\decrypt\awcocx_main.lua'
t = open(LUA, encoding='utf-8', errors='ignore').read()
pat = re.compile(r'多点找色界面 == "(造[^"]+)"(.{0,400}?)findMultiColor\(([^)]*)\)', re.S)
found = []
for m in pat.finditer(t):
    name = m.group(1)
    args = m.group(3)
    # region: 4 ints then main color then offsets then 0, 0.9
    am = re.match(r'\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*"([0-9A-Fa-f]+)[^"]*"\s*,\s*"([^"]*)"', args)
    if not am:
        found.append((name, None, args[:80]))
        continue
    found.append((name, tuple(int(am.group(i)) for i in range(1, 5)), am.group(5), am.group(6)))

print('total 造X branches:', len(found))
ok = [f for f in found if f[1]]
print('parsed:', len(ok))
for name, reg, main in [(f[0], f[1], f[2]) for f in ok][:200]:
    print('%-14s %s %s' % (name, reg, main))
print('--- unparsed ---')
for name, _, raw in [f for f in found if not f[1]][:20]:
    print(name, raw)
