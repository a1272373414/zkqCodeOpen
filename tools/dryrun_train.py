# -*- coding: utf-8 -*-
"""End-to-end dry run of mainBaseTrainTroops() on the emulator.

Mirrors the .kt logic step-by-step using adb taps + the project's real ColorSchema
matching (match_schema.py) so we can confirm the training flow is stable on-device:
  - open training page
  - clear Queue 1 (DeleteAll1 + MiddleGreenYes)
  - troops tab: priority dragon > giant > archer > barbarian, stop on GrayBarbarian (full)
  - clear Queue 2
  - spells tab: lightning sequence
  - clear Queue 3
  - siege tab: sequence
  - close

This is a UI-level execution of the SAME steps the Kotlin function performs (the compiled
script cannot be launched from CLI here), so it validates coordinates + branch logic, not
the Kotlin compiler path.
"""
import os
import re
import sys
import time
import subprocess
import io
import contextlib

ROOT = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ROOT)
import match_schema as ms  # noqa: E402

ADB = r"C:\Users\TANG\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "emulator-5556"
CAP = os.path.join(ROOT, "_tmp_cap.png")
DELAY = 1.0  # base delay (multiplier = 1)


def adb(args):
    subprocess.run([ADB, "-s", SERIAL] + args, check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def tap(x, y, d=0.4):
    adb(["shell", "input", "tap", str(x), str(y)])
    time.sleep(d)


def capture():
    adb(["shell", "screencap", "-p", "/sdcard/_cap.png"])
    adb(["pull", "/sdcard/_cap.png", CAP])
    adb(["shell", "rm", "/sdcard/_cap.png"])


def match(name):
    """Return (x, y) or None, using the real ColorSchema matching."""
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        res = ms.match(name, CAP)
    return res


def log(msg):
    print("[dryrun]", msg, flush=True)


def confirm_delete(target):
    """Tap the specific [target] DeleteAll schema if present, then confirm MiddleGreenYes."""
    capture()
    p = match(target)
    if p:
        log(f"  {target} 命中 {p} -> 点击删除")
        tap(p[0], p[1], 0.5)
        capture()
        yes = match("MiddleGreenYes")
        if yes:
            log(f"    确认弹窗命中 {yes} -> 确认")
            tap(yes[0], yes[1], 0.5)
            return True
        log(f"  {target} 点击后未出现确认弹窗（已清空或无需确认）")
        return True
    log(f"  未找到 {target}（队列为空，跳过清空）")
    return False


def switch_tab(x, y, verify, tab):
    tap(x, y, 0.8)
    capture()
    p = match(verify)
    if p:
        log(f"  切到【{tab}】成功（{verify} 命中 {p}）")
        return True
    log(f"  切到【{tab}】可能失败（未找到 {verify}）")
    return False


def main():
    log("=== 开始端到端练兵 dry run ===")
    capture()

    # 1. open training page
    p = match("TrainTroops")
    if not p:
        log("未找到 TrainTroops 按钮，中止")
        return
    log(f"点击 TrainTroops {p} 打开训练页")
    tap(p[0], p[1], 0.6)
    time.sleep(2.0)
    capture()

    # verify training page opened (TRAINING_PAGE signature: TrainingPage / AttackInTrainingPage*)
    tp = match("TrainingPage") or match("AttackInTrainingPage") or match("AttackInTrainingPage2") or match("AttackInTrainingPage3")
    if tp:
        log(f"训练页已打开（TRAINING_PAGE 特征命中 {tp}）")
    else:
        log("警告：未检测到 TRAINING_PAGE 特征，但继续")

    # 2. Clean Queue 1
    log("清理队列1")
    confirm_delete("DeleteAll1")

    # 3. troops tab + train
    log("切到圣水兵并训练")
    switch_tab(891, 234, "TrainBarbarian", "圣水兵")
    capture()
    full = False
    for i in range(1, 9):
        cap_t = match("TrainDragon") or match("TrainDragon2")
        if cap_t:
            log(f"  优先龙，连点25次 @ {cap_t}")
            for _ in range(25):
                tap(cap_t[0], cap_t[1], 0.04)
            full = True
            break
        g = match("TrainGiant")
        if g:
            for _ in range(5):
                tap(g[0], g[1], 0.04)
        a = match("TrainArcher")
        if a:
            for _ in range(40):
                tap(a[0], a[1], 0.04)
        b = match("TrainBarbarian")
        if b:
            for _ in range(40):
                tap(b[0], b[1], 0.04)
        capture()
        if match("GrayBarbarian"):
            log(f"  第{i}轮检测到 GrayBarbarian（满员）-> 停止训练")
            full = True
            break
        time.sleep(0.3)
    if not full:
        log("  8轮内未触发满员（资源不足或容量未满），属正常")

    # close troops tab
    log("关闭圣水兵面板 (219,139)")
    tap(219, 139, 1.0)

    # 4. Clean Queue 2
    log("清理队列2")
    confirm_delete("DeleteAll2")

    # 5. spells tab
    log("切到法术")
    switch_tab(797, 420, "TrainLighteningSpell", "法术")
    if match("TrainLighteningSpell"):
        coords = [(351, 621), (351, 621), (351, 621), (220, 499),
                  (91, 494), (91, 494), (91, 494), (91, 494)]
        for c in coords:
            tap(c[0], c[1], 0.05)
        log("  法术序列完成")
    tap(219, 139, 1.0)

    # 6. Clean Queue 3
    log("清理队列3")
    confirm_delete("DeleteAll3")

    # 7. siege tab
    log("切到攻城机器")
    switch_tab(1126, 423, "TrainSiegeMachine", "攻城机器")
    if match("TrainSiegeMachine"):
        coords = [(1047, 543), (610, 535), (364, 536), (138, 541)]
        for c in coords:
            tap(c[0], c[1], 0.05)
        log("  攻城序列完成")
    tap(219, 139, 1.0)
    tap(1232, 65, 0.3)

    capture()
    log("=== dry run 结束，最终回到主村/军队页，检查主界面特征 ===")
    home = match("TrainTroops")
    log(f"主界面 TrainTroops 命中: {home} (非None即已回到主界面)")


if __name__ == "__main__":
    main()
