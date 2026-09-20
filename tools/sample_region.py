# -*- coding: utf-8 -*-
"""Sample the dominant colors inside given regions of a screenshot."""
import sys
from collections import Counter

import numpy as np
from PIL import Image

path = sys.argv[1]
img = np.ascontiguousarray(np.asarray(Image.open(path).convert("RGB"))[:, :, ::-1])

# Regions of interest: (label, x1, y1, x2, y2)
regions = [
    ("DeleteAll3-region", 1219, 324, 1252, 366),
    ("candidate-bottom-trash", 950, 505, 1000, 550),
    ("candidate-right-trash", 1210, 310, 1260, 380),
    ("DeleteAll1-reference", 1219, 152, 1252, 185),
]
for label, x1, y1, x2, y2 in regions:
    block = img[y1:y2 + 1, x1:x2 + 1].reshape(-1, 3)
    top = Counter(map(tuple, block)).most_common(6)
    print(label, " ".join("%02X%02X%02Xx%d" % (c[0], c[1], c[2], n) for c, n in top))
