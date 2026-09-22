"""双指 / 多点触控调试工具。

复用设备上的 ShellServer（ws://localhost:6839/zkq），与 App 的 TouchActions 走同一条注入通道，
坐标使用与游戏一致的 1280×720 逻辑坐标系。协议见 docs/ServerDoc.md（touch_action 无响应回包）。

用法：
    import sys; sys.path.insert(0, 'tools')
    from multi_touch import MultiTouch
    mt = MultiTouch()
    mt.pinch_in(640, 360, spread=320, final=120, steps=24)   # 双指收拢 = 缩小画面
    mt.pinch_out(640, 360, spread=120, final=320, steps=24)  # 双指张开 = 放大画面
"""
import asyncio
import json
import subprocess
import sys

sys.path.insert(0, ".")
import game_state as gs  # 复用 ADB / DEVICE

WS_URL = "ws://localhost:6839/zkq"


def _ensure_forward():
    # adb forward tcp:6839 tcp:6839（幂等，失败不抛）
    subprocess.run(
        f'"{gs.ADB}" forward tcp:6839 tcp:6839',
        shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )


class MultiTouch:
    def __init__(self, url=WS_URL, auto_forward=True):
        if auto_forward:
            _ensure_forward()
        import websockets  # 延迟导入，便于在无依赖环境下降级提示
        self._ws = websockets
        self.url = url
        self._loop = asyncio.new_event_loop()

    def _gesture(self, events):
        """events: [(delay_s, {json}), ...]，在单条连接里依次发送，每条之间 sleep。"""
        async def _go():
            async with self._ws.connect(self.url) as conn:
                for delay, obj in events:
                    await conn.send(json.dumps(obj))
                    if delay:
                        await asyncio.sleep(delay)
        self._loop.run_until_complete(_go())

    def down(self, id, x, y):
        self._gesture([(0, {"actionType": "touch_action", "subAction": "touchdown",
                             "x": float(x), "y": float(y), "id": int(id)})])

    def move(self, id, x, y):
        self._gesture([(0, {"actionType": "touch_action", "subAction": "touchmove",
                             "x": float(x), "y": float(y), "id": int(id)})])

    def up(self, id):
        self._gesture([(0, {"actionType": "touch_action", "subAction": "touchup",
                             "id": int(id)})])

    def connection_test(self):
        async def _go():
            async with self._ws.connect(self.url) as conn:
                await conn.send(json.dumps({"actionType": "connection_test"}))
                return await asyncio.wait_for(conn.recv(), timeout=3)
        return self._loop.run_until_complete(_go())

    def _pinch(self, x1, y1, x2, y2, fx1, fy1, fx2, fy2, steps, step_ms):
        sd = step_ms / 1000.0
        ev = [(0, {"actionType": "touch_action", "subAction": "touchdown",
                   "x": float(x1), "y": float(y1), "id": 1}),
              (0, {"actionType": "touch_action", "subAction": "touchdown",
                   "x": float(x2), "y": float(y2), "id": 2})]
        for i in range(1, steps + 1):
            t = i / steps
            ev.append((sd, {"actionType": "touch_action", "subAction": "touchmove",
                            "x": float(x1 + (fx1 - x1) * t), "y": float(y1 + (fy1 - y1) * t), "id": 1}))
            ev.append((0, {"actionType": "touch_action", "subAction": "touchmove",
                           "x": float(x2 + (fx2 - x2) * t), "y": float(y2 + (fy2 - y2) * t), "id": 2}))
        ev.append((0, {"actionType": "touch_action", "subAction": "touchup", "id": 1}))
        ev.append((0, {"actionType": "touch_action", "subAction": "touchup", "id": 2}))
        self._gesture(ev)

    def pinch_in(self, cx, cy, spread=320, final=120, steps=24, step_ms=15):
        """双指从 cx±spread/2 收拢到 cx±final/2 = 缩小（zoom out）。"""
        self._pinch(cx - spread / 2, cy, cx + spread / 2, cy,
                    cx - final / 2, cy, cx + final / 2, cy, steps, step_ms)

    def pinch_out(self, cx, cy, spread=120, final=320, steps=24, step_ms=15):
        """双指从 cx±spread/2 张开到 cx±final/2 = 放大（zoom in）。"""
        self._pinch(cx - spread / 2, cy, cx + spread / 2, cy,
                    cx - final / 2, cy, cx + final / 2, cy, steps, step_ms)


if __name__ == "__main__":
    mt = MultiTouch()
    try:
        print("connection_test ->", mt.connection_test())
    except Exception as e:
        print("WS 不可用：", e)
