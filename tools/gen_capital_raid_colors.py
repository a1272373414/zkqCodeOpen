# -*- coding: utf-8 -*-
"""Generate clancapital/CapitalRaidColors.kt from the legacy script's colour literals.

This is M4-③ (都城入口拆块移植). It migrates the colour features the legacy script uses for
everything EXCEPT the battle deploy itself (which is already covered by CapitalDeployColors.kt):

  B1  capital screen id     : 左下角回营 / 都城界面 (same feature, already in FeatureColors -> not re-migrated)
  B2  enter capital/map     : 都城入口 (函数319a), 都城地图小船 + 都城等级角标 (函数275a 界面表),
                              congrats panel, "new raid" prompt
  B3  pick a district       : 都城地图锚点 (函数270a) + star/lock/"someone is attacking" markers.
                              Those three are scanned with a PER-DISTRICT rect in the legacy script, so
                              only their colour patterns are generated; Kotlin wraps them onto the
                              computed rect with ColorSchema.rescope via the generated factories.
  B4  attack / return       : 军队进攻, 存储上线提示, 进攻次数已用完
  other capital features    : 发起突袭 (可用/灰色/确认), 领都城币 (铸币坊/都城币界面/收集/确认/已满/关闭),
                              捐都城币 (建筑列表, 宝库捐币点, 确认, 加号)

坐标系：源脚本是 720x1280 竖屏原始缓冲区，本项目是 1280x720 横屏，映射沿用已验证的 90° 旋转
（docs/复用优化方案20260919.md §11.3）：x' = y, y' = 719 - x, offset (dx, dy) -> (dy, -dx)。

Usage: python tools/gen_capital_raid_colors.py
"""
import os
import sys

TOOLS = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, TOOLS)
from migrate_lua_colors import (LUA_PATH, convert_offsets,  # noqa: E402
                                parse_function_275a, portrait_to_landscape_rect)

ROOT = os.path.dirname(TOOLS)
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'coc', 'zkqcode', 'jar',
                   'code', 'colorschema', 'colorpackage', 'clancapital',
                   'CapitalRaidColors.kt')

# ---------------------------------------------------------------------------------------------
# Fixed-region features: (kotlin property, comment, portrait rect, main colour, offset string,
# direction, similarity). Every literal is copied verbatim from awcocx_main.lua; the trailing
# comment keeps the legacy line number so it can be re-checked against the source.
# ---------------------------------------------------------------------------------------------
FEATURES = [
    # --- B2 enter the clan capital ---------------------------------------------------
    ('CapitalClanEntryButton', 'Capital entry button on the clan screen (函数319a, global branch)',
     (4, 158, 442, 1122), '5172FE-101010',
     '-5|-8|4B6BF1-101010|-9|-4|4872F0-101010|2|-8|4E6CFA-101010|-11|4|4772F3-101010'
     '|-6|6|4E79FE-101010|2|7|4D6EFF-101010|8|14|496DF1-101010|5|17|4974F2-101010'
     '|9|27|5073FF-101010|-13|-14|496AFC-101010|-18|-21|5075FE-101010|-23|-15|4E7CFD-101010'
     '|-25|-28|4566E1-101010',
     0, 0.9),                                                                    # lua L21824
    ('CapitalClanEntryButton2', 'Capital entry button, fallback variant',
     (4, 158, 442, 1122), '5071FF-101010',
     '-5|-7|4868EC-101010|-9|-1|4774F3-101010|-6|8|4E78FF-101010|10|15|4A6CF2-101010'
     '|9|19|4E79FE-101010|9|30|4F74FF-101010|14|23|5174FF-101010|-13|-15|5072FF-101010'
     '|-15|-11|4B6CFE-101010|-17|-21|5072FF-101010',
     0, 0.9),                                                                    # lua L21828
    ('CapitalRaidCongrats', 'Raid "congratulations" panel shown after a raid ends',
     (604, 1051, 659, 1127), '2722EE-101010',
     '1|0|2722F0-101010|10|3|8D8EF3-101010|12|2|8D8EF3-101010|12|11|FFFFFF-101010|7|15|FFFFFF-101010'
     '|-1|9|F8F9F8-101010|-7|9|2520DC-101010',
     0, 0.9),                                                                    # lua L81817
    ('CapitalRaidNewRaid', 'Brand new raid prompt (raid just started)',
     (111, 535, 336, 739), '3AD38B-101010',
     '42|3|8DEABE-101010|1|-79|3AD48B-101010|44|-78|8EEABF-101010|-1|82|3AD38A-101010'
     '|43|83|8EEABF-101010|105|-94|2F2FB9-101010|129|-92|2F2FB9-101010|107|97|2E2EB8-101010'
     '|124|97|2E2EB8-101010',
     0, 0.9),                                                                    # lua L81839

    # --- B3 pick a district ---------------------------------------------------------
    ('CapitalRaidMapAnchor', 'Raid map anchor = the 9-district grid base point (函数269a/270a intX)',
     (392, 446, 713, 537), '366C57-101010',
     '0|8|4EB075-101010|-8|18|376B58-101010|-3|26|55BD80-101010|-6|37|152E3C-101010'
     '|1|40|4FAA7B-101010|2|52|56C284-101010|13|43|757FA8-101010|12|31|727FAE-101010'
     '|11|23|727DAB-101010|13|6|6C729C-101010|27|8|4B4D7A-101010|23|20|646D9E-101010'
     '|24|33|636D9D-101010|24|43|97A8E3-101010|18|26|97A8E2-101010',
     0, 0.9),                                                                    # lua L75176

    # --- B4 attack / return ---------------------------------------------------------
    ('CapitalRaidAttackCountUsed', 'No raid attacks left (0 attack chances)',
     (143, 1163, 176, 1203), 'FFFFFF',
     '5|-2|FFFFFF|7|1|FFFFFF|12|5|FFFFFF|9|11|FFFFFF|5|9|FFFFFF|1|11|FFFFFF|-6|0|000000'
     '|-6|8|000000|-9|0|2029DC|-9|10|2029DC|2|15|2027D2|9|15|2122C4|17|6|221DBA|17|0|221DBA'
     '|9|-6|2123C7|0|-6|2128D7|7|5|000000',
     0, 0.9),                                                                    # lua L94413
    ('CapitalRaidStorageFull', '"Storage limit reached" popup (tap it away while deploying)',
     (187, 646, 319, 914), '1DBD6D',
     '67|5|89F9E0|-3|105|1FBD70|73|102|90FBE4|-1|206|1DBD6D|72|200|8EFAE3',
     0, 0.9),                                                                    # lua L94633

    # --- start a raid ---------------------------------------------------------------
    ('CapitalRaidStartButton', '"Start raid" button, clickable state',
     (22, 1133, 142, 1206), '12509A',
     '-5|-8|28C091|6|-9|63D1B1|36|7|FFF7D6|33|-1|63D8B7|36|-26|63DAB9|29|-35|28C799'
     '|-26|-34|28BB8C',
     0, 0.9),                                                                    # lua L94363
    ('CapitalRaidStartGray', '"Start raid" button, greyed out (raid already running/finished)',
     (77, 1148, 134, 1250), '676767-101010',
     '-10|11|5C5C5C-101010|-3|13|C4C4C4-101010|4|7|FCFCFC-101010|-10|37|4B4B4B-101010'
     '|-2|34|6C6C6C-101010|4|29|B9B9B9-101010|16|30|CACACA-101010|12|-8|BEBEBE-101010'
     '|12|41|BEBEBE-101010',
     0, 0.9),                                                                    # lua L94353
    ('CapitalRaidStartConfirm', '"Start raid" confirmation button inside the popup',
     (69, 514, 150, 769), '2A91FF-101010',
     '55|-1|31ADFF-101010|-8|-112|2B8AFF-101010|54|-111|32ACFF-101010|-4|115|2B8EFF-101010'
     '|57|112|34AFFF-101010|40|-90|FADAA7-101010|37|-73|F8D79F-101010',
     0, 0.9),                                                                    # lua L93515

    # --- claim capital gold ---------------------------------------------------------
    ('CapitalMintWorkshop', 'Mint workshop (capital gold building; tap it to open the coin panel)',
     (151, 500, 411, 1181), '8D97A8-101010',
     '-1|-14|8795A5-101010|16|-19|1F1D1E-101010|20|-24|398DEB-101010|27|-19|368DED-101010'
     '|34|-27|8D7E85-101010|43|-25|378FEE-101010|39|-17|3891EE-101010|33|-3|40ABFC-101010',
     0, 0.9),                                                                    # lua L21846
    ('CapitalMintWorkshop2', 'Mint workshop, fallback variant (front view)',
     (151, 500, 411, 1181), '2F3330-101010',
     '13|-4|378EEE-101010|11|1|3990EE-101010|4|11|1F1F20-101010|-7|16|82909E-101010'
     '|-15|16|8C98AB-101010|-16|33|98A5B8-101010',
     0, 0.9),                                                                    # lua L21850
    ('CapitalCoinScreen', 'Capital coin panel is open (checked after tapping the mint)',
     (580, 1114, 669, 1197), '2621E8-101010',
     '31|4|8D8FF3-101010|-1|40|2621E5-101010|30|39|8D8FF3-101010|6|13|FDF9FD-101010'
     '|26|15|FFFFFF-101010|7|27|F9F9F9-101010|28|28|FFFFFF-101010',
     0, 0.9),                                                                    # lua L93061
    ('CapitalCoinFull', 'Capital gold storage is full (stop collecting)',
     (124, 939, 177, 957), '0098F2',
     '5|2|00ADFA|6|8|E0E8E8|10|2|00BCFD|10|7|E0E8E8|17|2|1ACCFF|20|8|E0E8E8|26|0|59DEFF'
     '|30|4|E0E8E8',
     0, 0.9),                                                                    # lua L93079
    ('CapitalCollectArrow', 'Collect arrow inside the mint (tap slightly left of it)',
     (284, 168, 473, 288), '0499FF-101010',
     '0|41|0896FF-101010|-3|18|00A0FF-101010|15|-7|20A4FF-101010|14|17|1FBBFF-101010'
     '|14|43|24A4FF-101010|11|-20|000000-101010|28|-20|000000-101010',
     0, 0.9),                                                                    # lua L93039
    ('CapitalCollectButton', 'Capital gold collect button (one per district)',
     (248, 115, 350, 1170), '0DADEE-101010',
     '7|1|C5148E-101010|15|-3|DD16B5-101010|15|3|DD19B5-101010|9|13|0D99E5-101010'
     '|1|-16|38D188-101010|1|16|36D187-101010|26|15|36D28B-101010',
     0, 0.9),                                                                    # lua L93109
    ('CapitalCollectConfirm', 'Capital gold collect confirmation (blue button)',
     (498, 613, 525, 676), '1C1DFD',
     '0|1|1C1DFD|-2|0|020205|2|1|020205|-3|-2|2728F6|2|-2|2729F6|-3|3|1F20FC|2|3|1F20FC'
     '|-7|0|000000|-7|2|000000',
     0, 0.9),                                                                    # lua L93119
    ('CapitalCoinSecondPage', 'Capital coin panel, second page (after a right swipe, TH>11)',
     (348, 360, 500, 1178), '01172D',
     '20|0|01172D|15|-4|01172D|17|5|01172D|27|-25|00C1F5|26|25|01B7F0|-5|19|01AFEC'
     '|-6|-17|01B3EF|-7|-22|0A87D4|-6|27|0184D3',
     0, 0.9),                                                                    # lua L93145

    # --- donate capital gold --------------------------------------------------------
    ('CapitalDonatePoint', 'Vault donate point (green plus)',
     (226, 688, 617, 832), '0DC58D-101010',
     '4|0|0DE5B5-101010|10|0|9DFFED-101010|-1|14|15C38D-101010|3|14|0DDDAD-101010'
     '|9|14|A7FFF4-101010',
     2, 0.9),                                                                    # lua L74616
    ('CapitalDonatePoint2', 'Vault donate point, fallback variant 1',
     (223, 450, 619, 554), '8D857D-101010',
     '0|2|8D857C-101010|-1|12|857D72-101010|-1|14|857D6F-101010|4|14|0D5BAD-101010'
     '|9|1|0D6BBC-101010|15|6|0D55A5-101010|12|18|0D56A5-101010',
     2, 0.9),                                                                    # lua L74620
    ('CapitalDonatePoint3', 'Vault donate point, fallback variant 2',
     (226, 630, 621, 810), '0DAFF1-101010',
     '5|2|DD14B5-101010|7|4|DD17B5-101010|5|4|DD16B1-101010',
     2, 0.9),                                                                    # lua L74626
    ('CapitalDonatePoint4', 'Vault donate point, fallback variant 3',
     (226, 630, 621, 810), 'E018B6-101010',
     '1|1|E018B6-101010|0|1|E018B5-101010|-3|-6|0DB0F2-101010|-5|-5|0DAFF3-101010'
     '|-7|-3|0DB1F4-101010|-3|7|0D9AE5-101010',
     2, 0.9),                                                                    # lua L74632
    ('CapitalDonateConfirm', 'Donate confirmation button',
     (302, 601, 360, 680), '19157D',
     '9|4|0345B3|-8|19|191787|3|19|0538A5|-2|34|1B157F|6|34|0339A9|27|2|036DBE|20|19|017BCC'
     '|22|31|0984D2',
     0, 0.9),                                                                    # lua L74678
    ('CapitalDonatePlus', 'Donate amount "+" (legacy holds it to max out the amount)',
     (356, 561, 409, 727), 'B1B1B1',
     '-3|8|B1B1B1|-3|26|B1B1B1|-3|91|B1B1B1|-2|120|B1B1B1|1|126|B1B1B1|4|123|FFFFFF'
     '|3|112|FFFFFF|3|79|FFFFFF|4|42|FFFFFF|4|8|FFFFFF|6|1|FFFFFF|9|-2|FFFFFF',
     0, 0.9),                                                                    # lua L74696

]

# ---------------------------------------------------------------------------------------------
# Dynamic-region markers: the legacy script re-scans every district with its OWN rect, so only
# the colour pattern is generated here; Kotlin builds the schema with
# `MyColors.raidXxx(x1, y1, x2, y2)` once the district rect is known (see CapitalRaid.kt).
# (kotlin factory, comment, main colour, offset string, direction, similarity)
# ---------------------------------------------------------------------------------------------
DYNAMIC = [
    ('raidStar', 'District white star (is any star already taken here)', 'FFFFFF-101010',
     '1|1|FFFFFF-101010|2|1|FFFFFF-101010|3|2|FFFFFF-101010|5|2|FFFFFF-101010|7|2|FFFFFF-101010'
     '|4|3|FFFFFF-101010|6|3|FFFFFF-101010|8|3|FFFFFF-101010|9|5|FFFFFF-101010',
     0, 0.9),                                                                    # lua L75102
    ('raidStarCount', 'District star cluster (counted to learn how many stars are taken)', 'FFFFFF-101010',
     '10|-4|CBCBCB-101010|12|-3|FFFFFF-101010|20|6|FFFFFF-101010|12|16|FFFFFF-101010'
     '|10|17|CCCCCC-101010|1|13|FFFFFF-101010|4|6|FFFFFF-101010|14|2|FFFFFF-101010',
     0, 0.9),                                                                    # lua L75110
    ('raidLocked', 'District not unlocked yet (blue lock)', '0DC0F8-101010',
     '8|-1|0DC1FC-101010|1|15|0DBDF7-101010|9|15|0DC1FD-101010|6|7|0D2535-101010',
     0, 0.9),                                                                    # lua L75106
    ('raidOccupied', 'Somebody else is attacking this district (variant 1)', '0D56A9-101010',
     '-1|1|0F53A6-101010|-2|2|0D50A3-101010|-3|3|0D4B9E-101010|-4|4|0D4B9F-101010'
     '|-5|5|0D4A99-101010|-1|7|F6D295-101010|-2|8|F5D28C-101010',
     0, 0.9),                                                                    # lua L75118
    ('raidOccupied2', 'Somebody else is attacking this district (variant 2)', '0D51A4-101010',
     '1|1|0D55A7-101010|2|2|0D56A8-101010|3|3|0D54AA-101010|5|5|0D5AAD-101010|7|7|125EB1-101010'
     '|4|-1|FCD7A4-101010|5|0|FDD998-101010',
     0, 0.9),                                                                    # lua L75122
]

# Entries taken from the 函数275a screen lookup table: (kotlin property, screen name, comment)
TABLE_FEATURES = [
    ('CapitalRaidMapBoat', '都城地图小船',
     'Raid map boat (waiting for it to appear = the capital map is open)'),
    ('CapitalBuildingListMain', '建筑列表主',
     'Building list panel (used to tell whether the panel is already open)'),
]


def sim_literal(sim):
    return '%d.0' % int(sim) if float(sim) == int(sim) else ('%g' % sim)


def emit_static(prop, comment, feat):
    x1, y1, x2, y2 = portrait_to_landscape_rect(*feat['rect'])
    offsets = convert_offsets(feat['offsets'])
    return ('    // %s\n'
            '    override val %s = ColorSchema.parse(\n'
            '        %d, %d, %d, %d, "%s",\n'
            '        "%s",\n'
            '        %d, %s, "%s"\n'
            '    )' % (comment, prop, x1, y1, x2, y2, feat['main'], offsets,
                      feat['direction'], sim_literal(feat['similarity']), prop))


def emit_dynamic(prop, comment, feat):
    offsets = convert_offsets(feat['offsets'])
    return ('    // %s\n'
            '    override fun %s(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema =\n'
            '        ColorSchema.parse(\n'
            '            x1, y1, x2, y2, "%s",\n'
            '            "%s",\n'
            '            %d, %s, "%s"\n'
            '        )' % (comment, prop, feat['main'], offsets,
                      feat['direction'], sim_literal(feat['similarity']), prop))


def main():
    text = open(LUA_PATH, encoding='utf-8').read()

    statics = [{'rect': r, 'main': m, 'offsets': o, 'direction': d, 'similarity': s}
               for (_, _, r, m, o, d, s) in FEATURES]
    dynamics = [{'main': m, 'offsets': o, 'direction': d, 'similarity': s}
                for (_, _, m, o, d, s) in DYNAMIC]

    table_feats, _ = parse_function_275a(text)
    for _, key, _c in TABLE_FEATURES:
        if key not in table_feats:
            print('!! 函数275a 未找到界面: %s' % key, file=sys.stderr)

    members = []
    for (prop, comment, _r, _m, _o, _d, _s), feat in zip(FEATURES, statics):
        members.append(emit_static(prop, comment, feat))
    for (prop, comment, _m, _o, _d, _s), feat in zip(DYNAMIC, dynamics):
        members.append(emit_dynamic(prop, comment, feat))
    for prop, key, comment in TABLE_FEATURES:
        variants = table_feats.get(key)
        if not variants:
            continue
        members.append(emit_static(prop, comment, variants[0]))

    iface = ['    val %s: ColorSchema' % p for (p, _c, _r, _m, _o, _d, _s) in FEATURES]
    iface += ['    /** %s — the region is only known at runtime, build it with this factory. */\n'
              '    fun %s(x1: Int, y1: Int, x2: Int, y2: Int): ColorSchema' % (c, p)
              for (p, c, _m, _o, _d, _s) in DYNAMIC]
    iface += ['    val %s: ColorSchema' % p for (p, _k, _c) in TABLE_FEATURES]

    kt = '''@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Clan capital RAID / currency features, migrated from the legacy freescript
 * (`awcocx_main.lua`) with the project-wide 90° portrait->landscape mapping.
 *
 * This is the colour layer of M4-③ (都城入口拆块移植):
 *   - B2 enter capital  : [CapitalClanEntryButton], [CapitalRaidMapBoat], [CapitalLevelBadge],
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
%s
}

object CapitalRaidColors : ICapitalRaidColors {
%s
}
''' % ('\n'.join(iface), '\n\n'.join(members))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, 'w', encoding='utf-8').write(kt)
    print('wrote %s (%d static, %d dynamic, %d from 函数275a)'
          % (OUT, len(FEATURES), len(DYNAMIC), len(TABLE_FEATURES)))


if __name__ == '__main__':
    main()
