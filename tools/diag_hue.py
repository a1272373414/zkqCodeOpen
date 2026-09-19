import cv2, numpy as np
from PIL import Image
SF = r'I:\coc\游戏截图\主世界-配兵-兵种2.png'
img = np.asarray(Image.open(SF).convert('RGB'))[:, :, ::-1].copy()
def report(lx, ly, r=40):
    sub = img[ly-r:ly+r, lx-r:lx+r]
    if sub.size == 0:
        print('   (%d,%d) empty' % (lx, ly)); return
    hsv = cv2.cvtColor(sub, cv2.COLOR_BGR2HSV)
    m = hsv.reshape(-1, 3)
    sat = m[:, 1]; val = m[:, 2]
    fg = m[(sat > 50) & (val > 50)]
    if len(fg) == 0:
        print('   (%4d,%4d) r=%d no warm px  meanBGR=%s' % (lx, ly, r, np.round(sub.reshape(-1,3).mean(0)).astype(int)))
        return
    hue = fg[:, 0]
    # fraction warm (hue 0-45 or 160-180 -> red/orange/yellow)
    warm = np.sum((hue < 45) | (hue > 160)) / len(fg)
    print('   (%4d,%4d) r=%d warmFrac=%.2f meanHue=%.0f nWarm=%d meanBGR=%s' % (
        lx, ly, r, warm, hue.mean(), len(fg), np.round(sub.reshape(-1,3).mean(0)).astype(int)))
print('top-row / candidate 烈焰熔炉 locations:')
for (x, y) in [(909,323),(1190,334),(1137,326),(648,348),(581,254),(970,126)]:
    report(x, y, 40)
# horizontal saturation profile at y~345 to find icon centres in top row
print('saturation profile y=345 (local peaks):')
hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
row = hsv[340:350, :, 1].max(axis=0).astype(float)
sm = np.zeros_like(row)
for i in range(len(row)):
    lo, hi = max(0, i-7), min(len(row), i+8)
    sm[i] = row[lo:hi].mean()
peaks = []
for x in range(3, len(sm)-3):
    if sm[x] > sm[x-3] and sm[x] > sm[x+3] and sm[x] > 40:
        if not peaks or x - peaks[-1] > 30:
            peaks.append(x)
for p in peaks:
    print('   peak x=%d sat=%.0f' % (p, sm[p]))
