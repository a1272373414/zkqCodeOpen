# -*- coding: utf-8 -*-
"""基于 adb sendevent 的多点触控调试工具（不依赖 WebSocket Server）。

背景：``adb shell input`` 命令只支持单指（tap / swipe），无法模拟双指缩放这类
多点手势。本项目 App 内部与 tools/multi_touch.py 走的是设备端 WebSocket Server
（root uinput 多点注入），而纯 adb 调试（tools/game_state.py 等脚本）一直受限于
``input`` 命令，无法做双指缩放（见 game_state.py 中 ensure_night_village /
ensure_main_village 的历史注释）。

本模块绕过 ``input``，直接通过 ``adb shell sendevent`` 向触摸屏设备节点写入多点触控
（MT，协议 B）事件序列，从而支持双指缩放 / 双指平移等手势，让纯 adb 调试也能复现
App 内的 pinchIn / pinchOut。

实现要点：
  - 自动探测触摸屏设备节点（``getevent -p`` 找含 ABS_MT_POSITION_X 的设备）。
  - 自动读取 ABS 轴 max，将 1280×720 逻辑坐标映射到设备原生坐标。
  - 把所有 sendevent 用 ``;`` 串联成单条 ``adb shell`` 执行，把几百次往返压缩成一次，
    保证双指手势连续、流畅。
  - 每个 SYN_REPORT 之间插入 ``sleep``，模拟人手节奏（缩放看相对位移，不卡顿即可）。

注意：写 /dev/input/eventX 通常需要 root；本项目运行在 root 环境（模拟器 / 真机 root）。
若探测不到设备，会回退 1:1 坐标并打印提示。

用法：
    import sys; sys.path.insert(0, 'tools')
    from adb_multitouch import AdbMultiTouch, pinch_in, pinch_out
    mt = AdbMultiTouch()
    mt.pinch_in_raw(141, 423, 1052, 352, 638, 365, 638, 365)  # 复现 App 的 zoomSmallMainBase 缩小
    pinch_in(640, 360, spread=320, final=120)                  # 双指收拢 = 缩小（zoom out）
    pinch_out(640, 360, spread=120, final=320)                 # 双指张开 = 放大（zoom in）
"""
import subprocess
import sys
import time

ADB = r"C:\Users\TANG\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "emulator-5556"

# linux/input.h 事件编码
EV_SYN = 0
EV_KEY = 1
EV_ABS = 3

SYN_REPORT = 0

BTN_TOUCH = 330

ABS_MT_SLOT = 47
ABS_MT_TOUCH_MAJOR = 48
ABS_MT_POSITION_X = 53
ABS_MT_POSITION_Y = 54
ABS_MT_TRACKING_ID = 57
ABS_MT_PRESSURE = 58

_BASE_W = 1280
_BASE_H = 720


def _adb(args, timeout=60):
    """执行 adb 命令，返回 stdout 文本。"""
    return subprocess.run(
        [ADB, "-s", SERIAL] + args,
        capture_output=True, text=True, timeout=timeout,
        check=True,
    ).stdout


class AdbMultiTouch:
    def __init__(self, verify_device=True):
        self.dev = None
        self.xmax = None
        self.ymax = None
        if verify_device:
            self._detect()

    def _detect(self):
        """探测触摸屏设备节点及其 ABS 轴范围。"""
        try:
            out = _adb(["shell", "getevent", "-p"])
        except Exception as e:
            print("[AdbMultiTouch] getevent -p 失败：%s（将回退 1:1 坐标）" % e)
            return
        parts = _split_devices(out)
        for part in parts:
            mdev = _re_dev.search(part)
            if not mdev:
                continue
            xm = _re_x.search(part)
            ym = _re_y.search(part)
            if xm and ym:
                self.dev = mdev.group(1)
                self.xmax = int(xm.group(1))
                self.ymax = int(ym.group(1))
                print("[AdbMultiTouch] 触摸屏设备 = %s (ABS max %dx%d)" %
                      (self.dev, self.xmax, self.ymax))
                return
        print("[AdbMultiTouch] 未找到多点触控设备（将回退 1:1 坐标，可能需要 root 或换用 WebSocket 通道）")

    def _map(self, x, y):
        """1280×720 逻辑坐标 → 设备原生坐标。"""
        if self.xmax and self.ymax:
            dx = round(x * self.xmax / _BASE_W)
            dy = round(y * self.ymax / _BASE_H)
            return int(dx), int(dy)
        return int(round(x)), int(round(y))

    def _exec(self, cmds):
        """把多条 sendevent / sleep 用 `;` 串联成单条 adb shell 执行。"""
        if not self.dev:
            raise RuntimeError("未探测到触摸屏设备，无法发送多点触控事件")
        script = "; ".join(cmds)
        _adb(["shell", script])

    def _emit(self, cmds, x, y, slot, tracking_id, major=8, pressure=50):
        """向某个 slot 写入单指触点信息。"""
        dx, dy = self._map(x, y)
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_SLOT, slot))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_TRACKING_ID, tracking_id))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_POSITION_X, dx))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_POSITION_Y, dy))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_TOUCH_MAJOR, major))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_PRESSURE, pressure))

    def _down(self, cmds, p1, p2):
        self._emit(cmds, p1[0], p1[1], 0, 0)
        self._emit(cmds, p2[0], p2[1], 1, 1)
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_KEY, BTN_TOUCH, 1))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_SYN, SYN_REPORT, 0))

    def _move(self, cmds, p1, p2, step_ms):
        self._emit(cmds, p1[0], p1[1], 0, 0)
        self._emit(cmds, p2[0], p2[1], 1, 1)
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_SYN, SYN_REPORT, 0))
        if step_ms > 0:
            cmds.append("sleep %.3f" % (step_ms / 1000.0))

    def _up(self, cmds):
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_SLOT, 0))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_TRACKING_ID, -1))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_SLOT, 1))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_ABS, ABS_MT_TRACKING_ID, -1))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_KEY, BTN_TOUCH, 0))
        cmds.append("sendevent %s %d %d %d" % (self.dev, EV_SYN, SYN_REPORT, 0))

    def pinch_in_raw(self, x1, y1, x2, y2, fx1, fy1, fx2, fy2,
                     steps=20, step_ms=14, dt=0.9):
        """双指从 (x1,y1)/(x2,y2) 各自移动到 (fx1,fy1)/(fx2,fy2)。

        两指收拢（终点比起点更靠近）= 缩小画面；两指张开（终点比起点更分散）= 放大画面。
        参数与 App 的 TouchActions.pinchIn(...) 对齐：两指分别向各自的终点移动。
        """
        cmds = []
        self._down(cmds, (x1, y1), (x2, y2))
        for i in range(1, steps + 1):
            t = i / steps
            cx1 = x1 + (fx1 - x1) * t
            cy1 = y1 + (fy1 - y1) * t
            cx2 = x2 + (fx2 - x2) * t
            cy2 = y2 + (fy2 - y2) * t
            self._move(cmds, (cx1, cy1), (cx2, cy2), step_ms)
        self._up(cmds)
        self._exec(cmds)
        if dt:
            time.sleep(dt)

    def pinch_in(self, cx, cy, spread=320, final=120, steps=20, step_ms=14, dt=0.9):
        """以 (cx,cy) 为中心，双指从 ±spread/2 收拢到 ±final/2 = 缩小（zoom out）。"""
        self.pinch_in_raw(cx - spread / 2, cy, cx + spread / 2, cy,
                          cx - final / 2, cy, cx + final / 2, cy,
                          steps, step_ms, dt)

    def pinch_out(self, cx, cy, spread=120, final=320, steps=20, step_ms=14, dt=0.9):
        """以 (cx,cy) 为中心，双指从 ±spread/2 张开到 ±final/2 = 放大（zoom in）。"""
        self.pinch_in_raw(cx - spread / 2, cy, cx + spread / 2, cy,
                          cx - final / 2, cy, cx + final / 2, cy,
                          steps, step_ms, dt)


# --- getevent -p 解析（设备块切分 + 轴范围正则） ---
import re  # noqa: E402  （放在文件后部仅为就近使用）

_re_dev = re.compile(r'add device \d+:\s*(/dev/input/event\d+)')
_re_x = re.compile(r'0035\b[^\n]*max\s+(\d+)')
_re_y = re.compile(r'0036\b[^\n]*max\s+(\d+)')


def _split_devices(out):
    """按 `add device N: /dev/...` 把 getevent -p 输出切成设备块。"""
    return re.split(r'(?=add device \d+:\s*/dev/)', out)


# --- 模块级便捷函数（供 game_state.py 等直接 import） ---
_default_mt = None


def _get_default():
    global _default_mt
    if _default_mt is None:
        _default_mt = AdbMultiTouch()
    return _default_mt


def pinch_in(cx, cy, spread=320, final=120, steps=20, step_ms=14, dt=0.9):
    _get_default().pinch_in(cx, cy, spread, final, steps, step_ms, dt)


def pinch_out(cx, cy, spread=120, final=320, steps=20, step_ms=14, dt=0.9):
    _get_default().pinch_out(cx, cy, spread, final, steps, step_ms, dt)


def pinch_in_raw(x1, y1, x2, y2, fx1, fy1, fx2, fy2, steps=20, step_ms=14, dt=0.9):
    _get_default().pinch_in_raw(x1, y1, x2, y2, fx1, fy1, fx2, fy2, steps, step_ms, dt)


if __name__ == "__main__":
    mt = AdbMultiTouch()
    print("设备:", mt.dev, "ABS:", mt.xmax, mt.ymax)
