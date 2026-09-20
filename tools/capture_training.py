# -*- coding: utf-8 -*-
"""Capture the main-base training-list screenshots for feature generation (task A).

Usage:
  1. Make sure the emulator (emulator-5556) is running and the game is on the
     MAIN VILLAGE (主村庄) screen.
  2. Run:  python tools/capture_training.py
  3. Screenshots are written to tools/captures/training/.

The script taps the on-screen buttons by their known 1280x720 coordinates
(reused from TrainTroops.kt / FeatureColors.kt), so the game must be in
landscape 1280x720.

Captured files:
  - 00_main_village.png        (sanity check: should show home base)
  - 01_training_base.png       (training page, before switching tabs)
  - 02_troops_top.png          (圣水兵 tab, top of list)
  - 03_troops_scroll1.png      (圣水兵 tab, scrolled once)
  - 04_troops_scroll2.png      (圣水兵 tab, scrolled twice)
  - 05_spells.png              (法术 tab)
  - 06_siege.png               (攻城机器 tab)
"""
import os
import sys
import time
import subprocess

ADB = r"C:\Users\TANG\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "emulator-5556"

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "captures", "training")
os.makedirs(OUT_DIR, exist_ok=True)


def adb(args):
    subprocess.run([ADB, "-s", SERIAL] + args, check=True)


def tap(x, y):
    adb(["shell", "input", "tap", str(x), str(y)])
    time.sleep(0.8)


def swipe(x1, y1, x2, y2, dur=300):
    adb(["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(dur)])
    time.sleep(1.0)


def capture(name):
    path = os.path.join(OUT_DIR, name)
    with open(path, "wb") as fp:
        subprocess.run([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"], stdout=fp, check=True)
    print("saved", path)
    time.sleep(0.6)


# Known coordinates (landscape 1280x720)
TRAIN_TROOPS_BTN = (52, 521)          # center of FeatureColors.TrainTroops (17,483,88,559)
TAB_TROOPS = (891, 234)
TAB_SPELLS = (797, 420)
TAB_SIEGE = (1126, 423)
CLOSE_PANEL = (219, 139)
# vertical scroll inside the troop list
SCROLL_X = 640
SCROLL_TOP = 220
SCROLL_BOTTOM = 660


def main():
    print("== capture_training: ensure game is on MAIN VILLAGE ==")
    # 00 sanity
    capture("00_main_village.png")

    # open training page
    tap(*TRAIN_TROOPS_BTN)
    time.sleep(1.5)
    capture("01_training_base.png")

    # 圣水兵 tab
    tap(*TAB_TROOPS)
    time.sleep(1.0)
    capture("02_troops_top.png")

    swipe(SCROLL_X, SCROLL_BOTTOM, SCROLL_X, SCROLL_TOP)
    capture("03_troops_scroll1.png")

    swipe(SCROLL_X, SCROLL_BOTTOM, SCROLL_X, SCROLL_TOP)
    capture("04_troops_scroll2.png")

    # back to top of list for the next tab switch
    swipe(SCROLL_X, SCROLL_TOP, SCROLL_X, SCROLL_BOTTOM)
    swipe(SCROLL_X, SCROLL_TOP, SCROLL_X, SCROLL_BOTTOM)
    time.sleep(0.5)

    # 法术 tab
    tap(*TAB_SPELLS)
    time.sleep(1.0)
    capture("05_spells.png")

    # 攻城机器 tab
    tap(*TAB_SIEGE)
    time.sleep(1.0)
    capture("06_siege.png")

    print("== done. files in:", OUT_DIR)


if __name__ == "__main__":
    main()
