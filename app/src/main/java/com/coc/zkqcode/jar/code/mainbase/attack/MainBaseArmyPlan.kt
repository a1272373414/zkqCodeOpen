package com.coc.zkqcode.jar.code.mainbase.attack

import com.coc.zkqcode.jar.code.universal.recognizer.PixelFontOcr

/**
 * 主世界造兵方案（训练侧，与下兵解耦；不含英雄与攻城器）。
 *
 * 方案清单（UI 下拉选项顺序即下标）：
 *  0 蛮弓胖保底：野蛮人/弓箭手/巨人各占 1/3 兵营容量，余量补弓箭手；造 闪电法术×20。
 *  1 闪电飞龙：飞龙×20、气球兵×4、闪电法术×20。
 *  2 镜像飞龙：飞龙×20、气球兵×4、镜像法术×6、狂暴法术×2。
 *  3 图腾飞龙：飞龙×20、气球兵×4、图腾法术×20。
 *  4 手动配兵：不训练（保留用户在配兵界面配的兵）。
 *
 * 运行时若所选方案（1/2/3）所需的兵种/法术未解锁，则自动回退到方案 0（保底）。
 */
enum class ArmyPlan(val index: Int, val label: String) {
    BARB_ARCH_GIANT(0, "蛮弓胖保底"),
    LIGHTNING_DRAGON(1, "闪电飞龙"),
    MIRROR_DRAGON(2, "镜像飞龙"),
    TOTEM_DRAGON(3, "图腾飞龙"),
    MANUAL(4, "手动配兵");

    companion object {
        fun fromIndex(i: Int) = entries.getOrNull(i) ?: BARB_ARCH_GIANT
        val OPTIONS = entries.map { it.label }
    }
}

/** 普通兵种住房占用（法术占用法术营，单独计，不影响兵营容量分配）。 */
private val TROOP_HOUSING = mapOf(
    "野蛮人" to 1, "弓箭手" to 1, "巨人" to 5, "飞龙" to 20, "气球兵" to 5
)

/** 方案 1/2/3 的固定造兵清单（方案 0 走容量分配，方案 4 不训练）。 */
private val PLAN_TROOPS: Map<ArmyPlan, List<Pair<String, Int>>> = mapOf(
    ArmyPlan.LIGHTNING_DRAGON to listOf("飞龙" to 20, "气球兵" to 4),
    ArmyPlan.MIRROR_DRAGON to listOf("飞龙" to 20, "气球兵" to 4),
    ArmyPlan.TOTEM_DRAGON to listOf("飞龙" to 20, "气球兵" to 4),
)

private val PLAN_SPELLS: Map<ArmyPlan, List<Pair<String, Int>>> = mapOf(
    ArmyPlan.LIGHTNING_DRAGON to listOf("闪电法术" to 20),
    ArmyPlan.MIRROR_DRAGON to listOf("镜像法术" to 6, "狂暴法术" to 2),
    ArmyPlan.TOTEM_DRAGON to listOf("图腾法术" to 20),
)

/** 各方案所需训练的兵种（显示名+数量）；方案 0 容量方案返回空（另行计算）。 */
fun planTroops(plan: ArmyPlan): List<Pair<String, Int>> = PLAN_TROOPS[plan] ?: emptyList()

/** 各方案所需训练的法术（显示名+数量）。 */
fun planSpells(plan: ArmyPlan): List<Pair<String, Int>> = when (plan) {
    ArmyPlan.LIGHTNING_DRAGON -> listOf("闪电法术" to 20)
    ArmyPlan.MIRROR_DRAGON -> listOf("镜像法术" to 6, "狂暴法术" to 2)
    ArmyPlan.TOTEM_DRAGON -> listOf("图腾法术" to 20)
    ArmyPlan.BARB_ARCH_GIANT -> listOf("闪电法术" to 20)
    ArmyPlan.MANUAL -> emptyList()
}

/**
 * 方案 0（蛮弓胖保底）按兵营总容量 [totalHousing] 分配：
 * 巨人占 1/3（每只占 5 住房）、野蛮人占 1/3、弓箭手占 1/3，取整后余量补弓箭手。
 */
fun planBarbArchGiantTroops(totalHousing: Int): List<Pair<String, Int>> {
    val third = totalHousing / 3
    val giantHousing = (third / 5) * 5
    val rest = (totalHousing - giantHousing).coerceAtLeast(0)
    val barbHousing = rest / 2
    val archHousing = rest - barbHousing
    val used = giantHousing + barbHousing + archHousing
    val residual = (totalHousing - used).coerceAtLeast(0)
    return listOf(
        "巨人" to giantHousing / 5,
        "野蛮人" to barbHousing,
        "弓箭手" to archHousing + residual,
    )
}

/**
 * 读取训练页/军队页顶部的兵营容量 "已用/总"，返回 (已用, 总数)。
 *
 * 区域与源脚本一致：源脚本 `awcocx_main.lua` 的 `函数247a`（v=="兵"）在竖屏读
 * `(540,400)-(566,666)`（找到兵营图标时为 `(540, intY+18)-(566, intY+110)`）。
 * 按文档 11.3 的 90° 映射 `(x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1)` 换算到横屏即
 * `(400,153)-(666,179)`：**右边界 666 与 y 带 [153,179] 完全一致**。这里取源脚本
 * "锚定分支"的左边界（图标右侧 ≈580），比兜底区(400)更紧，只框住数字本身、避开左侧
 * 兵营图标与右侧红色感叹号（校验脚本见 tools/calibrate_capacity_region.py）。
 * 读取失败时返回 null。
 */
private val CAPACITY_REGION = intArrayOf(580, 153, 666, 179)

suspend fun readTroopHousing(): Pair<Int, Int>? {
    val text = PixelFontOcr.recognizeDigits(
        CAPACITY_REGION[0], CAPACITY_REGION[1], CAPACITY_REGION[2], CAPACITY_REGION[3],
        grayMin = 160, grayMax = 255, maxSaturation = 70
    ) ?: return null
    // 形如 "340/340"（'/' 现已可识别）；个别情况 '/' 仍可能退化成 '?'，所以按数字分组最稳：
    // 取「最后两组」= (已用, 总)，语义同源脚本的 splitStr(ret, "/")。
    val numbers = Regex("\\d+").findAll(text)
        .mapNotNull { it.value.toIntOrNull() }
        .toList()
    if (numbers.size < 2) return null
    return numbers[numbers.size - 2] to numbers[numbers.size - 1]
}

/** 只取兵营容量上限（总数），供旧调用点使用。 */
suspend fun readTroopHousingTotal(): Int? = readTroopHousing()?.second
