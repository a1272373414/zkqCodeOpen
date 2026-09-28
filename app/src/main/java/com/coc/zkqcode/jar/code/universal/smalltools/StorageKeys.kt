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

    // 联赛(CWL)进攻主开关（T10）：按账号，1=开启
    const val LEAGUE_WAR_ENABLED = "LeagueWarEnabled"

    // 训练主开关（T12）：按账号，1=开启
    const val TRAIN_ENABLED = "TrainEnabled"
    // 升级主开关（T13）：按账号，1=开启
    const val UPGRADE_ENABLED = "UpgradeEnabled"

    // 每日福利主开关（T41）：按账号，1=开启
    const val DAILY_REWARDS_ENABLED = "DailyRewardsEnabled"
    // 每日福利各子项开关（按账号，1=开启）：令牌 / 红包 / 活动奖励 / 开箱 / 签到 / 卖药水
    const val DAILY_TOKEN = "DailyToken"
    const val DAILY_RED_PACKET = "DailyRedPacket"
    const val DAILY_EVENT = "DailyEvent"
    const val DAILY_CHEST = "DailyChest"
    const val DAILY_SIGN_IN = "DailySignIn"
    const val DAILY_SELL_POTION = "DailySellPotion"

    // 都城币捐主开关（T39）：按账号，1=开启
    const val CAPITAL_COIN_DONATE_ENABLED = "CapitalCoinDonateEnabled"
    // 都城币捐目标数量（按账号，空/0=默认捐满）
    const val CAPITAL_COIN_DONATE_AMOUNT = "CapitalCoinDonateAmount"

    // 审批入群主开关（T40）：按账号，1=开启
    const val CLAN_APPROVE_ENABLED = "ClanApproveEnabled"

    // 放置新建筑主开关（T38）：按账号，1=开启
    const val PLACE_BUILDING_ENABLED = "PlaceBuildingEnabled"
    // 放置新建筑目标（按账号，建筑名关键字，如「箭塔」「兵营」「墙」；空=不放置）
    const val PLACE_BUILDING_TARGET = "PlaceBuildingTarget"

    // 中文字库自动采集主开关（T34）：按账号，1=开启。开启后 bot 跑图过程中会定期
    // 用 ML Kit 当老师自动采集中文字模，默认关闭以免拖慢主流程。
    const val AUTO_HARVEST_FONT = "AutoHarvestFont"

    // 中文字库自动采集节流：上次采集时间戳（毫秒），按账号记录
    const val FONT_HARVEST_LAST = "FontHarvestLast"

    /**
     * Generate a storage key with account number suffix.
     * Used for per-account data that needs to be tracked separately.
     */
    fun withAccountNumber(key: String, accountNumber: Int): String {
        return "${key}${accountNumber}"
    }
}
