import sys
from PIL import Image
import numpy as np
from collections import Counter

path = sys.argv[1] if len(sys.argv) > 1 else "tools/_tmp_cap.png"
img = np.ascontiguousarray(np.asarray(Image.open(path).convert("RGB"))[:, :, ::-1])
h, w = img.shape[:2]
print("shape", img.shape)

cols = 40
# Scan the top region where category tabs / page title may sit.
for y in (40, 60, 80, 100, 120, 140, 160, 180):
    line = "y%-4d " % y
    for rx in range(cols):
        x0, x1 = int(rx * w / cols), int((rx + 1) * w / cols)
        block = img[y:y + 1, x0:x1].reshape(-1, 3)
        c = Counter(map(tuple, block)).most_common(1)[0][0]
        line += "%02X%02X%02X " % (c[0], c[1], c[2])
    print(line)
