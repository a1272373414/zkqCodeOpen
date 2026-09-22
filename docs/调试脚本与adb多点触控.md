# 调试脚本与 adb 多点触控

## 背景：adb 调试不支持双指缩放

`tools/game_state.py` 等调试脚本原本用 `adb shell input tap/swipe` 模拟触控。但 `adb input`
命令**只支持单指**，无法模拟双指缩放这类多点手势。

App 内部（`TouchActions.pinchIn` / `pinchOut`）与 `tools/multi_touch.py` 走的是设备端
WebSocket Server（root uinput 多点注入），能支持双指；而纯 adb 调试通道此前缺失该能力，
导致需要缩放的调试流程（切夜世界、回主世界、战斗归一化截图）只能用平移近似或手动兜底。

## 解决方案：adb_multitouch.py

新增 `tools/adb_multitouch.py`，绕过 `input`，直接通过 `adb shell sendevent` 向触摸屏设备
节点写入多点触控（MT，协议 B）事件序列，实现真正的双指缩放：

- 自动探测触摸屏设备节点（`getevent -p` 找含 `ABS_MT_POSITION_X` 的设备），并读取 ABS 轴
  max，将 1280×720 逻辑坐标映射到设备原生坐标。
- 用 `ABS_MT_SLOT` / `ABS_MT_TRACKING_ID` / `BTN_TOUCH` / `SYN_REPORT` 构造双指事件。
- 所有 `sendevent` 用 `;` 串联成**单条** `adb shell` 执行，把几百次往返压缩成一次；每个
  `SYN_REPORT` 之间插入 `sleep` 模拟人手节奏。
- 写 `/dev/input/eventX` 需要 root（本项目运行在 root 环境）；探测不到设备时会回退 1:1 坐标
  并打印提示。

### 用法

```python
import sys; sys.path.insert(0, 'tools')
from adb_multitouch import AdbMultiTouch, pinch_in, pinch_out

mt = AdbMultiTouch()
mt.pinch_in_raw(141, 423, 1052, 352, 638, 365, 638, 365)  # 复现 App 的 zoomSmallMainBase 缩小
pinch_in(640, 360, spread=320, final=120)   # 双指收拢 = 缩小（zoom out）
pinch_out(640, 360, spread=120, final=320)  # 双指张开 = 放大（zoom in）
```

- `pinch_in_raw(x1, y1, x2, y2, fx1, fy1, fx2, fy2, ...)`：两指分别从起点移动到终点，与 App
  `TouchActions.pinchIn` 的 8 坐标参数对齐（两指收拢 = 缩小，张开 = 放大）。
- `pinch_in` / `pinch_out`：以中心 + 幅度方式，便于一般调试。

## 集成点

- `tools/game_state.py`：新增 `pinch_in(...)` 包装；`ensure_night_village` 与
  `ensure_main_village` 现用真正的双指缩小（`pinch_in(141,423,1052,352,638,365,638,365)`），
  对齐 App 的 `zoomSmallMainBase` / `zoomSmallBuilderBase`。
- `tools/capture_bar.py`：`NORMALIZE_PAN` 补全了 `pinchIn` 归一化，完整复现
  `ZoomSmallMainBase(isForAttack=true)`，使战斗截图处于归一化缩放，可正确验证
  `DeployGeometry` 的固定部署坐标。

## 与 multi_touch.py 的区别

| 通道 | 底层 | 是否需要 Server | 适用场景 |
| --- | --- | --- | --- |
| `multi_touch.py` | WebSocket → root uinput | 需要（ws://localhost:6839/zkq） | App 运行时的调试 |
| `adb_multitouch.py` | `adb shell sendevent` | 不需要（需 root） | 纯 adb、Server 不可用时的调试 |

## 坐标约定

所有坐标沿用项目 1280×720 逻辑坐标系（与 `tap` / `swipe` 一致）。

## 验证

在主世界实测：触摸屏设备 `/dev/input/event2`，ABS 1279×719（≈ 1:1）。`pinch_in`（缩小）与
`pinch_out`（放大）均触发明显且方向相反的画面变化（像素差异均值 41~47），确认双指缩放在
主世界生效。
