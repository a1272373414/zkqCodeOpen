import os, cv2, numpy as np
from PIL import Image
ICON_ROOT = r'D:\work\my\codebuddy_test\coc_manager_test01\src\main\resources\static\icon'
SF = r'I:\coc\游戏截图\主世界-配兵-兵种2.png'
CROP = 110

def qbin(px):
    # px: (N,3) uint8 RGB -> 512-bin histogram (8^3)
    q = (px[:, 0] // 32) * 64 + (px[:, 1] // 32) * 8 + (px[:, 2] // 32)
    h, _ = np.histogram(q, bins=512, range=(0, 512))
    s = h.sum()
    return h.astype(np.float64) / s if s else h.astype(np.float64)

# 1) icon histograms (mask = alpha>128)
icons = {}
for f in sorted(os.listdir(os.path.join(ICON_ROOT, '黑油兵'))):
    if not f.endswith('.png'):
        continue
    im = Image.open(os.path.join(ICON_ROOT, '黑油兵', f)).convert('RGBA')
    a = np.asarray(im)[:, :, 3]; rgb = np.asarray(im)[:, :, :3]
    mask = a > 128
    icons[f[:-4]] = qbin(rgb[mask])

# 2) slot candidates: 12 known + detected round blobs
known = [(411,629),(1065,629),(927,494),(1141,352),(1192,631),(671,632),
         (1065,495),(892,503),(798,489),(931,632),(538,629),(536,495)]
cands = known + [(909,323),(1190,334),(487,276),(215,280),(351,317),
                 (279,483),(1197,496),(686,492),(156,512),(277,630),(648,348),(581,254)]
cands = list(dict.fromkeys(cands))  # dedupe

img = np.asarray(Image.open(SF).convert('RGB'))[:, :, ::-1].copy()

def classify(lx, ly):
    sub = img[ly-CROP//2:ly+CROP//2, lx-CROP//2:lx+CROP//2]
    if sub.shape[0] != CROP or sub.shape[1] != CROP:
        return []
    h = qbin(sub.reshape(-1, 3))
    res = []
    for name, hi in icons.items():
        inter = float(np.minimum(h, hi).sum())
        res.append((name, inter))
    res.sort(key=lambda t: -t[1])
    return res

print('slot -> top3 icon matches by colour histogram:')
for (lx, ly) in cands:
    top = classify(lx, ly)
    tag = 'KNOWN' if (lx, ly) in known else 'cand '
    print('  (%4d,%4d) %s top: %s' % (lx, ly, tag,
          ', '.join('%s=%.2f' % (n, s) for n, s in top[:3])))
