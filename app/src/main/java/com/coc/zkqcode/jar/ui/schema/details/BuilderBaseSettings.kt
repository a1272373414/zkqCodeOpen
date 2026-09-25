@file:Suppress("ClassName")

package com.coc.zkqcode.jar.ui.schema.details

import com.coc.zkqcode.jar.ui.schema.SettingDef

object BuilderBaseSettings {
    val NO_BUILDER_BASE = SettingDef("no_builder_base", "不打夜世界", 0, "BUILDER_BASE_SETTINGS")
    val BUILDER_BASE_FARMING = SettingDef("builder_base_farming", "夜世界打资源", 1, "BUILDER_BASE_SETTINGS")
    // 夜世界下兵方案（默认方案3）：
    //   0/缺省/3 = 方案3（卡位式：全卡位轮询 + 固定边缘落点 + 白框识别 + 有效点池）
    //   1        = 当前方案（主世界几何+点选落点）
    //   2        = 方案2（源项目保真：函数123a/323a/128a）
    val NIGHT_WORLD_DEPLOY_PLAN = SettingDef("night_world_deploy_plan", "夜世界下兵方案(0/3=方案3卡位式,1=当前方案,2=方案2)", "3", "BUILDER_BASE_SETTINGS")
    val SWITCH_ACCOUNT_AFTER_BATTLES = SettingDef(
        "switch_account_after_battles", "每次对战以下局数后切号", 2, "BUILDER_BASE_SETTINGS"
    )
    val SWITCH_ACCOUNT_AFTER_BATTLES_WITH_TASKS = SettingDef(
        "switch_account_after_battles_with_tasks", "接取竞赛后，对战以下局数后切号", 4, "BUILDER_BASE_SETTINGS"
    )
    val STOP_WHEN_RESOURCE_FULL = SettingDef("stop_when_resource_full", "资源满后停止对战", 1, "BUILDER_BASE_SETTINGS")
    val TROPHY_PUSHING_MODE = SettingDef("trophy_pushing_mode", "上分模式", 0, "BUILDER_BASE_SETTINGS")
    val ELIXIR_CART_FARMING = SettingDef("elixir_cart_farming", "刷圣水车", 0, "BUILDER_BASE_SETTINGS")
    val BUILDER_BASE_RESEARCH = SettingDef("builder_base_research", "夜世界研究", 1, "BUILDER_BASE_SETTINGS")
    val BUILDER_BASE_BUILD_SETTING = SettingDef("builder_base_build_setting", "自动建造", 1, "BUILDER_BASE_SETTINGS")
    val BUILDER_BASE_WALL_UPGRADE_SETTINGS = SettingDef("builder_base_wall_upgrade_settings", "升级城墙", 1, "BUILDER_BASE_SETTINGS")
    val BUILDER_BASE_BATCH_WALL_UPGRADE_SETTINGS = SettingDef("builder_base_batch_wall_upgrade_settings", "批量升级城墙", 1, "BUILDER_BASE_SETTINGS")
    val BUILDER_BASE_UPGRADE_WALL_THRESHOLD = SettingDef(
        "builder_base_upgrade_wall_threshold", "金水高于以下百分比后升级城墙", "85", "BUILDER_BASE_SETTINGS"
    )
    val BUILDER_BASE_REMOVE_OBSTACLES = SettingDef("builder_base_remove_obstacles", "移除障碍物", 1, "BUILDER_BASE_SETTINGS")
    val BUILDER_BASE_SAVE_WORKER = SettingDef("builder_base_save_worker", "留1工人升级城墙", 0, "BUILDER_BASE_SETTINGS")

    val all = listOf(
        NO_BUILDER_BASE,
        BUILDER_BASE_FARMING,
        NIGHT_WORLD_DEPLOY_PLAN,
        SWITCH_ACCOUNT_AFTER_BATTLES,
        STOP_WHEN_RESOURCE_FULL,
        TROPHY_PUSHING_MODE,
        ELIXIR_CART_FARMING,
        BUILDER_BASE_RESEARCH,
        BUILDER_BASE_BUILD_SETTING,
        BUILDER_BASE_WALL_UPGRADE_SETTINGS,
        BUILDER_BASE_BATCH_WALL_UPGRADE_SETTINGS,
        BUILDER_BASE_UPGRADE_WALL_THRESHOLD,
        BUILDER_BASE_REMOVE_OBSTACLES,
        BUILDER_BASE_SAVE_WORKER,
        SWITCH_ACCOUNT_AFTER_BATTLES_WITH_TASKS
    )
}
