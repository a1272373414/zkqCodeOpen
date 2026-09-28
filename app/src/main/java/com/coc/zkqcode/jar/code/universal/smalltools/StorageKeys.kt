package com.coc.zkqcode.jar.code.universal.smalltools

/**
 * Centralized storage key management for persistent data.
 * All storage keys used with readMemory/writeMemory should be defined here.
 */
object StorageKeys {
    // Global keys (no account suffix)
    const val ACCOUNT_NUMBER = "accountNumber"

    // Per-account keys (use withAccountNumber to add account suffix)
    const val MAIN_BASE_REMOVE_OBSTACLES = "MainBaseRemoveObstacles"
    const val BUILDER_BASE_REMOVE_OBSTACLES = "BuilderBaseRemoveObstacles"
    const val CHECK_NEW_BUILDING_ARROWS = "CheckNewBuildingArrows"
    const val MAIN_BASE_CHECK_TUTORIALS = "MainBaseCheckTutorials"
    const val MAIN_BASE_TRAIN_TROOPS = "MainBaseTrainTroops"
    const val BUILDER_BASE_TRAIN_TROOPS = "BuilderBaseTrainTroops"
    const val CLICK_OTTOS_POST = "ClickOttosPost"
    const val JOIN_CLAN = "JoinClan"
    const val PLACE_HERO_BANNERS = "PlaceHeroBanners"
    const val UPGRADE_GEARS_AND_PETS = "UpgradeGearsAndPets"

    // Per-account keys for clan games (部落竞赛)
    /** 上次「检查竞赛」的时间戳（节流用，对齐源脚本 3 小时一次）。 */
    const val CLAN_GAMES_CHECK = "ClanGamesCheck"

    /** 上次「没找到竞赛屋」的时间戳（节流用，对齐源脚本 10 分钟后再找）。 */
    const val CLAN_GAMES_NOT_FOUND = "ClanGamesNotFound"

    /** 账号是否已接取竞赛任务（"1"=已接，对应源 `已接主竞赛/已接夜竞赛`）。 */
    const val CLAN_GAMES_ACCEPTED = "ClanGamesAccepted"

    // Per-account keys for dynamic adjustment resource thresholds
    const val DYNAMIC_GOLD = "DynamicGold"
    const val DYNAMIC_ELIXIR = "DynamicElixir"
    const val DYNAMIC_DARK_ELIXIR = "DynamicDarkElixir"

    // Per-account key for war base layout switching (换阵, T04): 目标阵型号 1~6，0/空=不换
    const val WAR_BASE_LAYOUT = "WarBaseLayout"

    // 商人/每周精选购买主开关（T08）：按账号，1=开启
    const val TRADER_ENABLED = "TraderEnabled"

    // 部落战进攻主开关（T09）：按账号，1=开启
    const val CLAN_WAR_ENABLED = "ClanWarEnabled"
    // 部落战「留1刀」：开启时仅剩 1 次机会则跳过（源「留一次进攻机会」）
    const val CLAN_WAR_KEEP_ONE = "ClanWarKeepOne"

    /**
     * Generate a storage key with account number suffix.
     * Used for per-account data that needs to be tracked separately.
     */
    fun withAccountNumber(key: String, accountNumber: Int): String {
        return "${key}${accountNumber}"
    }
}
