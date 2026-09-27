@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Clan capital RAID / currency features, migrated from the legacy freescript
 * (`awcocx_main.lua`) with the project-wide 90° portrait->landscape mapping.
 *
 * This is the colour layer of M4-③ (都城入口拆块移植):
 *   - B2 enter capital  : [CapitalClanEntryButton], [CapitalRaidMapBoat],
 *     [CapitalRaidCongrats], [CapitalRaidNewRaid]
 *   - B3 pick a district: [CapitalRaidMapAnchor] plus the dynamic `raid*` markers
 *     (the legacy script scans every district with its OWN rect, so those markers are exposed as
 *      factories taking the rect instead of fixed regions)
 *   - B4 attack / return: [CapitalRaidAttackCountUsed], [CapitalRaidStorageFull]
 *   - start raid / claim / donate: [CapitalRaidStart*], the `CapitalMint*` / `CapitalCoin*` /
 *     `CapitalCollect*` and `CapitalDonate*` sets
 *
 * Regenerate with tools/gen_capital_raid_colors.py.
 * NOTE: none of these could be verified on a device yet (there is no capital screenshot offline),
 * TODO: the legacy vault lookup uses an image match on 都城宝库.png.
 */
interface ICapitalRaidColors {
    val CapitalClanEntryButton: ColorSchema
    val CapitalClanEntryButton2: ColorSchema
    /** "要立即进攻吗？"确认弹窗：底部深绿横幅 + 右上大红X。 */
    val CapitalAttackConfirm: ColorSchema
    /** 确认弹窗中的"进攻"按钮 = 绿色（部队已满，可进攻）。 */
    val CapitalAttackConfirmReady: ColorSchema
    /** 确认弹窗中的"进攻"按钮 = 灰色（部队未满，不可进攻）。 */
    val CapitalAttackConfirmGray: ColorSchema
    /** 都城地图：选中子城后底部弹出的白色按钮条（侦察/进攻/详细信息）。 */
    val CapitalDistrictBar: ColorSchema
    /** 战斗结算界面的绿色"地图"回城按钮（源脚本"回营主"，放弃按钮消失后点击它回都城）。 */
    val CapitalResultMapButton: ColorSchema
    /**
     * 按钮条中的"进攻"按钮 = 红色（该子城可进攻）。
     * 2026-09-27 实机标定：旧 CapitalDistrictAttackGray（灰特征）在红色可用按钮上也会误命中，
     * 导致所有子城被误判"不可打"。改为反向判据：红色按钮命中=可打，未命中=灰/不可打。
     */
    val CapitalDistrictAttackRed: ColorSchema
    /** 按钮条中的"进攻"按钮 = 灰色（该子城已三星/不可进攻）。 */
    val CapitalDistrictAttackGray: ColorSchema
    /** 都城地图：选中子城后底部弹出的红色"进攻"按钮条（源脚本 L47307 cmpColorEx，映射自竖屏）。 */
    val CapitalBottomAttackBar: ColorSchema
    val CapitalRaidCongrats: ColorSchema
    val CapitalRaidNewRaid: ColorSchema
    val CapitalRaidMapAnchor: ColorSchema
    val CapitalRaidAttackCountUsed: ColorSchema
    val CapitalRaidStorageFull: ColorSchema
    val CapitalRaidStartButton: ColorSchema
    val CapitalRaidStartGray: ColorSchema
    val CapitalRaidStartConfirm: ColorSchema
    val CapitalMintWorkshop: ColorSchema
    val CapitalMintWorkshop2: ColorSchema
    val CapitalCoinScreen: ColorSchema
    val CapitalCoinFull: ColorSchema
    val CapitalCollectArrow: ColorSchema
    val CapitalCollectButton: ColorSchema
    val CapitalCollectConfirm: ColorSchema
    val CapitalCoinSecondPage: ColorSchema
    val CapitalDonatePoint: ColorSchema
    val CapitalDonatePoint2: ColorSchema
    val CapitalDonatePoint3: ColorSchema
    val CapitalDonatePoint4: ColorSchema
    val CapitalDonateConfirm: ColorSchema
    val CapitalDonatePlus: ColorSchema
    /** District white star (is any star already taken here) — the region is only known at runtime, build it with this factory. */
    fun raidStar(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema
    /** District star cluster (counted to learn how many stars are taken) — the region is only known at runtime, build it with this factory. */
    fun raidStarCount(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema
    /** District not unlocked yet (blue lock) — the region is only known at runtime, build it with this factory. */
    fun raidLocked(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema
    /** Somebody else is attacking this district (variant 1) — the region is only known at runtime, build it with this factory. */
    fun raidOccupied(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema
    /** Somebody else is attacking this district (variant 2) — the region is only known at runtime, build it with this factory. */
    fun raidOccupied2(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema
    val CapitalRaidMapBoat: ColorSchema
    val CapitalBuildingListMain: ColorSchema
    /** 都城村庄主界面顶部"部落都城"标签：只在都城主视图出现，区别于主世界/夜世界。 */
    val CapitalVillageLabel: ColorSchema
    /** 都城村庄主界面右上角"都城币"图标：金色六边形外框 + 红色六边形内芯，只在都城主视图出现。 */
    val CapitalGoldIcon: ColorSchema
}

object CapitalRaidColors : ICapitalRaidColors {
    // 都城入口 = 主世界海岸的热气球飞艇（橙红气球×3 + 棕色平台，上方有红色 S⚔ 突袭次数标记）。
    // 偏移按稳定镜头（zoomToCapitalShore 固定 pinchIn 时长后）的真实截图 fail_v4.png 重标：
    // 命中 @(1002,369)；偏移仅用气球本体×3 与平台暗部。
    // 注意：气球上方的红色 S⚔ 突袭次数标记是【对战期间才显示】的，不能作为特征点
    // （2026-09-26 曾纳入 S⚔ 偏移，被指出后移除）。夜世界与主世界 HUD 均零误命中。
    // 注意1：颜色串按 BGR 写（ColorSchema.parseBgrToRgb 按 0xBBGGRR 解析）。
    // 注意2：红白条纹帆船是去夜世界的船，绝不能作为都城入口特征（2026-09-26 曾误采船身导致点错）。
    override val CapitalClanEntryButton = ColorSchema.parse(
        700, 250, 1150, 600, "4E70FF",
        "-18|-14|5072FF,-99|63|4D76FF,-126|63|4B68F2,-45|70|071529",
        0, 0.88, "CapitalClanEntryButton"
    )

    // 都城入口兜底：同一热气球特征、放宽相似度以容忍光照/透视差异。
    override val CapitalClanEntryButton2 = ColorSchema.parse(
        700, 250, 1150, 600, "4E70FF",
        "-18|-14|5072FF,-99|63|4D76FF,-126|63|4B68F2,-45|70|071529",
        0, 0.82, "CapitalClanEntryButton2"
    )

    // Raid "congratulations" panel shown after a raid ends
    override val CapitalRaidCongrats = ColorSchema.parse(
        1051, 60, 1127, 115, "2722EE-101010",
        "0|-1|2722F0-101010,3|-10|8D8EF3-101010,2|-12|8D8EF3-101010,11|-12|FFFFFF-101010,15|-7|FFFFFF-101010,9|1|F8F9F8-101010,9|7|2520DC-101010",
        0, 0.9, "CapitalRaidCongrats"
    )

    // Brand new raid prompt (raid just started)
    override val CapitalRaidNewRaid = ColorSchema.parse(
        535, 383, 739, 608, "3AD38B-101010",
        "3|-42|8DEABE-101010,-79|-1|3AD48B-101010,-78|-44|8EEABF-101010,82|1|3AD38A-101010,83|-43|8EEABF-101010,-94|-105|2F2FB9-101010,-92|-129|2F2FB9-101010,97|-107|2E2EB8-101010,97|-124|2E2EB8-101010",
        0, 0.9, "CapitalRaidNewRaid"
    )

    // Raid map anchor = the 9-district grid base point (函数269a/270a intX)
    override val CapitalRaidMapAnchor = ColorSchema.parse(
        446, 6, 537, 327, "366C57-101010",
        "8|0|4EB075-101010,18|8|376B58-101010,26|3|55BD80-101010,37|6|152E3C-101010,40|-1|4FAA7B-101010,52|-2|56C284-101010,43|-13|757FA8-101010,31|-12|727FAE-101010,23|-11|727DAB-101010,6|-13|6C729C-101010,8|-27|4B4D7A-101010,20|-23|646D9E-101010,33|-24|636D9D-101010,43|-24|97A8E3-101010,26|-18|97A8E2-101010",
        0, 0.9, "CapitalRaidMapAnchor"
    )

    // No raid attacks left (0 attack chances)
    override val CapitalRaidAttackCountUsed = ColorSchema.parse(
        1163, 543, 1203, 576, "FFFFFF",
        "-2|-5|FFFFFF,1|-7|FFFFFF,5|-12|FFFFFF,11|-9|FFFFFF,9|-5|FFFFFF,11|-1|FFFFFF,0|6|000000,8|6|000000,0|9|2029DC,10|9|2029DC,15|-2|2027D2,15|-9|2122C4,6|-17|221DBA,0|-17|221DBA,-6|-9|2123C7,-6|0|2128D7,5|-7|000000",
        0, 0.9, "CapitalRaidAttackCountUsed"
    )

    // "Storage limit reached" popup (tap it away while deploying)
    override val CapitalRaidStorageFull = ColorSchema.parse(
        646, 400, 914, 532, "1DBD6D",
        "5|-67|89F9E0,105|3|1FBD70,102|-73|90FBE4,206|1|1DBD6D,200|-72|8EFAE3",
        0, 0.9, "CapitalRaidStorageFull"
    )

    // "Start raid" button, clickable state
    override val CapitalRaidStartButton = ColorSchema.parse(
        1133, 577, 1206, 697, "12509A",
        "-8|5|28C091,-9|-6|63D1B1,7|-36|FFF7D6,-1|-33|63D8B7,-26|-36|63DAB9,-35|-29|28C799,-34|26|28BB8C",
        0, 0.9, "CapitalRaidStartButton"
    )

    // "Start raid" button, greyed out (raid already running/finished)
    override val CapitalRaidStartGray = ColorSchema.parse(
        1148, 585, 1250, 642, "676767-101010",
        "11|10|5C5C5C-101010,13|3|C4C4C4-101010,7|-4|FCFCFC-101010,37|10|4B4B4B-101010,34|2|6C6C6C-101010,29|-4|B9B9B9-101010,30|-16|CACACA-101010,-8|-12|BEBEBE-101010,41|-12|BEBEBE-101010",
        0, 0.9, "CapitalRaidStartGray"
    )

    // "Start raid" confirmation button inside the popup
    override val CapitalRaidStartConfirm = ColorSchema.parse(
        514, 569, 769, 650, "2A91FF-101010",
        "-1|-55|31ADFF-101010,-112|8|2B8AFF-101010,-111|-54|32ACFF-101010,115|4|2B8EFF-101010,112|-57|34AFFF-101010,-90|-40|FADAA7-101010,-73|-37|F8D79F-101010",
        0, 0.9, "CapitalRaidStartConfirm"
    )

    // Mint workshop (capital gold building; tap it to open the coin panel)
    override val CapitalMintWorkshop = ColorSchema.parse(
        500, 308, 1181, 568, "8D97A8-101010",
        "-14|1|8795A5-101010,-19|-16|1F1D1E-101010,-24|-20|398DEB-101010,-19|-27|368DED-101010,-27|-34|8D7E85-101010,-25|-43|378FEE-101010,-17|-39|3891EE-101010,-3|-33|40ABFC-101010",
        0, 0.9, "CapitalMintWorkshop"
    )

    // Mint workshop, fallback variant (front view)
    override val CapitalMintWorkshop2 = ColorSchema.parse(
        500, 308, 1181, 568, "2F3330-101010",
        "-4|-13|378EEE-101010,1|-11|3990EE-101010,11|-4|1F1F20-101010,16|7|82909E-101010,16|15|8C98AB-101010,33|16|98A5B8-101010",
        0, 0.9, "CapitalMintWorkshop2"
    )

    // Capital coin panel is open (checked after tapping the mint)
    override val CapitalCoinScreen = ColorSchema.parse(
        1114, 50, 1197, 139, "2621E8-101010",
        "4|-31|8D8FF3-101010,40|1|2621E5-101010,39|-30|8D8FF3-101010,13|-6|FDF9FD-101010,15|-26|FFFFFF-101010,27|-7|F9F9F9-101010,28|-28|FFFFFF-101010",
        0, 0.9, "CapitalCoinScreen"
    )

    // Capital gold storage is full (stop collecting)
    override val CapitalCoinFull = ColorSchema.parse(
        939, 542, 957, 595, "0098F2",
        "2|-5|00ADFA,8|-6|E0E8E8,2|-10|00BCFD,7|-10|E0E8E8,2|-17|1ACCFF,8|-20|E0E8E8,0|-26|59DEFF,4|-30|E0E8E8",
        0, 0.9, "CapitalCoinFull"
    )

    // Collect arrow inside the mint (tap slightly left of it)
    override val CapitalCollectArrow = ColorSchema.parse(
        168, 246, 288, 435, "0499FF-101010",
        "41|0|0896FF-101010,18|3|00A0FF-101010,-7|-15|20A4FF-101010,17|-14|1FBBFF-101010,43|-14|24A4FF-101010,-20|-11|000000-101010,-20|-28|000000-101010",
        0, 0.9, "CapitalCollectArrow"
    )

    // Capital gold collect button (one per district)
    override val CapitalCollectButton = ColorSchema.parse(
        115, 369, 1170, 471, "0DADEE-101010",
        "1|-7|C5148E-101010,-3|-15|DD16B5-101010,3|-15|DD19B5-101010,13|-9|0D99E5-101010,-16|-1|38D188-101010,16|-1|36D187-101010,15|-26|36D28B-101010",
        0, 0.9, "CapitalCollectButton"
    )

    // Capital gold collect confirmation (blue button)
    override val CapitalCollectConfirm = ColorSchema.parse(
        613, 194, 676, 221, "1C1DFD",
        "1|0|1C1DFD,0|2|020205,1|-2|020205,-2|3|2728F6,-2|-2|2729F6,3|3|1F20FC,3|-2|1F20FC,0|7|000000,2|7|000000",
        0, 0.9, "CapitalCollectConfirm"
    )

    // Capital coin panel, second page (after a right swipe, TH>11)
    override val CapitalCoinSecondPage = ColorSchema.parse(
        360, 219, 1178, 371, "01172D",
        "0|-20|01172D,-4|-15|01172D,5|-17|01172D,-25|-27|00C1F5,25|-26|01B7F0,19|5|01AFEC,-17|6|01B3EF,-22|7|0A87D4,27|6|0184D3",
        0, 0.9, "CapitalCoinSecondPage"
    )

    // Vault donate point (green plus)
    override val CapitalDonatePoint = ColorSchema.parse(
        688, 102, 832, 493, "0DC58D-101010",
        "0|-4|0DE5B5-101010,0|-10|9DFFED-101010,14|1|15C38D-101010,14|-3|0DDDAD-101010,14|-9|A7FFF4-101010",
        2, 0.9, "CapitalDonatePoint"
    )

    // Vault donate point, fallback variant 1
    override val CapitalDonatePoint2 = ColorSchema.parse(
        450, 100, 554, 496, "8D857D-101010",
        "2|0|8D857C-101010,12|1|857D72-101010,14|1|857D6F-101010,14|-4|0D5BAD-101010,1|-9|0D6BBC-101010,6|-15|0D55A5-101010,18|-12|0D56A5-101010",
        2, 0.9, "CapitalDonatePoint2"
    )

    // Vault donate point, fallback variant 2
    override val CapitalDonatePoint3 = ColorSchema.parse(
        630, 98, 810, 493, "0DAFF1-101010",
        "2|-5|DD14B5-101010,4|-7|DD17B5-101010,4|-5|DD16B1-101010",
        2, 0.9, "CapitalDonatePoint3"
    )

    // Vault donate point, fallback variant 3
    override val CapitalDonatePoint4 = ColorSchema.parse(
        630, 98, 810, 493, "E018B6-101010",
        "1|-1|E018B6-101010,1|0|E018B5-101010,-6|3|0DB0F2-101010,-5|5|0DAFF3-101010,-3|7|0DB1F4-101010,7|3|0D9AE5-101010",
        2, 0.9, "CapitalDonatePoint4"
    )

    // Donate confirmation button
    override val CapitalDonateConfirm = ColorSchema.parse(
        601, 359, 680, 417, "19157D",
        "4|-9|0345B3,19|8|191787,19|-3|0538A5,34|2|1B157F,34|-6|0339A9,2|-27|036DBE,19|-20|017BCC,31|-22|0984D2",
        0, 0.9, "CapitalDonateConfirm"
    )

    // Donate amount "+" (legacy holds it to max out the amount)
    override val CapitalDonatePlus = ColorSchema.parse(
        561, 310, 727, 363, "B1B1B1",
        "8|3|B1B1B1,26|3|B1B1B1,91|3|B1B1B1,120|2|B1B1B1,126|-1|B1B1B1,123|-4|FFFFFF,112|-3|FFFFFF,79|-3|FFFFFF,42|-4|FFFFFF,8|-4|FFFFFF,1|-6|FFFFFF,-2|-9|FFFFFF",
        0, 0.9, "CapitalDonatePlus"
    )

    // District white star (is any star already taken here)
    override fun raidStar(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema =
        ColorSchema.parse(
            x1, y1, x2, y2, "FFFFFF-101010",
            "1|-1|FFFFFF-101010,1|-2|FFFFFF-101010,2|-3|FFFFFF-101010,2|-5|FFFFFF-101010,2|-7|FFFFFF-101010,3|-4|FFFFFF-101010,3|-6|FFFFFF-101010,3|-8|FFFFFF-101010,5|-9|FFFFFF-101010",
            0, 0.9, "raidStar"
        )

    // District star cluster (counted to learn how many stars are taken)
    override fun raidStarCount(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema =
        ColorSchema.parse(
            x1, y1, x2, y2, "FFFFFF-101010",
            "-4|-10|CBCBCB-101010,-3|-12|FFFFFF-101010,6|-20|FFFFFF-101010,16|-12|FFFFFF-101010,17|-10|CCCCCC-101010,13|-1|FFFFFF-101010,6|-4|FFFFFF-101010,2|-14|FFFFFF-101010",
            0, 0.9, "raidStarCount"
        )

    // District not unlocked yet (blue lock)
    override fun raidLocked(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema =
        ColorSchema.parse(
            x1, y1, x2, y2, "0DC0F8-101010",
            "-1|-8|0DC1FC-101010,15|-1|0DBDF7-101010,15|-9|0DC1FD-101010,7|-6|0D2535-101010",
            0, 0.9, "raidLocked"
        )

    // Somebody else is attacking this district (variant 1)
    override fun raidOccupied(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema =
        ColorSchema.parse(
            x1, y1, x2, y2, "0D56A9-101010",
            "1|1|0F53A6-101010,2|2|0D50A3-101010,3|3|0D4B9E-101010,4|4|0D4B9F-101010,5|5|0D4A99-101010,7|1|F6D295-101010,8|2|F5D28C-101010",
            0, 0.9, "raidOccupied"
        )

    // Somebody else is attacking this district (variant 2)
    override fun raidOccupied2(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema =
        ColorSchema.parse(
            x1, y1, x2, y2, "0D51A4-101010",
            "1|-1|0D55A7-101010,2|-2|0D56A8-101010,3|-3|0D54AA-101010,5|-5|0D5AAD-101010,7|-7|125EB1-101010,-1|-4|FCD7A4-101010,0|-5|FDD998-101010",
            0, 0.9, "raidOccupied2"
        )

    // "要立即进攻吗？"确认弹窗：主点 = 底部横幅纯绿 (200,565) RGB(35,122,76)，
    // 偏移含横幅多点 + 右上大红X (1150,110) RGB(215,16,20)。弹窗居中固定，已真机截图验证命中。
    override val CapitalAttackConfirm = ColorSchema.parse(
        0, 0, 1280, 720, "4C7A23",
        "450|0|4C7A23,700|25|4C7A23,920|47|4C7A23,950|-455|1410D7",
        0, 0.9, "CapitalAttackConfirm"
    )
    // 确认弹窗"进攻"按钮可用（部队已满）：主点 = 按钮亮绿 (1120,405) RGB(215,243,127)，
    // 深绿下缘 (1080,420) RGB(132,205,44)。
    override val CapitalAttackConfirmReady = ColorSchema.parse(
        950, 300, 1280, 500, "7FF3D7",
        "-40|15|2CCD84",
        0, 0.88, "CapitalAttackConfirmReady"
    )
    // 确认弹窗"进攻"按钮禁用（部队未满）：主点 = 按钮灰 (1120,405) RGB(144,144,144)，
    // 深色下缘 (1080,420) RGB(36,34,32)。
    override val CapitalAttackConfirmGray = ColorSchema.parse(
        950, 300, 1280, 500, "909090",
        "-40|15|202224",
        0, 0.88, "CapitalAttackConfirmGray"
    )

    // 都城地图：选中子城后底部弹出的白色按钮条（侦察绿/进攻/详细信息蓝）。
    // 主点 = 面板左侧 (300,620) RGB(221,221,213)；偏移含面板中部、右端与蓝色详细信息按钮
    // (850,630) RGB(107,164,250)。真机截图 district_tapped.png 标定。
    override val CapitalDistrictBar = ColorSchema.parse(
        250, 560, 1050, 720, "D5DDDD",
        "200|10|BCC3C3,680|0|D5DDDD,550|10|FAA46B",
        0, 0.88, "CapitalDistrictBar"
    )

    // 战斗结算界面的绿色"地图"回城按钮（源脚本"回营主"）：
    // 主点 (579,597) RGB(141,209,64)，偏移 (624,597) RGB(141,209,64)、(624,585) 浅色高亮 RGB(185,233,131)、
    // (624,609) RGB(139,214,59)。2026-09-27 由用户结算截图（72% 摧毁率）标定。颜色串为 BGR。
    override val CapitalResultMapButton = ColorSchema.parse(
        500, 540, 760, 680, "40D18D-181818",
        "45|0|40D18D-181818,0|-12|B9E983-202020,0|12|3BD68B-181818",
        0, 0.88, "CapitalResultMapButton"
    )
    // 按钮条中的"进攻"按钮红色（该子城可进攻）：主点 (760,620) RGB(255,133,135)（红按钮主体亮红），
    // 偏移 (720,615) RGB(255,143,145) 与 (800,615) RGB(255,134,138) 同为按钮主体。
    // 2026-09-27 由实机截图标定：红按钮态三点全亮红；灰按钮态同三点为 170~177 均匀灰，区分度完美。
    // 注意颜色串为 BGR。
    override val CapitalDistrictAttackRed = ColorSchema.parse(
        600, 560, 900, 720, "8785FF-181818",
        "-40|-5|918FFF-181818,40|-5|8A86FF-181818",
        0, 0.9, "CapitalDistrictAttackRed"
    )
    // 按钮条中的"进攻"按钮灰色（该子城已三星/不可进攻）：主点 (750,640) RGB(191,191,191)，
    // 深灰文字 (700,630) RGB(92,92,92)。真机截图 district_tapped.png 标定。
    // 2026-09-27 踩坑：该特征在红色可用按钮上也会误命中（命中点落在按钮上方浅灰带上），
    // 不可再用作"不可打"判据，保留仅供日志参考；判据请用 CapitalDistrictAttackRed。
    override val CapitalDistrictAttackGray = ColorSchema.parse(
        600, 560, 900, 720, "BFBFBF",
        "-50|-10|5C5C5C",
        0, 0.88, "CapitalDistrictAttackGray"
    )

    // 都城地图：选中子城后底部弹出的红色"进攻"按钮条。
    // 源脚本 L47307 cmpColorEx("66|712|1D1DF5,95|712|8585FD,65|793|201DF5,97|793|8585FD,63|753|1F1DF4,98|756|8585FD", 0.9)
    // 按映射A (dx,dy)->(dy,-dx) 从竖屏主点 (66,712)->横屏 (712,653) 映射；源项目已稳定运行。
    override val CapitalBottomAttackBar = ColorSchema.parse(
        650, 580, 850, 700, "1D1DF5",
        "0|-29|8585FD,81|1|201DF5,81|-31|8585FD,41|3|1F1DF4,44|-32|8585FD",
        0, 0.9, "CapitalBottomAttackBar"
    )

    // Raid map boat (waiting for it to appear = the capital map is open)
    override val CapitalRaidMapBoat = ColorSchema.parse(
        20, 568, 149, 695, "2D3891-101010",
        "6|-5|303C90-101010,11|-5|2C3D8F-101010,28|-5|2F378D-101010,41|0|2E3588-101010,12|-36|4F6EF0-101010,20|-37|4E6FF2-101010,48|-43|4A69E6-101010,38|-17|223E55-101010,60|53|4270E0-101010",
        0, 0.9, "CapitalRaidMapBoat"
    )

    // Building list panel (used to tell whether the panel is already open)
    override val CapitalBuildingListMain = ColorSchema.parse(
        503, 85, 806, 113, "FFFFFF-101010",
        "14|0|FFFFFF-101010,29|0|FFFFFF-101010,46|0|FFFFFF-101010,117|0|FFFFFF-101010,127|0|FFFFFF-101010,137|0|FFFFFF-101010,154|0|FFFFFF-101010",
        0, 0.9, "CapitalBuildingListMain"
    )

    // 都城村庄主界面顶部"部落都城"标签：深色文字+浅蓝底+羊皮纸图标，只在都城主视图出现。
    // 由 zoom2_base.png 标定：文字@(540,14) RGB(30,36,39)，标签底@(540,16) RGB(184,232,255)，
    // 羊皮纸@(545,19) RGB(220,220,203)。
    override val CapitalVillageLabel = ColorSchema.parse(
        525, 10, 570, 25, "27241E",
        "5|0|27241E,0|2|FFE8B8,8|2|FFE9AD,5|5|CBDCDC",
        0, 0.88, "都城村庄标签"
    )

    // 都城村庄主界面右上角"都城币"图标：外圈金色六边形、中间红色六边形（中心品红 RGB(182,24,224)）。
    // 由 zoom2_base.png 标定：品红中心@(1235,40) RGB(182,24,224)，金外框@(1245,27) RGB(246,166,15)、
    // @(1248,30) RGB(245,184,16)、底部@(1235,55) RGB(213,121,21)。
    // 验证：zoom2/zoom 序列/state4/battle_start3/entry_fail2 等村庄帧全部命中；主世界 zoom3 与
    // 突袭地图（capital_enter 及 run 录屏 28 帧 boat 帧）全部无命中——仅出现在都城村庄视图，
    // 不出现在主世界/夜世界/突袭地图层，可作为与左侧军队入口图标并列的都城判定依据。
    // 注意：颜色串按 BGR 写（ColorSchema.parseBgrToRgb 按 0xBBGGRR 解析）。
    override val CapitalGoldIcon = ColorSchema.parse(
        1215, 25, 1255, 62, "E018B6",
        "-5|-2|E315BA,5|2|DE18B3,0|5|DF16B4,10|-13|0FA6F6,13|-10|10B8F5,0|15|1579D5",
        0, 0.88, "都城币图标"
    )
}
