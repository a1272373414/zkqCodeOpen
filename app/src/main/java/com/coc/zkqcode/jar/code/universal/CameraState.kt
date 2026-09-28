package com.coc.zkqcode.jar.code.universal

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.jar.code.builderbase.others.zoomSmallBuilderBase
import com.coc.zkqcode.jar.code.mainbase.others.zoomSmallMainBase

/**
 * 镜头（缩放）全局状态 —— 对齐源脚本的全局标记 `_ENV["已缩小画面"]`。
 *
 * 源脚本语义（doc/decrypt/awcocx_main.lua）：
 *  - `函数72a`（双指捏合缩小）执行后置 `_ENV["已缩小画面"] = true`；
 *  - 需要远景的地方统一写 `if 已缩小画面 == false then 函数72a() end`（共 35 处），
 *    即"没缩过才缩一次"，避免每处都重做手势；
 *  - 放大画面（都城入口 `函数319a`）或切换村庄（`函数60a`/`函数61a` 末尾）后复位为 false，
 *    因为放大/切场景都会破坏"已缩小"这一前提；
 *  - 该标记是**全局共享**的：夜世界建筑识别、主世界找建筑大循环、打鱼/进攻、升墙、
 *    都城突袭、脚本功能主循环都在读它。
 *
 * 本项目的差异与取舍：
 *  - 原本没有这个标记，所有调用点都无条件 `zoomSmallMainBase()` / `zoomSmallBuilderBase()`。
 *    好处是不会漏缩，坏处是每处都重做完整手势（pinchIn + 多次 swipe），每处多花 1~3 秒。
 *  - 这里补上标记与幂等封装 [ensureZoomedOutMainBase] / [ensureZoomedOutBuilderBase]，
 *    并把"放大/切场景"的复位点补齐，语义与源脚本保持一致。
 *  - 仅当镜头处于**通用远景**时才允许标记为真：建造摆放视角（isForBuild）与进攻边缘视角
 *    （isForAttack）都是专用视角，不置真，避免后续模块误跳过缩小。
 */
object CameraState {
    /** true = 主世界镜头当前已是通用缩小（远景）状态。对应源 `已缩小画面`。 */
    @Volatile
    var mainVillageZoomedOut: Boolean = false

    /** true = 夜世界镜头当前已是通用缩小（远景）状态。 */
    @Volatile
    var builderBaseZoomedOut: Boolean = false

    /** 主世界当前缩放等级（0=最近 … 3=最远），-1=未知。由 MapLocator 写入。 */
    @Volatile
    var mainVillageZoomLevel: Int = -1

    /** 夜世界当前缩放等级，语义同 [mainVillageZoomLevel]。 */
    @Volatile
    var builderBaseZoomLevel: Int = -1

    /**
     * 标记主世界镜头"已被放大/平移/切场景"，不再是可复用的远景。
     * 调用点：都城入口放大（源 函数319a 内置 `已缩小画面=false`）、进入主世界成功
     * （源 函数60a 末尾复位）。
     */
    fun markMainVillageZoomedIn() {
        mainVillageZoomedOut = false
    }

    /** 同 [markMainVillageZoomedIn]，作用于夜世界（源 函数61a 末尾复位）。 */
    fun markBuilderBaseZoomedIn() {
        builderBaseZoomedOut = false
    }

    /**
     * 切换村庄（主世界 ↔ 夜世界 ↔ 都城）后调用：两个村庄的镜头状态同时失效。
     * 源脚本只有一个全局标记，切村庄处统一置 false（函数60a/61a 末尾）；
     * 本项目按村庄分了两个标记，故一次清两个。
     */
    fun markVillageSwitched() {
        mainVillageZoomedOut = false
        builderBaseZoomedOut = false
    }

    /** 切换账号 / 重开游戏时整体复位：新账号进入游戏后镜头是未知的，必须重新缩小。 */
    fun reset() {
        mainVillageZoomedOut = false
        builderBaseZoomedOut = false
        mainVillageZoomLevel = -1
        builderBaseZoomLevel = -1
    }
}

/**
 * 幂等版 [zoomSmallMainBase]：已是通用远景则跳过，否则缩小并置位。
 * 对应源脚本 `if 已缩小画面 == false then 函数72a() end`。
 */
suspend fun ensureZoomedOutMainBase(isForBuild: Boolean = false, isForAttack: Boolean = false) {
    // 专用视角（建造摆放 / 进攻边缘）每次都要重做，不参与幂等
    if (CameraState.mainVillageZoomedOut && !isForBuild && !isForAttack) {
        ShowMessage("主世界：镜头已是缩小状态，跳过重复缩小")
        return
    }
    zoomSmallMainBase(isForBuild = isForBuild, isForAttack = isForAttack)
}

/** 幂等版 [zoomSmallBuilderBase]，语义同 [ensureZoomedOutMainBase]。 */
suspend fun ensureZoomedOutBuilderBase(isForBuild: Boolean = false) {
    if (CameraState.builderBaseZoomedOut && !isForBuild) {
        ShowMessage("夜世界：镜头已是缩小状态，跳过重复缩小")
        return
    }
    zoomSmallBuilderBase(isForBuild = isForBuild)
}
