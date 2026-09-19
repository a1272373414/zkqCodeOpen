import os, cv2, numpy as np
from PIL import Image
ICON_ROOT = r'D:\work\my\codebuddy_test\coc_manager_test01\src\main\resources\static\icon'
SF = r'I:\coc\游戏截图\主世界-配兵-兵种2.png'
T = 100
def load_icon(name, color):
    im = Image.open(os.path.join(ICON_ROOT, color, name + '.png')).convert('RGBA')
    a = np.asarray(im)[:, :, 3]; rgb = np.asarray(im)[:, :, :3]
    ys, xs = np.where(a > 128)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    art = rgb[y0:y1 + 1, x0:x1 + 1]; am = a[y0:y1 + 1, x0:x1 + 1]
    ah, aw = art.shape[:2]; side = max(ah, aw)
    cv = np.zeros((side, side, 3), np.uint8); cm = np.zeros((side, side), np.uint8)
    cv[(side-ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = art
    cm[(side-ah)//2:(side-ah)//2+ah, (side-aw)//2:(side-aw)//2+aw] = am
    g = cv2.cvtColor(cv2.resize(cv, (T, T), cv2.INTER_AREA), cv2.COLOR_RGB2GRAY)
    m = cv2.resize(cm, (T, T), cv2.INTER_NEAREST); _, m = cv2.threshold(m, 128, 255, cv2.THRESH_BINARY)
    return g, m
img = np.asarray(Image.open(SF).convert('RGB'))[:, :, ::-1].copy()
gimg = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
for name in ['烈焰熔炉', '熔岩猎犬', '瓦基丽武神', '女巫', '英雄猎手']:
    g, m = load_icon(name, '黑油兵')
    print('=== %s (sc=1.0) ===' % name)
    for (lx, ly) in [(800,630),(790,628),(810,632),(785,630),(815,630)]:
        sub = gimg[ly-50:ly+50, lx-50:lx+50]
        if sub.shape[0]!=100 or sub.shape[1]!=100:
            continue
        res = cv2.matchTemplate(sub, g, cv2.TM_CCOEFF_NORMED, mask=m)
        print('   (%d,%d) ncc=%.3f' % (lx, ly, float(res[0,0])))
    # also whole-image best for this troop
    res = cv2.matchTemplate(gimg, g, cv2.TM_CCOEFF_NORMED, mask=m)
    _, mx, _, loc = cv2.minMaxLoc(res)
    print('   whole-image best ncc=%.3f at (%d,%d)' % (mx, loc[0]+50, loc[1]+50))
