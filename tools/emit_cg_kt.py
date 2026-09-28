# -*- coding: utf-8 -*-
"""生成部落竞赛图标模板的 Kotlin 源文件。

复用 gen_cg_colors.py 的坐标换算（竖屏 720x1280 -> 横屏 1280x720：x' = y, y' = 719 - x；偏移 (dx,dy) -> (dy,-dx)）。
输出：
  - ClanGamesTaskIcons.kt   竞赛任务图标（源 函数231a L33301~33512）
  - ClanGamesRewardIcons.kt 竞赛奖励图标（源 函数232a L33559~33605）
"""
import io
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_cg_colors import SRC, CALL, CALL_VAR, OFF, OFF_STR  # noqa: E402

OUT_DIR = r"d:\work\my\zkqfz\zkqCodeOpen\app\src\main\java\com\coc\zkqcode\jar\code\colorschema\colorpackage\mainbase"


def collect(start, end, prefix, override=None):
    """返回 [(源行号, Kotlin 片段)]"""
    with open(SRC, "rb") as f:
        raw = f.read()
    lines = raw.replace(b"\r", b"").split(b"\n")
    items = []
    idx = 0
    for i in range(start, min(end, len(lines)) + 1):
        text = lines[i - 1].decode("utf-8", "ignore")
        m = CALL.search(text)
        already_mapped = False
        if m:
            x1, y1, x2, y2 = (int(m.group(k)) for k in (1, 2, 3, 4))
            main_color = m.group(5)
        else:
            mv = CALL_VAR.search(text)
            if not mv:
                continue
            o = [int(v) for v in override.split(",")]
            x1, y1, x2, y2 = o[1], 719 - o[2], o[3], 719 - o[0]
            main_color = mv.group(1)
            m = mv
            already_mapped = True
        sim_m = re.findall(r",\s*(0\.\d+)\s*\)", text)
        sim = sim_m[-1] if sim_m else "0.9"
        off_m = OFF_STR.search(text[m.end():])
        offsets = []
        if off_m:
            for om in OFF.finditer(off_m.group(1)):
                dx, dy, col = int(om.group(1)), int(om.group(2)), om.group(3)
                offsets.append("%d|%d|%s" % (dy, -dx, col))
        idx += 1
        name = "%s%02d" % (prefix, idx)
        nx1, ny1, nx2, ny2 = (x1, y1, x2, y2) if already_mapped else (y1, 719 - x2, y2, 719 - x1)
        body = '    /** 源 L%d */\n    val %s = ColorSchema.parse(\n        %d, %d, %d, %d, "%s",\n        "%s",\n        0, %s, "%s"\n    )' % (
            i, name, nx1, ny1, nx2, ny2, main_color, ",".join(offsets), sim, name
        )
        items.append((i, name, body))
    return items


def emit(file_name, header, items, groups):
    """groups: [(组名, [名称...]), ...]"""
    buf = io.StringIO()
    buf.write(header)
    for _, _, body in items:
        buf.write(body + "\n\n")
    for gname, names in groups:
        buf.write("    /** %s */\n    val %s = listOf(\n" % (gname_zh(gname), gname))
        buf.write(",\n".join("        %s" % n for n in names))
        buf.write("\n    )\n\n")
    buf.write("}\n")
    path = os.path.join(OUT_DIR, file_name)
    with open(path, "w", encoding="utf-8") as f:
        f.write(buf.getvalue())
    print("written: " + path)


def gname_zh(name):
    return {
        "SIMPLE_TEMPLATES": "简单任务（源 v==1 通用段，先试这一组）",
        "HARD_TEMPLATES": "困难任务（源 v==2 追加段，简单组没命中时再试）",
        "ALL_TEMPLATES": "全部任务图标模板（简单 + 困难）",
        "ALL": "全部奖励图标模板（源 函数232a 的 16 组奖励图标）",
    }.get(name, name)


def main():
    task_simple = collect(33304, 33492, "ClanGamesTaskIcon")
    task_hard = collect(33495, 33505, "ClanGamesHardTaskIcon")
    rewards = collect(33559, 33605, "ClanGamesRewardIcon", "230,445,520,1110")
    print("task_simple=%d task_hard=%d rewards=%d" % (len(task_simple), len(task_hard), len(rewards)))

    task_header = '''@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * 部落竞赛「可接任务图标」模板库，移植自源脚本 `awcocx_main.lua` 函数231a（L33301~33512）。
 *
 * 作用：竞赛面板里任务卡片很多，源脚本不是"见到第一张卡就接"，而是先用一批**任务图标模板**
 * 在任务列表里挑出【夜世界（建筑大师）类】任务再接 —— 本项目 UI 上的说明
 * "只会接取夜世界竞赛任务"正是这个语义。
 *
 * 坐标：源脚本是竖屏 720x1280，本项目是横屏 1280x720，按项目通用 90° 映射换算
 * （区域 (x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1)；偏移 (dx,dy) -> (dy,-dx)）。
 * 颜色串沿用源脚本的 BGR 写法，与 [ColorSchema.parse] 一致。
 *
 * 分组（对齐源函数231a 的 v 参数）：
 *  - [SIMPLE_TEMPLATES] 源 `v == 1 or v == 2` 通用段（先找"简单"任务）
 *  - [HARD_TEMPLATES]   源 `v == 2` 追加段（简单没找到时，连"困难"任务一起找）
 *
 * TODO(真机标定)：这些模板取自 2023 年版游戏截图，当前版本的竞赛任务图标配色大概率已变化，
 * 首轮真机需用 `tools/make_feature.py` 重新标定命中率低的模板。
 */
object ClanGamesTaskIcons {

'''

    reward_header = '''@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * 部落竞赛「可领奖励图标」模板库，移植自源脚本 `awcocx_main.lua` 函数232a（L33559~33605）。
 *
 * 作用：竞赛的奖励档位是一排卡片，源脚本在某一行奖励区域里按顺序尝试 16 组奖励图标模板，
 * 命中即点该图标领取；一组都没命中时退回"随便点一张奖励卡"。
 *
 * 坐标同 [ClanGamesTaskIcons]：竖屏 720x1280 -> 横屏 1280x720 的 90° 映射。
 * 这里的区域 (445,199,1110,489) 是"整条奖励带"；调用方会按命中行用
 * [ColorSchema.rescope] 收窄到该行附近（对应源 `函数232a(230, y-44, 520, y+52)`）。
 *
 * TODO(真机标定)：同任务图标，配色需真机复核。
 */
object ClanGamesRewardIcons {

'''

    emit(
        "ClanGamesTaskIcons.kt", task_header, task_simple + task_hard,
        [
            ("SIMPLE_TEMPLATES", [n for _, n, _ in task_simple]),
            ("HARD_TEMPLATES", [n for _, n, _ in task_hard]),
            ("ALL_TEMPLATES", [n for _, n, _ in task_simple + task_hard]),
        ],
    )
    emit(
        "ClanGamesRewardIcons.kt", reward_header, rewards,
        [("ALL", [n for _, n, _ in rewards])],
    )


if __name__ == "__main__":
    main()
