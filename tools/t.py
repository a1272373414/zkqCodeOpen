import numpy as np, os
from PIL import Image
from scipy import ndimage
B='I:/coc/游戏截图/补充1'
f=sorted(x for x in os.listdir(B) if '兵种配置' in x)[0]
img=np.asarray(Image.open(B+'/'+f).convert('RGB')).astype(np.int32)
sat=img.max(2)-img.min(2)
lbl,n=ndimage.label(sat>55)
print(img.shape, n)
for i in range(1,n+1):
    ys,xs=np.where(lbl==i)
    h=ys.max()-ys.min()+1
    w=xs.max()-xs.min()+1
    print(i,w,h,ys.min())
