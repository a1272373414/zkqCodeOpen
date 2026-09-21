package com.coc.zkqcode.jar.code.universal.deploy

import com.coc.zkqcode.jar.code.universal.smalltools.getConfigRuntime
import com.coc.zkqcode.jar.ui.schema.Schema

/**
 * Deploy "strategy" primitives ported from the legacy source project
 * (cocfz-apk-test, `awcocx_main.lua`). These describe *how* troops are placed,
 * as opposed to *which* troops are present (that is the job of the color schemas).
 *
 * Legacy naming kept in the comments so the mapping stays traceable.
 */

/**
 * Mirrors the legacy `下兵方式` (deploy mode) config:
 * - [FOUR_SIDES]         "四面"           deploy on both the top and bottom sides
 * - [SINGLE_FAKE_SWIPE]  "单面仿滑屏"      single side, simulated swipe (tap stream)
 * - [SINGLE_REAL_SWIPE]  "单面真滑屏"      single side, real multi-finger swipe
 * - [SINGLE_CENTER_TAP]  "单面中间单点"    single side, repeated taps at the quadrant middle
 */
enum class DeployMode(val label: String) {
    FOUR_SIDES("四面"),
    SINGLE_FAKE_SWIPE("单面仿滑屏"),
    SINGLE_REAL_SWIPE("单面真滑屏"),
    SINGLE_CENTER_TAP("单面中间单点")
}

/**
 * Mirrors the legacy `援兵位置` (reinforcement / main-force position) config.
 * [isTop] tells whether the quadrant sits on the "upper" half of the battlefield, [isLeft]
 * whether it sits on the left half - the source keys its per-type offset signs off both.
 */
enum class DeploySide(val label: String, val isTop: Boolean, val isLeft: Boolean) {
    TOP_LEFT("左上", true, true),
    TOP_RIGHT("右上", true, false),
    BOTTOM_LEFT("左下", false, true),
    BOTTOM_RIGHT("右下", false, false)
}

/**
 * Mirrors the legacy `放兵类型` switch inside `函数327a`. Each type pulls the drop point inward by
 * a different amount ([inwardOffset], the source's portrait pixels) so troops and spells land at
 * slightly different spots; the sign per quadrant is applied by [DeployGeometry].
 *
 * 图腾 is wave-dependent and handled separately by [DeployGeometry.typeOffsetN].
 */
enum class DeployType(val label: String, val inwardOffset: Int) {
    TROOP("兵种", 0),
    SPELL("法术", 30),
    SKELETON("骷髅", 80),
    BAT("蝙蝠", 80),
    JUMP("弹跳", 73),
    EARTHQUAKE("地震", 60),
    FREEZE("冰冻", 95),
    INVISIBLE("隐形", 50),
    MIRROR("镜像", 30),
    ENTANGLE("蔓生", 100),
    POISON("毒药", 130),
    LIGHTNING("雷电", 150),
    RAGE("愤怒法术", 30),
    TOTEM("图腾", 120),
    BOMB("炸弹", 30),
    ICE_BLOCK("冰障", 50)
}

/**
 * Runtime-tunable deploy settings. Mirrors the legacy globals `下兵方式` / `援兵位置` / `放兵速度`.
 * `speed` uses the legacy unit (default 25, hard lower bound 15) and feeds tap intervals.
 *
 * `count` is the legacy `兵数`: the source reads it from its army config, this project does not
 * know it, so it is the "how many units do we assume one round holds" default used to cut the
 * quadrant line into steps (16 -> 4 taps per quadrant in 四面, which is the source's most common
 * case).
 */
data class DeploySettings(
    val mode: DeployMode = DeployMode.FOUR_SIDES,
    val reinforcementSide: DeploySide = DeploySide.TOP_LEFT,
    val speed: Int = DEFAULT_SPEED,
    val count: Int = DEFAULT_COUNT
) {
    /** Legacy clamps 放兵速度 to a minimum of 15. */
    val safeSpeed: Int get() = speed.coerceAtLeast(MIN_SPEED)

    companion object {
        const val DEFAULT_SPEED = 25
        const val MIN_SPEED = 15
        const val DEFAULT_COUNT = 16
    }
}

/**
 * Deploy settings read from the schema config, mirroring the source's `活鱼下兵方式` /
 * `援兵位置` / `放兵速度`.
 *
 * The 下兵方式 / 援兵位置 dropdowns store the option INDEX, which maps 1:1 onto the declaration
 * order of [DeployMode] / [DeploySide] (and matches the legacy order: 四面 / 单面仿滑屏 /
 * 单面真滑屏 / 单面中间单点, 左上 / 右上 / 左下 / 右下).
 */
fun readDeploySettings(): DeploySettings {
    val mode = DeployMode.values().getOrElse(
        readDeployIndex(Schema.MAIN_BASE_SETTINGS.DEPLOY_MODE.key)
    ) { DeployMode.FOUR_SIDES }
    val side = DeploySide.values().getOrElse(
        readDeployIndex(Schema.MAIN_BASE_SETTINGS.DEPLOY_SIDE.key)
    ) { DeploySide.TOP_LEFT }
    return DeploySettings(mode = mode, reinforcementSide = side)
}

/** Reads a dropdown value as an option index, falling back to 0 (the first option). */
private fun readDeployIndex(key: String): Int =
    runCatching { getConfigRuntime(key).trim().toIntOrNull() ?: 0 }.getOrDefault(0)
