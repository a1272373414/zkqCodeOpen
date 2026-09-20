# -*- coding: utf-8 -*-
import sys
from collections import Counter
from PIL import Image
import numpy as np

png = sys.argv[1]
img = np.asarray(Image.open(png).convert("RGB"))[:, :, ::-1].copy()  # BGR
h, w = img.shape[:2]
print("size", w, h)
mode = sys.argv[2] if len(sys.argv) > 2 else "size"
if mode == "color":
    x, y = int(sys.argv[3]), int(sys.argv[4])
    b, g, r = img[y, x]
    print("BGR %02X%02X%02X  RGB %02X%02X%02X" % (b, g, r, r, g, b))
elif mode == "top":
    n = int(sys.argv[3]) if len(sys.argv) > 3 else 12
    flat = [tuple(c) for c in img.reshape(-1, 3)]
    for col, cnt in Counter(flat).most_common(n):
        b, g, r = col
        print("%02X%02X%02X : %d" % (b, g, r, cnt))
elif mode == "find":
    # find color hex within region x1 y1 x2 y2 tolerance
    hexc = sys.argv[3]
    x1, y1, x2, y2 = int(sys.argv[4]), int(sys.argv[5]), int(sys.argv[6]), int(sys.argv[7])
    tol = int(sys.argv[8]) if len(sys.argv) > 8 else 24
    tb, tg, tr = int(hexc[4:6], 16), int(hexc[2:4], 16), int(hexc[0:2], 16)
    reg = img[y1:y2, x1:x2].astype(int)
    d = np.abs(reg[:, :, 0] - tb) + np.abs(reg[:, :, 1] - tg) + np.abs(reg[:, :, 2] - tr)
    ys, xs = np.where(d <= tol)
    print("hits", len(xs))
    if len(xs):
        # centroid in absolute coords + bbox
        cx = int(xs.mean()) + x1
        cy = int(ys.mean()) + y1
        print("centroid", cx, cy, "bbox",
              xs.min() + x1, ys.min() + y1, xs.max() + x1, ys.max() + y1)
else:
    print("modes: color / top / find")