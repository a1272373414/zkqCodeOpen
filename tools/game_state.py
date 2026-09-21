# -*- coding: utf-8 -*-
"""Shared ground-truth helpers: page detection + safe recovery to training page.

Page detection mirrors SceneState.kt detectCurrentScene() / detectVillage():
  village FIRST (village-only builder icons) —— 训练部队按钮在主世界/夜世界/都城都会命中，
  不能用来判断所属村庄，所以先问"哪个村庄"：
    NIGHT_VILLAGE = BuilderBaseWorker / BuilderBaseWorker2          (BuilderBaseUpgradeColors.kt)
    MAIN_VILLAGE  = MainBaseWorker/2/3 / GoblinWorker               (MainBaseUpgradeColors.kt)
                    / GoblinResearcher                              (MainBaseResearchColors.kt)
  TRAINING_PAGE = TrainingPage / AttackInTrainingPage*              (MainBaseTraining.kt)
  BATTLE        = AttackButton (进攻！)                             (MainBaseAttackColors.kt)
  fallback      = MyColors.TrainTroops hit -> main_village          (FeatureColors.kt)
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
F_ATTACK = os.path.join(PKG, r"mainbase\MainBaseAttackColors.kt")
F_UPGRADE = os.path.join(PKG, r"mainbase\MainBaseUpgradeColors.kt")
F_RESEARCH = os.path.join(PKG, r"mainbase\MainBaseResearchColors.kt")
F_BB_UPGRADE = os.path.join(PKG, r"builderbase\BuilderBaseUpgradeColors.kt")
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
_feat_attack_button = parse_file(F_ATTACK, {"AttackButton"})

# --- 所属村庄识别（与 Kotlin SceneState.detectVillage 同源）---
# 主世界 / 夜世界 各由一组「村庄独有」的工人图标决定。
# 注意：训练部队按钮（FeatureColors.TrainTroops）在主世界、夜世界、都城都会命中，
# 只能说明"这是某个村庄 HUD"，不能说明是哪个村庄 —— 这正是旧 page_of 把夜世界误判成
# 主村庄的原因（见 SceneState.kt 的 Village / detectVillage 注释）。
_feat_worker_night = parse_file(F_BB_UPGRADE, {"BuilderBaseWorker", "BuilderBaseWorker2"})
_feat_worker_main = (parse_file(F_UPGRADE, {"MainBaseWorker", "MainBaseWorker2",
                                            "MainBaseWorker3", "GoblinWorker"})
                     + parse_file(F_RESEARCH, {"GoblinResearcher"}))


def village_of(img):
    """'main' / 'night' / 'unknown'：只用村庄独有工人图标判断（同 Kotlin detectVillage）。"""
    if find_first(img, _feat_worker_night):
        return "night"
    if find_first(img, _feat_worker_main):
        return "main"
    return "unknown"


def page_of(img):
    """Returns one of: main_village / night_village / training / battle / popup / unknown.

    顺序与 Kotlin SceneState.detectCurrentScene() 一致：
      ① 先问所属村庄（村庄独有工人图标）→ night_village / main_village
      ② 练兵页（TrainingPage / AttackInTrainingPage*）
      ③ 战斗页（AttackButton）
      ④ 兜底：训练部队按钮（主世界/夜世界/都城都有）→ main_village
      ⑤ 本工具额外区分：可关闭的弹窗 → popup
         （Kotlin 侧弹窗由 sweepBlockingPopups() 关闭、不作为场景，这里保留便于脚本处理）
    """
    v = village_of(img)
    if v == "night":
        return "night_village"
    if v == "main":
        return "main_village"
    if find_first(img, _feat_training):
        return "training"
    if find_first(img, _feat_attack_button):
        return "battle"
    if find_first(img, _feat_train_troops):
        return "main_village"
    if find_first(img, _feat_redx) or find_first(img, _feat_dialog):
        return "popup"
    return "unknown"


def in_village(img):
    """是否停在某个村庄 HUD（主世界或夜世界）——「训练部队」按钮在两个村庄都能点。"""
    return page_of(img) in ("main_village", "night_village")


_last_back_at = 0.0          # 未知弹窗兜底的限流时间戳（同 Kotlin UNKNOWN_OVERLAY_BACK_INTERVAL_MS）


def detect_page(rounds=2, name="detect.png", back_fallback=False, back_interval=8.0):
    """同 Kotlin SceneState.detectCurrentScene()。

    ① 村庄判不出时先关可关闭弹窗（close_dialogs：训练页上的 RedX 会被跳过），再重新识别一次；
    ② [back_fallback] 打开时，若识别不出任何已知页面，则按返回键兜底关闭"认不出的弹窗"
       （与 Kotlin dismissUnknownOverlayWithBack 一致：战斗中不按、且做时间间隔限流）。
       Kotlin 侧始终开启；工具侧默认关闭，避免影响交互式调试。
    """
    global _last_back_at
    img = cv2.imread(cap(name))
    for _ in range(max(0, rounds - 1)):
        if village_of(img) != "unknown":
            break
        hit = find_first(img, _feat_redx)
        if hit and not find_first(img, _feat_training):
            tap(hit[1], hit[2], dt=1.2)
        else:
            hit = find_first(img, _feat_dialog)
            if not hit:
                break
            tap(hit[1] + 960, hit[2] + 30, dt=1.2)
        img = cv2.imread(cap(name))
    page = page_of(img)
    if page == "unknown" and back_fallback and time.time() - _last_back_at >= back_interval:
        _last_back_at = time.time()
        print("  页面无法识别 → 按返回键尝试关闭未知弹窗")
        keyevent(4, dt=1.5)
        img = cv2.imread(cap(name))
        page = page_of(img)
    print("  识别页面 = %s（村庄 = %s）" % (page, village_of(img)))
    return page


def ensure_night_village(max_rounds=5):
    """从主世界切到夜世界（同 Kotlin enterBuilderBase：先把视角拉远，再点"去夜世界"的木船）。

    注意：缩放需要多点手势（adb 的 `input swipe` 做不了），这里只做 Kotlin
    zoomSmallMainBase 里的两次平移；实测在当前视角下平移后点 (317,474) 即可上船。
    """
    for _ in range(max_rounds):
        img = cv2.imread(cap('env0.png'))
        if village_of(img) == "night":
            return True
        tap(1279, 100, dt=0.6)                      # clickRightBottom(1)
        swipe(200, 500, 950, -500, dur=500, dt=1.0)  # zoomSmallMainBase 的平移
        swipe(218, 523, 939, 162, dur=500, dt=1.0)
        for x, y in ((317, 474), (336, 512), (313, 568), (300, 450), (330, 540)):
            tap(x, y, dt=1.6)
            if village_of(cv2.imread(cap('env1.png'))) == "night":
                return True
    return village_of(cv2.imread(cap('env2.png'))) == "night"


def ensure_main_village(max_rounds=6):
    """从夜世界（或其它页面）回到主世界（同 Kotlin enterMainBase 的点击序列）。

    注意：Kotlin enterMainBase 在点击前会先 zoomSmallBuilderBase()，其中包含一次双指缩小
    （TouchActions.pinchIn，需要多点触控）。模拟器的 adb input 只支持单指 tap/swipe，无法做
    双指缩放，因此在未缩小的视角下「回主世界」的船可能不在下方网格覆盖的范围内 —— 这是工具侧
    的输入限制，不影响 App 内 enterMainBase()（App 走 root uinput 多点注入）。若切不回去，
    请直接在模拟器里手动切回主世界再运行脚本。
    """
    for _ in range(max_rounds):
        img = cv2.imread(cap('emv0.png'))
        if village_of(img) == "main":
            return True
        tap(1279, 100, dt=0.6)                      # clickRightBottom(1)
        if village_of(cv2.imread(cap('emv1.png'))) == "main":
            return True
        # 夜世界地图右上角的「回营 / 船」热点（同 Kotlin enterMainBase）
        swipe(750, 150, 750, 550, dur=500, dt=0.8)
        for x in range(960, 1001, 30):
            for y in range(35, 211, 30):
                tap(x, y, dt=0.05)
        for x in range(1000, 1051, 30):
            for y in range(250, 331, 30):
                tap(x, y, dt=0.05)
        time.sleep(0.5)
    return village_of(cv2.imread(cap('emv2.png'))) == "main"


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
        if page == "night_village":
            # 本工具链的用例都针对主世界的练兵页；夜世界的「训练部队」会打开夜世界选兵面板，
            # 所以先切回主世界（同 Kotlin enterMainBase）。
            print('  当前在夜世界 → 先切回主世界')
            ensure_main_village()
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
    """关掉挡住界面的弹窗（RedX / 通用对话框，例如误点卡片"i"打开的兵种详情）。

    与 Kotlin sweepBlockingPopups 一致：训练部队页面右上角也有一个"红叉"（点它是关闭训练页），
    它同样会被 RedX 特征命中（实机 (1217,65)），这里必须跳过，否则会把训练页本身关掉。
    """
    for _ in range(max_rounds):
        img = cv2.imread(cap('dlg.png'))
        if not find_first(img, _feat_training):
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
        if in_village(img):
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
