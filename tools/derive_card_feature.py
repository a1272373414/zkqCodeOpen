# -*- coding: utf-8 -*-
"""为单张卡片派生训练卡片特征（用于修复迁移后不匹配的少数兵种）。

思路：以卡片的图标像素为基础，选一个"与同页其它卡片差异最大"的锚点，再配若干同样有判别力的偏移点；
生成后用与运行期相同的匹配算法在整页选兵区域校验：必须命中自身卡片、且不命中任何其它卡片。

用法：
  python derive_card_feature.py --shot sw_spell_p1.png --x 875 --y 635 \
      --others "85,519;350,490;1137,508;741,642;743,505;90,630;213,637;875,510;220,510;351,638;478,508;467,649;610,639;1137,619;988,511;598,505;1002,641" \
      --key 骷髅
"""
import argparse
import cv2
import numpy as np

REGION = (23, 429, 1279, 693)   # 选兵面板区域（与迁移特征一致）
TOL = 25
MAX_OFFS = 12
R = 60


def card_crop(img, x, y, r=70):
    return img[max(0, y - r):y + r, max(0, x - r):x + r]


def colorful(px):
    return int(px.max()) - int(px.min()) > 50


def discriminate(crop, others, u, v):
    """锚点候选在相对位置 (u,v) 处：与其它卡片同位置的像素差异（取最小）。"""
    if u < 0 or v < 0 or v >= crop.shape[0] or u >= crop.shape[1]:
        return 0
    c = crop[v, u].astype(int)
    best = 1e9
    for o in others:
        if v >= o.shape[0] or u >= o.shape[1]:
            continue
        d = np.abs(c - o[v, u].astype(int)).sum()
        best = min(best, d)
    return 0 if best == 1e9 else best


def match_positions(img, main, offs, region=REGION, tol=TOL):
    x1, y1, x2, y2 = region
    sub = img[y1:y2, x1:x2].astype(np.int16)
    mask = (np.abs(sub - main.astype(np.int16)) <= tol).all(axis=2)
    ys, xs = np.where(mask)
    out = []
    for y, x in zip(ys, xs):
        ok = True
        for dx, dy, c in offs:
            tx, ty = x + dx, y + dy
            if tx < 0 or ty < 0 or ty >= sub.shape[0] or tx >= sub.shape[1]:
                ok = False
                break
            if (np.abs(sub[ty, tx] - c.astype(np.int16)) > tol).any():
                ok = False
                break
        if ok:
            out.append((x + x1, y + y1))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--shot', required=True)
    ap.add_argument('--dir', default='captures/training')
    ap.add_argument('--x', type=int, required=True)
    ap.add_argument('--y', type=int, required=True)
    ap.add_argument('--others', default='')
    ap.add_argument('--key', default='NEW')
    args = ap.parse_args()

    img = cv2.imread('%s/%s' % (args.dir, args.shot))
    crop = card_crop(img, args.x, args.y)
    h, w = crop.shape[:2]
    others = [card_crop(img, int(p.split(',')[0]), int(p.split(',')[1]))
              for p in args.others.split(';') if p.strip()]

    # 1) 选锚点：判别力最大且颜色不单调
    best = (-1, 0, 0)
    for v in range(20, h - 20, 2):
        for u in range(20, w - 20, 2):
            if not colorful(crop[v, u]):
                continue
            d = discriminate(crop, others, u, v)
            if d > best[0]:
                best = (d, u, v)
    _, ax, ay = best
    main = crop[ay, ax]
    print('anchor rel=(%d,%d) color=%s disc=%d' % (ax, ay, main, best[0]))

    # 2) 选偏移点：周边判别力大的位置，尽量分散
    cand = []
    for v in range(max(0, ay - R), min(h, ay + R + 1), 3):
        for u in range(max(0, ax - R), min(w, ax + R + 1), 3):
            if (u == ax and v == ay) or not colorful(crop[v, u]):
                continue
            d = discriminate(crop, others, u, v)
            if d < 120:
                continue
            cand.append((d, u, v))
    cand.sort(reverse=True)
    offs = []
    for d, u, v in cand:
        if abs(int(crop[v, u].max()) - int(main.max())) > 60:
            continue
        offs.append((u - ax, v - ay, crop[v, u]))
        if len(offs) >= MAX_OFFS:
            break
    print('offsets=%d' % len(offs))

    # 3) 校验：必须命中自身，且不命中同页其它卡片
    # 注意：命中的是**锚点**位置（= 卡片中心 + 锚点在卡片内的相对偏移），不是卡片中心
    exp_x = args.x - 70 + ax
    exp_y = args.y - 70 + ay
    hits = match_positions(img, main, offs)
    own = [p for p in hits if abs(p[0] - exp_x) <= 12 and abs(p[1] - exp_y) <= 12]
    bad = []
    for p in hits:
        for s in [q for q in args.others.split(';') if q.strip()]:
            ox, oy = (int(v) for v in s.split(','))
            if abs(p[0] - ox) <= 15 and abs(p[1] - oy) <= 15:
                bad.append('%d,%d' % (p[0], p[1]))
    print('hits=%d own=%d wrong=%d %s' % (len(hits), len(own), len(bad), bad[:5]))

    if own and not bad:
        # 与 game_state/App 一致的口径：按图像通道书写顺序（cv2 的 B,G,R）输出，不做 RGB 交换
        off_str = ','.join('%d|%d|%02X%02X%02X' % (dx, dy, int(c[0]), int(c[1]), int(c[2]))
                           for dx, dy, c in offs)
        print('OK -> paste:')
        print('        %d, %d, %d, %d, "%02X%02X%02X",' % (REGION[0], REGION[1], REGION[2], REGION[3],
                                                           int(main[0]), int(main[1]), int(main[2])))
        print('        "%s",' % off_str)
    else:
        print('FAILED: need manual anchor/offsets')


if __name__ == '__main__':
    main()
