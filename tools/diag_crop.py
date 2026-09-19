import cv2, numpy as np
from PIL import Image
SF = r'I:\coc\游戏截图\主世界-配兵-兵种2.png'
img = np.asarray(Image.open(SF).convert('RGB'))[:, :, ::-1].copy()
hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
for (lx, ly) in [(909,323),(1190,334),(1141,352)]:
    sub = img[ly-48:ly+48, lx-48:lx+48]
    hsub = hsv[ly-48:ly+48, lx-48:lx+48]
    fire = ((hsub[:,:,0]<50)|(hsub[:,:,0]>160)) & (hsub[:,:,1]>50) & (hsub[:,:,2]>90)
    ys, xs = np.where(fire)
    if len(xs):
        print('(%d,%d) fire-bright px=%d centroid=(%d,%d) bbox w=%d h=%d' % (
            lx, ly, len(xs), xs.mean()+lx-48, ys.mean()+ly-48,
            xs.max()-xs.min()+1, ys.max()-ys.min()+1))
    else:
        print('(%d,%d) NO fire-bright px' % (lx, ly))
    print('   crop meanBGR=%s std=%.1f' % (np.round(sub.reshape(-1,3).mean(0)).astype(int), sub.std()))
