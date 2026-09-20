# -*- coding: utf-8 -*-
"""Match a ColorSchema (from the project's .kt) against a CoC screenshot.

Usage:
  python tools/match_schema.py <name> <png>
      -> print "FOUND x y" or "NOT FOUND"

It reads the real ColorSchema.parse(...) definition from the .kt files so the
match is identical to what the app uses (threshold = 255*(1-similarity)).
"""
import os
import re
import sys
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
KT_ROOT = os.path.join(ROOT, "..", "app", "src", "main",
                       "java", "com", "coc", "zkqcode", "jar", "code",
                       "colorschema")


def bgr_hex_to_rgb(h):
    h = h.split("-")[0]
    v = int(h, 16)
    b = (v >> 16) & 0xFF
    g = (v >> 8) & 0xFF
    r = v & 0xFF
    return (r, g, b)


def load_schemas():
    schemas = {}
    for dp, _, files in os.walk(KT_ROOT):
        for f in files:
            if not f.endswith(".kt"):
                continue
            parse_file(os.path.join(dp, f), schemas)
    return schemas


def parse_file(path, schemas):
    text = open(path, encoding="utf-8").read()
    for m in re.finditer(
            r"override\s+val\s+(\w+)\s*=\s*ColorSchema\.parse\(", text):
        name = m.group(1)
        start = m.end()
        depth = 1
        i = start
        while i < len(text) and depth > 0:
            c = text[i]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
            i += 1
        block = text[start:i - 1]
        block = re.sub(r"//[^\n]*", "", block)
        args = split_args(block)
        if len(args) >= 8:
            schemas[name] = {
                "x1": int(args[0]), "y1": int(args[1]),
                "x2": int(args[2]), "y2": int(args[3]),
                "main": unq(args[4]),
                "offsets": unq(args[5]),
                "dir": int(args[6]), "sim": float(args[7]),
            }


def unq(s):
    s = s.strip()
    if s.startswith('"') and s.endswith('"'):
        s = s[1:-1]
    return s


def split_args(block):
    args = []
    cur = ""
    in_q = False
    for c in block:
        if c == '"':
            in_q = not in_q
            cur += c
        elif c == "," and not in_q:
            args.append(cur.strip())
            cur = ""
        else:
            cur += c
    if cur.strip():
        args.append(cur.strip())
    return args


def match(name, png):
    schemas = load_schemas()
    if name not in schemas:
        print("UNKNOWN SCHEMA", name)
        return None
    s = schemas[name]
    img = np.asarray(Image.open(png).convert("RGB")).astype(int)
    h, w = img.shape[:2]
    thr = int(round(255 * (1 - s["sim"])))
    x1, y1, x2, y2 = s["x1"], s["y1"], s["x2"], s["y2"]
    x1, x2 = max(0, x1), min(w - 1, x2)
    y1, y2 = max(0, y1), min(h - 1, y2)
    mr, mg, mb = bgr_hex_to_rgb(s["main"])
    offs = []
    for seg in s["offsets"].split(","):
        if not seg.strip():
            continue
        p = seg.split("|")
        dx, dy = int(p[0]), int(p[1])
        cr, cg, cb = bgr_hex_to_rgb(p[2])
        offs.append((dx, dy, cr, cg, cb))
    reg = img[y1:y2 + 1, x1:x2 + 1]
    mr_a = reg[:, :, 0]
    mg_a = reg[:, :, 1]
    mb_a = reg[:, :, 2]
    main_ok = (np.abs(mr_a - mr) <= thr) & (np.abs(mg_a - mg) <= thr) & (np.abs(mb_a - mb) <= thr)
    ys, xs = np.where(main_ok)
    hits = []
    for j, i in zip(ys, xs):
        ay, ax = y1 + j, x1 + i
        ok = True
        for dx, dy, cr, cg, cb in offs:
            py, px = ay + dy, ax + dx
            if px < 0 or px >= w or py < 0 or py >= h:
                ok = False
                break
            pr, pg, pb = img[py, px]
            if abs(pr - cr) > thr or abs(pg - cg) > thr or abs(pb - cb) > thr:
                ok = False
                break
        if ok:
            hits.append((ax, ay))
            if len(hits) >= 50:
                break
    if hits:
        cx = int(sum(p[0] for p in hits) / len(hits))
        cy = int(sum(p[1] for p in hits) / len(hits))
        print("FOUND %d %d  (hits=%d)" % (cx, cy, len(hits)))
        return (cx, cy)
    print("NOT FOUND")
    return None


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("usage: match_schema.py <name> <png>")
    else:
        match(sys.argv[1], sys.argv[2])

