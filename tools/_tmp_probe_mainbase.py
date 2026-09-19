import numpy as np
from PIL import Image
from scipy import ndimage
B='I:/coc/游戏截图/补充1'
f=sorted(x for x in __import__('os').listdir(B) if '兵种配置' in x)[0]
img=np.asarray(Image.open(B+'/'+f).convert('RGB')).astype(np.int32)
