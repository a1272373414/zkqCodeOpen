import cv2, numpy as np
from PIL import Image
SF = r'I:\coc\游戏截图\主世界-配兵-兵种2.png'
img = np.asarray(Image.open(SF).convert('RGB'))[:, :, ::-1].copy()
hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
# fire: warm hue, some saturation; allow darker fire (val>25)
fire = ((hsv[:, :, 0] < 50) | (hsv[:, :, 0] > 160)) & (hsv[:, :, 1] > 50) & (hsv[:, :, 2] > 25)
fire = fire.astype(np.uint8) * 255
num, lab, stats, cents = cv2.connectedComponentsWithStats(fire, 8)
known = [(411,629),(1065,629),(927,494),(1141,352),(1192,631),(671,632),
         (1065,495),(892,503),(798,489),(931,632),(538,629),(536,495)]
print('fire-coloured round-ish icon blobs (area 1200..9000, aspect 0.6..1.7):')
for i in range(1, num):
    a = stats[i, cv2.CC_STAT_AREA]
    cx = stats[i, cv2.CC_STAT_LEFT] + stats[i, cv2.CC_STAT_WIDTH] // 2
    cy = stats[i, cv2.CC_STAT_TOP] + stats[i, cv2.CC_STAT_HEIGHT] // 2
    w = stats[i, cv2.CC_STAT_WIDTH]; h = stats[i, cv2.CC_STAT_HEIGHT]
    if a < 1200 or a > 9000:
        continue
    if w / max(1, h) < 0.6 or w / max(1, h) > 1.7:
        continue
    if any(abs(cx - kx) < 70 and abs(cy - ky) < 70 for kx, ky in known):
        continue
    print('   cx={} cy={} area={} {}x{} aspect={:.2f}'.format(cx, cy, a, w, h, w / max(1, h)))
