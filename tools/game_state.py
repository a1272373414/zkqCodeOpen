# -*- coding: utf-8 -*-
"""Shared ground-truth helpers: page detection + safe recovery to training page.

Page detection mirrors SceneState.kt detectCurrentScene():
  MAIN_VILLAGE  = MyColors.TrainTroops hit            (FeatureColors.kt)
  TRAINING_PAGE = TrainingPage / AttackInTrainingPage* (MainBaseTraining.kt)
  POPUP         = RedX (UIColors.kt) / CommonDialog (FeatureColors.kt)
"""
import os
import re
import subprocess
import time

import sys

# Windows 控制台默认 GBK，中文 print 会乱码 —— 统一改成 UTF-8（所有引用本模块的工具都受益）
for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding='utf-8')
    except Exception:
        pass

import cv2
import numpy as np

ADB = r"C:\Users\TANG\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "emulator-5556"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "tools", "captures", "training")
os.makedirs(OUT, exist_ok=True)
TH = 25
PKG = os.path.join(ROOT, r"app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage")
F_FEATURE = os.path.join(PKG, "FeatureColors.kt")
F_UI = os.path.join(PKG, "UIColors.kt")
F_TRAINING = os.path.join(PKG, r"mainbase\MainBaseTraining.kt")
F_TRAIN_BTN = os.path.join(PKG, r"mainbase\MainBaseTrainCardColors.kt")
F_BLACK = os.path.join(PKG, r"mainbase\MainBaseBlackElixirTroopColors.kt")
F_SUPER = os.path.join(PKG, r"mainbase\MainBaseSuperTroopColors.kt")


def adb(a):
    subprocess.run([ADB, "-s", SERIAL] + a, check=True)


def tap(x, y, dt=0.9):
    adb(["shell", "input", "tap", str(x), str(y)])
    time.sleep(dt)


def keyevent(code, dt=1.0):
    adb(["shell", "input", "keyevent", str(code)])
    time.sleep(dt)


def swipe(x1, y1, x2, y2, dur=400, dt=1.0):
    adb(["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(dur)])
    time.sleep(dt)


def cap(name):
    p = os.path.join(OUT, name)
    with open(p, "wb") as f:
        subprocess.run([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"], stdout=f, check=True)
    time.sleep(0.4)
    return p


def parse_file(path, names=None):
    txt = open(path, encoding="utf-8").read()
    pat = re.compile(
        r'(\w+)\s*=\s*ColorSchema\.parse\(\s*'
        r'(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'
        r'"([0-9A-Fa-f]{6})",\s*"([^"]*)"', re.DOTALL)
    out = []
    for m in pat.finditer(txt):
        name = m.group(1)
        if names is not None and name not in names:
            continue
        rx1, ry1, rx2, ry2 = (int(m.group(i)) for i in (2, 3, 4, 5))
        anchor = hex_arr(m.group(6))
        offs = []
        for tok in m.group(7).split(','):
            if not tok:
                continue
            dx, dy, h = tok.split('|')
            offs.append((int(dx), int(dy), hex_arr(h)))
        out.append((name, (rx1, ry1, rx2, ry2), anchor, offs))
    return out


def hex_arr(h):
    """
    把 ColorSchema 的 6 位十六进制按**书写顺序**逐字节解析，并与 cv2 读入的通道顺序（B,G,R）比较。
    这是与 App 一致的实测口径：App 里这些特征（含生产在用的 FeatureColors/MainBaseTraining）
    在同样的"逐字节比较"下稳定命中（例如 `TrainBarbarian` 用于 `panel_open` 判面板开合）。
    """
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], dtype=np.int16)


def hex_of(px):
    """图像像素（cv2 的 B,G,R 顺序）-> 与上面同口径的十六进制字符串。"""
    return '%02X%02X%02X' % (int(px[0]), int(px[1]), int(px[2]))


def collect(scan, anchor, offs):
    if scan is None or scan.size == 0:
        return None
    mask = (np.abs(scan.astype(np.int16) - anchor) <= TH).all(axis=2)
    ys, xs = np.where(mask)
    for y, x in zip(ys, xs):
        ok = True
        for dx, dy, c in offs:
            tx, ty = x + dx, y + dy
            if tx < 0 or tx >= scan.shape[1] or ty < 0 or ty >= scan.shape[0]:
                ok = False
                break
            if (np.abs(scan[ty, tx].astype(np.int16) - c) > TH).any():
                ok = False
                break
        if ok:
            return (int(x), int(y))
    return None


def find_first(img, feats):
    """First feature hit -> (name, x, y) using region crop + offset conversion."""
    for (name, (rx1, ry1, rx2, ry2), a, o) in feats:
        hit = collect(img[ry1:ry2, rx1:rx2], a, o)
        if hit:
            return name, hit[0] + rx1, hit[1] + ry1
    return None


_feat_train_troops = parse_file(F_FEATURE, {"TrainTroops"})
_feat_dialog = parse_file(F_FEATURE, {"CommonDialog"})
_feat_redx = parse_file(F_UI, {"RedX"})
_feat_reload = parse_file(F_UI, {"ReloadGameButton"})
_feat_training = parse_file(F_TRAINING, {"TrainingPage", "AttackInTrainingPage",
                                         "AttackInTrainingPage2", "AttackInTrainingPage3"})
_feat_trainbarb = parse_file(F_TRAINING, {"TrainBarbarian"})


def page_of(img):
    """Returns one of: main_village / training / popup / unknown."""
    if find_first(img, _feat_train_troops):
        return "main_village"
    if find_first(img, _feat_training):
        return "training"
    if find_first(img, _feat_redx) or find_first(img, _feat_dialog):
        return "popup"
    return "unknown"


def panel_open(img):
    """True when the 圣水兵 selection panel is open (TrainBarbarian marker visible)."""
    return find_first(img, _feat_trainbarb) is not None


def recover_to_training(max_rounds=8):
    """Get back to the training page from any state, self-verifying each step."""
    for _ in range(max_rounds):
        img = cv2.imread(cap("state.png"))
        page = page_of(img)
        if page == "training":
            return True
        if page == "main_village":
            tap(52, 521, dt=2.0)  # 训练部队 button
            img = cv2.imread(cap("state.png"))
            if page_of(img) == "training":
                return True
            continue
        if page == "popup":
            hit = find_first(img, _feat_redx) or find_first(img, _feat_dialog)
            if hit and hit[0] == "RedX":
                tap(hit[1], hit[2], dt=1.2)
            elif hit:  # CommonDialog: close at dialog top-right, same as sweepBlockingPopups
                tap(hit[1] + 960, hit[2] + 30, dt=1.2)
            continue
        keyevent(4, dt=1.2)  # BACK for unknown screens
    return False


def count_hits(img, feats):
    return sum(1 for (_, (rx1, ry1, rx2, ry2), a, o) in feats
               if collect(img[ry1:ry2, rx1:rx2], a, o) is not None)


# --- 掉线弹窗处理（"还在吗？因为太久没有进行操作，您已断开连接。"+ 重新载入游戏） ---
RELOAD_BTN = (385, 462)   # "重新载入游戏" 文字中心（1280x720 实测）


def disconnect_dialog(img):
    """掉线弹窗特征：弹窗内部是几块大面积均匀灰底（无图标/无渐变）。"""
    for (x, y) in ((474, 296), (829, 237), (600, 500)):
        p = img[y - 6:y + 6, x - 6:x + 6].astype(int)
        if p.size == 0 or p.std() > 12:
            return False
        m = p.reshape(-1, 3).mean(axis=0)
        if m.max() - m.min() > 25:
            return False
    return True


def ensure_online(dt=18):
    """若出现掉线弹窗则点击"重新载入游戏"并等待重连。返回是否处理过。

    优先用 App 里的 `ReloadGameButton` 特征精确定位按钮文字（与 Kotlin 侧同一份特征），
    特征没命中时再回退到"灰底判定 + 固定坐标"。
    """
    img = cv2.imread(cap('online.png'))
    h = find_first(img, _feat_reload)
    if h:
        print('检测到掉线弹窗（「重新载入游戏」@%d,%d）→ 点击并等待重连' % (h[1], h[2]))
        tap(h[1] + 14, h[2] + 16, dt=dt)
        return True
    if not disconnect_dialog(img):
        return False
    print('检测到掉线弹窗（灰底判定）→ 点击固定坐标"重新载入游戏"')
    tap(RELOAD_BTN[0], RELOAD_BTN[1], dt=dt)
    return True


def close_dialogs(max_rounds=3):
    """关掉挡住界面的弹窗（RedX / 通用对话框，例如误点卡片"i"打开的兵种详情）。"""
    for _ in range(max_rounds):
        img = cv2.imread(cap('dlg.png'))
        h = find_first(img, _feat_redx)
        if h:
            print('  关闭弹窗 RedX@(%d,%d)' % (h[1], h[2]))
            tap(h[1], h[2], dt=1.2)
            continue
        h = find_first(img, _feat_dialog)
        if h:
            print('  关闭通用对话框@(%d,%d)' % (h[1], h[2]))
            tap(h[1] + 960, h[2] + 30, dt=1.2)
            continue
        return True
    return False


def ensure_picker(tab_xy, verify_feat, max_try=3):
    """回到训练页并打开 [tab_xy] 对应的选兵面板（用 [verify_feat] 验证是否已打开）。"""
    for _ in range(max_try):
        ensure_online()
        close_dialogs()
        img = cv2.imread(cap('pk0.png'))
        if page_of(img) == 'main_village':
            h = find_first(img, _feat_train_troops)
            if h:
                tap(h[1], h[2], dt=2.5)
                img = cv2.imread(cap('pk1.png'))
        if verify_feat is not None and find_first(img, [verify_feat]):
            return True
        if panel_open(img):
            tap(219, 139, dt=1.0)      # 先关掉当前面板，避免"点开又关掉"
        tap(tab_xy[0], tab_xy[1], dt=1.5)
        if verify_feat is None or find_first(cv2.imread(cap('pk2.png')), [verify_feat]):
            return True
    return False
