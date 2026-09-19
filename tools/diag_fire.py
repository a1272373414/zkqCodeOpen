import cv2, numpy as np
from PIL import Image
SF = r'I:\coc\游戏截图\主世界-配兵-兵种2.png'
img = np.asarray(Image.open(SF).convert('RGB'))[:, :, ::-1].copy()
hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
# warm fire mask: hue 0-45 (red/orange/yellow) and decent sat/val
fire = ((hsv[:, :, 0] < 45) | (hsv[:, :, 0] > 160)) & (hsv[:, :, 1] > 40) & (hsv[:, :, 2] > 30)
fire = fire.astype(np.uint8) * 255
# restrict to top area y 280..400
fire[400:, :] = 0
num, lab, stats, cents = cv2.connectedComponentsWithStats(fire, 8)
print('warm blobs in top area (y<400):')
for i in range(1, num):
    a = stats[i, cv2.CC_STAT_AREA]
    if a < 80:
        continue
    cx = stats[i, cv2.CC_STAT_LEFT] + stats[i, cv2.CC_STAT_WIDTH] // 2
    cy = stats[i, cv2.CC_STAT_TOP] + stats[i, cv2.CC_STAT_HEIGHT] // 2
    w = stats[i, cv2.CC_STAT_WIDTH]; h = stats[i, cv2.CC_STAT_HEIGHT]
    print('   cx=%4d cy=%4d area=%4d %3dx%3d aspect=%.2f' % (cx, cy, a, w, h, w/max(1,h)))
