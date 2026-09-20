import sys
from PIL import Image
import numpy as np
from collections import Counter

path = sys.argv[1] if len(sys.argv) > 1 else "tools/_tmp_cap.png"
img = np.ascontiguousarray(np.asarray(Image.open(path).convert("RGB"))[:, :, ::-1])
h, w = img.shape[:2]
print("shape", img.shape)

# Coarse 16x9 grid of dominant (B,G,R) colors, each cell sampled at center 20x20 block.
cols, rows = 16, 9
for ry in range(rows):
    line = ""
    for rx in range(cols):
        y0, y1 = int(ry * h / rows), int((ry + 1) * h / rows)
        x0, x1 = int(rx * w / cols), int((rx + 1) * w / cols)
        block = img[y0:y1, x0:x1].reshape(-1, 3)
        c = Counter(map(tuple, block)).most_common(1)[0][0]
        line += "%02X%02X%02X " % (c[0], c[1], c[2])
    print(line)
