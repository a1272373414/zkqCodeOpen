@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Clan capital BATTLE deploy features, migrated from the legacy script
 * (`awcocx_main.lua` 特征: 都城下兵_* / 残血 / 偷袭法术下兵点 / 偷袭冰冻法术下兵点),
 * with the same 90° portrait->landscape mapping as the rest of the project.
 *
 * - `CapitalDeploy*`      : pick a unit/spell in the capital battle deploy bar.
 * - `CapitalLowHpTargets` : low-HP enemy markers; the first hit is used as the deploy point.
 * - `CapitalSneak*Points` : splash points for the sneak spell / freeze spell.
 *
 * Regenerate with tools/gen_capital_deploy_colors.py.
 */
interface ICapitalDeployColors {
    val CapitalDeploySuperMiner: ColorSchema
    val CapitalDeployHogRaider: ColorSchema
    val CapitalDeploySuperGiant: ColorSchema
    val CapitalDeploySuperWizard: ColorSchema
    val CapitalDeploySneakyArcher: ColorSchema
    val CapitalDeploySuperBarbarian: ColorSchema
    val CapitalDeployBattleRam: ColorSchema
    val CapitalDeploySkeletonSpell: ColorSchema
    val CapitalDeployHealingSpell: ColorSchema
    val CapitalDeployLightningSpell: ColorSchema
    val CapitalDeployJumpSpell: ColorSchema
    val CapitalDeployFreezeSpell: ColorSchema
    val CapitalLowHpTargets: List<ColorSchema>
    val CapitalSneakSpellPoints: List<ColorSchema>
    val CapitalSneakFreezePoints: List<ColorSchema>
}

object CapitalDeployColors : ICapitalDeployColors {
    override val CapitalDeploySuperMiner = ColorSchema.parse(
        28, 584, 1220, 715, "7C9FEB",
        "0|7|174B79,-7|8|325C88,23|12|4A679E,28|6|5772A7,3|-17|3D9CFA,-8|-21|3AB4FF,28|33|44B0FF,4|-18|3998F6",
        0, 0.9, "CapitalDeploySuperMiner"
    )
    override val CapitalDeployHogRaider = ColorSchema.parse(
        35, 586, 1249, 712, "365199",
        "5|0|6683D0,15|5|0C1C2C,20|5|0A1D30,30|8|0F1F37,8|36|141C70,3|22|141830,12|-16|E8C496,8|-32|E0B887,-30|-8|80A2E8,33|-35|485660,-33|-36|D29C59",
        0, 0.9, "CapitalDeployHogRaider"
    )
    override val CapitalDeploySuperGiant = ColorSchema.parse(
        75, 586, 1177, 709, "A0C5FF",
        "8|-8|127AFE,-5|-8|228AFB,-14|-20|78ACFF,-19|-38|169FFF,-2|-37|1053B5,-45|-37|D09A5A,25|7|183DA2",
        0, 0.9, "CapitalDeploySuperGiant"
    )
    override val CapitalDeploySuperWizard = ColorSchema.parse(
        75, 586, 1177, 709, "111835",
        "15|-7|70FFFF,-8|-16|70FFFF,-37|4|114CD0,29|35|3750C4,23|16|204FB6,5|38|182F5D,-37|-29|D6A064,10|-21|485178",
        0, 0.9, "CapitalDeploySuperWizard"
    )
    override val CapitalDeploySneakyArcher = ColorSchema.parse(
        75, 586, 1177, 709, "38C0A2",
        "-4|-1|38B89E,7|-26|90B1F8,21|-48|D2A066,4|-48|C15968,-42|-43|C05667,-33|-51|D8A870,-2|-34|7B4CDF",
        0, 0.9, "CapitalDeploySneakyArcher"
    )
    override val CapitalDeploySuperBarbarian = ColorSchema.parse(
        75, 586, 1177, 709, "58B2EC",
        "15|-17|34F1F0,15|33|5464AC,23|18|505D85,26|13|686468,3|-31|40A2E4,-30|-10|7CB4F0,-39|-29|D49E60,30|-4|D09A66",
        0, 0.9, "CapitalDeploySuperBarbarian"
    )
    override val CapitalDeployBattleRam = ColorSchema.parse(
        75, 586, 1177, 709, "50CBFF",
        "24|15|30343C,18|3|404C5E,29|-8|5A6068,0|-17|3D566D,-17|-16|304B61,-28|-30|D09C5A,43|-33|D09958,5|34|88C5FF",
        0, 0.9, "CapitalDeployBattleRam"
    )
    override val CapitalDeploySkeletonSpell = ColorSchema.parse(
        35, 586, 1249, 712, "908480",
        "0|-10|E8BCB8,-21|-11|806B68,-33|-32|AB5B58,12|-15|D1ACA8,25|-28|935D5D,19|27|504848,-1|16|282320,-6|16|28231E,-3|23|282220,17|16|288CC9",
        0, 0.9, "CapitalDeploySkeletonSpell"
    )
    override val CapitalDeployHealingSpell = ColorSchema.parse(
        75, 586, 1177, 709, "71CCF8",
        "12|15|205968,23|14|1F8CD0,27|19|24ACE9,-1|-2|73CCF8,4|-12|E0F4F8,-32|-19|58C6E0,4|-32|304E60",
        0, 0.9, "CapitalDeployHealingSpell"
    )
    override val CapitalDeployLightningSpell = ColorSchema.parse(
        75, 586, 1177, 709, "BE6730",
        "-1|-19|207385,-1|15|BA6430,-1|-31|2D4653,38|-30|FFFFFF,-38|-41|FFFFFF,-27|-45|D1716A,4|24|207080",
        0, 0.9, "CapitalDeployLightningSpell"
    )
    override val CapitalDeployJumpSpell = ColorSchema.parse(
        75, 586, 1177, 709, "43B070-151515",
        "0|-6|44B271-151515,-3|-13|227488-151515,2|-12|227387-151515,13|-15|17AAD1-151515,1|-31|75B5FF-151515,31|-14|44F6D3-151515,-25|-56|CD4E67-151515",
        0, 0.9, "CapitalDeployJumpSpell"
    )
    override val CapitalDeployFreezeSpell = ColorSchema.parse(
        28, 584, 1220, 715, "EBC9B5",
        "13|-5|F4DBCA,19|26|389AD7,8|26|3D748C,19|35|D7B9B1,22|29|D4B5AB,13|-6|F4DBCA,0|-20|697F94,-15|-19|F1BDAE",
        0, 0.9, "CapitalDeployFreezeSpell"
    )
    override val CapitalLowHpTargets = listOf(
        ColorSchema.parse(
            169, 223, 1207, 577, "0015f2",
            "1|0|0014ef,2|0|0012ed,3|0|0110fa,1|1|00121b,2|1|00111a,3|1|000f18,6|1|000c12,7|1|000a10,9|1|00070b,11|1|000507",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "0002d4",
            "1|0|0002d4,2|0|0002d4,3|0|0001d4,4|1|0c0d0e,6|1|0a0f0d,7|1|060b0f,8|1|0f1214,9|1|060e19,10|1|0a1921,11|1|091821",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "645ef1",
            "1|0|6160e7,2|0|5c5be0,3|0|5554d6,4|0|5253d5,0|1|0813d5,1|1|141fc8,2|1|141fc2,3|1|0f1bbc,4|1|0d19b9",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "4e5bf6",
            "1|0|4e5bf6,2|0|4e5bf6,3|0|4e5af6,4|0|4d5af6,0|1|0215cb,1|1|0315cb,2|1|0215cb,3|1|0215cb,4|1|0215cb",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "4ae799",
            "1|0|4ae697,2|0|4ae697,3|0|4be598,4|0|4be599,0|-1|01070e,1|-1|01070e,3|-1|020711,4|-1|030714,5|-1|030817",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "10c75f",
            "1|0|10c660,2|0|11c760,3|0|10c660,5|0|13ca62,6|0|13ca62,0|1|050b0a,1|1|050b06,2|1|030709,4|1|030c06,5|1|030c06,7|1|030c06",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "78ddb2",
            "1|0|7adeb3,2|0|7adeb3,3|0|7adeb3,4|0|7adeb3,0|-1|000000,1|-1|000000,3|-1|000000,4|-1|000000,5|-1|000000,6|-1|000000,9|-1|000000",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "7ee9ad",
            "1|0|7ee9ad,2|0|7ee9ad,3|0|7ee9ad,4|0|7ee9ad,5|0|7ee9ad,6|0|7ee9ad,0|1|11d767,1|1|11d566,2|1|11d566,3|1|11d566,4|1|11d566,5|1|11d566,6|1|11d566",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "24297a",
            "1|0|1119a6,2|0|030fc8,3|0|020fcb,1|1|0b141a,2|1|09121a,3|1|0a141c,4|1|0c0f19,5|1|0d111e,6|1|151d29,7|1|192229,10|1|161a24",
            4, 0.9, "CapitalLowHpTargets"
        ),
        ColorSchema.parse(
            169, 223, 1207, 577, "378e6e",
            "1|0|2e8767,2|0|2a8d6b,3|0|20956a,4|0|1ba772,0|1|171819,1|1|181b1c,2|1|1a2121,3|1|151a1c,4|1|0c100f,5|1|050806,6|1|010403,7|1|010301,8|1|010301",
            4, 0.9, "CapitalLowHpTargets"
        ),
    )
    override val CapitalSneakSpellPoints = listOf(
        ColorSchema.parse(
            169, 4, 1273, 586, "24269D",
            "2|-3|4749AF,16|2|166486,6|24|19B1DA,0|-11|189CC9,-1|-15|282ABE,-1|1|22259A,-10|16|1B7ABE,19|15|158ACE",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            186, 90, 1091, 566, "1AABF1-151515",
            "0|-1|1AA9F1-151515,-1|-7|4242BB-151515,-5|-7|5157DB-151515,-6|-7|4F56D7-151515,-3|-1|168ACE-151515,-8|0|189DE4-151515,-3|-8|6375FF-151515",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "28496E",
            "-12|1|274569,-18|0|2E2722,-16|-18|675255,-9|-19|2F2620,-16|-24|3F332F,-17|-32|1837B5,-3|-19|AB8E96,-1|-11|6C9ED7",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1A91DD",
            "8|-2|218AD9,15|-9|1888D1,-14|-7|1A84CF,-18|-12|12435E,-10|-12|3F4AC4,16|-12|1696D7",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "7C6B73",
            "0|-4|8A737B,13|-3|3F3FC7,14|-12|3E40D7,13|11|71A4E1,12|24|575052,-2|19|322C28",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "302A2A",
            "-5|-5|403530,-9|-9|585355,11|16|3F5478,12|12|384F76,19|3|18A0D0,-28|0|3B4F73,-19|10|403430",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1A91DD",
            "8|-2|218AD9,15|-9|1888D1,-14|-7|1A84CF,-18|-12|12435E,-10|-12|3F4AC4,16|-12|1696D7",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "7C6B73",
            "0|-4|8A737B,13|-3|3F3FC7,14|-12|3E40D7,13|11|71A4E1,12|24|575052,-2|19|322C28",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "20A5FB",
            "-10|11|10679F,-17|11|0F587A,-30|1|127FB0,-22|-8|1F3CBF,-8|-9|223CBD",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1CB8FA",
            "-29|2|169BDD,-22|-7|1D37BA,-8|-8|2541CC,-9|-15|1B81C8,-18|-11|0A465C,-13|-14|4449EF",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "403539",
            "-12|-3|2E2B27,8|-9|51525B,7|-16|7CB8F7,-13|-14|2D425D,-15|-18|152333,0|-29|8B7882,-10|-30|99838C,-6|-40|1C36AC",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "665F60",
            "12|-5|6C6468,5|-5|383838,-1|0|655C60,8|10|305480,17|2|305481,15|18|807880,-14|9|617F97",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "48B1FF",
            "0|-3|3475B8,1|-10|40A4FF,7|-5|4098FB,6|-8|47A5FF,20|-2|30598C,-5|18|52453E,16|16|BFB1B8",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "20C0FF",
            "-16|-9|287DBA,2|-14|203CC4,12|-7|147FC6,3|2|106498,-17|-10|2596E1,2|-13|253DC1",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "302820",
            "-3|6|281E18,-1|6|423D78,-7|14|303367,-15|9|303468,-17|-1|1394D8,-8|-3|508BC6",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1C2030",
            "7|-1|516199,-8|3|415599,-7|-2|3DFFFF,-3|10|44FFFF,8|4|4DFFFF,0|-22|0E14BE",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "2C2623",
            "11|5|585257,21|-1|2D5483,13|-1|272760,14|-9|618ABA,20|0|233F5C,7|1|284C70,13|-2|393CAE,18|-10|79ADEB",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "373A97-202020",
            "-4|-3|4142A6-202020,1|1|373A98-202020,-2|-3|4144A8-202020,-4|5|166595-202020,-3|4|15679B-202020,1|7|6892BA-202020,2|7|6B90B9-202020",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "2D2D6A-202020",
            "1|-1|2B2B66-202020,-7|-3|1DBCFF-202020,-7|-4|1EC9FF-202020,-8|0|147BBB-202020,-10|-5|3C3DF3-202020",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1DBCFF-202020",
            "0|-1|1EC9FF-202020,0|0|1DBCFF-202020,0|2|1587BE-202020,-2|3|1478B6-202020,-3|-2|3C3DF3-202020,-3|-1|3C3DDE-202020",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "70A0DD-202020",
            "-3|0|5C86BC-202020,-5|1|679CDA-202020,3|-1|71A3E1-202020,-3|2|6895CB-202020,-5|14|555ADB-202020,-1|13|4343CF-202020,-7|15|3535C2-202020",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "7FB6F5-202020",
            "-2|0|7BAEF2-202020,-6|1|6AA0E0-202020,2|-1|8BC6FF-202020,4|-3|7FB6FB-202020,-3|7|3D3EB2-202020,-4|10|383BAA-202020",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "2F398B-202020",
            "1|-8|2C4482-202020,-3|1|3FFFFF-202020,-7|-2|4DFFFF-202020,-1|-6|32E4FF-202020",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "41FFFF-202020",
            "0|1|43FFFF-202020,3|12|47FFFF-202020,3|8|29CCFF-202020,3|6|26C3FF-202020,15|5|3FFFFF-202020",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            250, 159, 486, 307, "4F55D8-151515",
            "-1|0|5056D8-151515,1|4|168BCF-151515,5|0|4E5ACB-151515,-2|-4|1BB1FF-151515,-4|7|179CE0-151515,1|5|158CCF-151515,0|0|4F55D8-151515",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "19A2D1-101010",
            "-7|11|3D5178-101010,16|-2|253655-101010,-23|-6|4AA5FF-101010,-7|-15|327CD5-101010,-5|-12|4A6B9C-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "1797C8-101015",
            "-1|1|189ACE-101015,-3|9|3B517B-101015,-6|12|3B5178-101015,-8|14|3A4F76-101015,-20|17|483F3F-101015,-14|14|483F3F-101015,-35|7|3D3330-101015,-39|4|3E3431-101015,-43|-2|374C72-101015",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "818B98-101010",
            "2|-3|8C94A2-101010,0|-3|848E98-101010,-1|-12|225CBF-101010,0|-13|2666CE-101010,0|-10|77A5EF-101010,0|-11|1EB5FE-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "7F8591-101010",
            "3|-1|6F7378-101010,-2|-6|DAECFF-101010,1|-6|AEBCCA-101010,1|-14|3F7FEA-101010,-1|-11|689DEA-101010,-2|-16|44B7F5-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "62DFFF-101010",
            "-1|1|5CFFFF-101010,-2|9|28B9FF-101010,1|6|45F9FF-101010,0|-1|3457B5-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "2FF3FF-101015",
            "0|-1|1CF0FF-101015,-1|6|37FDFF-101015,-1|-7|38FEFE-101015,-1|-8|2FFEFE-101015,4|-1|292E39-101015,3|-1|212226-101015",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "22A4FF-101015",
            "5|3|63FFFF-101015,4|3|58FFFF-101015,6|6|20BFFF-101015,2|10|24AFFF-101015,2|11|26C8FF-101015,4|1|2E4D9D-101015",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "393DD6-101010",
            "-2|1|4242D7-101010,-5|2|4646DB-101010,-11|3|4A51DF-101010,-9|0|51545D-101010,-7|-2|51555E-101010,-2|-3|575A63-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "5B5FFF-101010",
            "2|0|80858B-101010,-2|2|646665-101010,-4|1|777671-101010,-1|-4|C2CBD2-101010,-5|-2|989591-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "71636C-101015",
            "-1|0|71636B-101015,-3|0|73656D-101015,-4|0|71636B-101015,9|13|7CB4F5-101015,7|15|6C9AD4-101015,3|16|6491CD-101015",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "3B3BD9-101015",
            "-2|2|3942DD-101015,-7|3|4444DD-101015,-12|4|464ADF-101015,-7|-1|4F535B-101015,-5|3|4346DB-101015,-10|4|4B4FDB-101015,-11|4|4A51DF-101015",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "72BCFD-101010",
            "-1|1|70BFF7-101010,0|0|72BCFD-101010,-1|1|70BFF7-101010,-4|-4|161414-101010,-5|-6|685C60-101010,-6|-5|655B5B-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "476BFF-101010",
            "-1|-1|476BFF-101010,-2|-2|476BFF-101010,-2|-3|476BFF-101010,3|1|8C7778-101010,3|2|826F75-101010,4|0|8F7988-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "2CBFEB-101010",
            "-1|0|29AED3-101010,-1|-1|2AB2D5-101010,-1|-4|687EFF-101010,-1|-3|6A80FF-101010,0|-3|6278FF-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "3E5DA8-101010",
            "-3|-2|5F6871-101010,-2|-4|595A62-101010,-5|-5|8F94A8-101010,-2|-6|70737D-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "4677B3-101010",
            "3|2|887778-101010,0|2|9B8787-101010,1|0|4075AD-101010,3|4|766D72-101010,3|3|756666-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "4B7BB8-101010",
            "-1|-1|4B7DB9-101010,-1|0|4D80C0-101010,-1|-1|4B7DB9-101010,3|-2|BAA1A0-101010,3|-2|BAA1A0-101010,3|-3|B89E9C-101010,0|-5|B39C9C-101010,-1|-5|B19996-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "4A8BAC-101010",
            "4|-4|4DBDFB-101010,-1|-8|59CCFE-101010,-6|-7|4EBEF7-101010,-2|-10|4FBDFD-101010,6|-5|54D5FE-101010,12|10|867D83-101010,9|12|6B666B-101010,0|15|C6B6BE-101010,11|12|B3A7B0-101010",
            0, 0.9, "CapitalSneakSpellPoints"
        ),
    )
    override val CapitalSneakFreezePoints = listOf(
        ColorSchema.parse(
            169, 4, 1273, 586, "24269D",
            "2|-3|4749AF,16|2|166486,6|24|19B1DA,0|-11|189CC9,-1|-15|282ABE,-1|1|22259A,-10|16|1B7ABE,19|15|158ACE",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "28496E",
            "-12|1|274569,-18|0|2E2722,-16|-18|675255,-9|-19|2F2620,-16|-24|3F332F,-17|-32|1837B5,-3|-19|AB8E96,-1|-11|6C9ED7",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1A91DD",
            "8|-2|218AD9,15|-9|1888D1,-14|-7|1A84CF,-18|-12|12435E,-10|-12|3F4AC4,16|-12|1696D7",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "7C6B73",
            "0|-4|8A737B,13|-3|3F3FC7,14|-12|3E40D7,13|11|71A4E1,12|24|575052,-2|19|322C28",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "302A2A",
            "-5|-5|403530,-9|-9|585355,11|16|3F5478,12|12|384F76,19|3|18A0D0,-28|0|3B4F73,-19|10|403430",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1A91DD",
            "8|-2|218AD9,15|-9|1888D1,-14|-7|1A84CF,-18|-12|12435E,-10|-12|3F4AC4,16|-12|1696D7",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "7C6B73",
            "0|-4|8A737B,13|-3|3F3FC7,14|-12|3E40D7,13|11|71A4E1,12|24|575052,-2|19|322C28",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "20A5FB",
            "-10|11|10679F,-17|11|0F587A,-30|1|127FB0,-22|-8|1F3CBF,-8|-9|223CBD",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1CB8FA",
            "-29|2|169BDD,-22|-7|1D37BA,-8|-8|2541CC,-9|-15|1B81C8,-18|-11|0A465C,-13|-14|4449EF",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "403539",
            "-12|-3|2E2B27,8|-9|51525B,7|-16|7CB8F7,-13|-14|2D425D,-15|-18|152333,0|-29|8B7882,-10|-30|99838C,-6|-40|1C36AC",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "20C0FF",
            "-16|-9|287DBA,2|-14|203CC4,12|-7|147FC6,3|2|106498,-17|-10|2596E1,2|-13|253DC1",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "302820",
            "-3|6|281E18,-1|6|423D78,-7|14|303367,-15|9|303468,-17|-1|1394D8,-8|-3|508BC6",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1C2030",
            "7|-1|516199,-8|3|415599,-7|-2|3DFFFF,-3|10|44FFFF,8|4|4DFFFF,0|-22|0E14BE",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "2C2623",
            "11|5|585257,21|-1|2D5483,13|-1|272760,14|-9|618ABA,20|0|233F5C,7|1|284C70,13|-2|393CAE,18|-10|79ADEB",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "373A97-202020",
            "-4|-3|4142A6-202020,1|1|373A98-202020,-2|-3|4144A8-202020,-4|5|166595-202020,-3|4|15679B-202020,1|7|6892BA-202020,2|7|6B90B9-202020",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "2D2D6A-202020",
            "1|-1|2B2B66-202020,-7|-3|1DBCFF-202020,-7|-4|1EC9FF-202020,-8|0|147BBB-202020,-10|-5|3C3DF3-202020",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "1DBCFF-202020",
            "0|-1|1EC9FF-202020,0|0|1DBCFF-202020,0|2|1587BE-202020,-2|3|1478B6-202020,-3|-2|3C3DF3-202020,-3|-1|3C3DDE-202020",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "70A0DD-202020",
            "-3|0|5C86BC-202020,-5|1|679CDA-202020,3|-1|71A3E1-202020,-3|2|6895CB-202020,-5|14|555ADB-202020,-1|13|4343CF-202020,-7|15|3535C2-202020",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "7FB6F5-202020",
            "-2|0|7BAEF2-202020,-6|1|6AA0E0-202020,2|-1|8BC6FF-202020,4|-3|7FB6FB-202020,-3|7|3D3EB2-202020,-4|10|383BAA-202020",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "2F398B-202020",
            "1|-8|2C4482-202020,-3|1|3FFFFF-202020,-7|-2|4DFFFF-202020,-1|-6|32E4FF-202020",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            169, 4, 1273, 586, "41FFFF-202020",
            "0|1|43FFFF-202020,3|12|47FFFF-202020,3|8|29CCFF-202020,3|6|26C3FF-202020,15|5|3FFFFF-202020",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            250, 159, 486, 307, "4F55D8-151515",
            "-1|0|5056D8-151515,1|4|168BCF-151515,5|0|4E5ACB-151515,-2|-4|1BB1FF-151515,-4|7|179CE0-151515,1|5|158CCF-151515,0|0|4F55D8-151515",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "19A2D1-101010",
            "-7|11|3D5178-101010,16|-2|253655-101010,-23|-6|4AA5FF-101010,-7|-15|327CD5-101010,-5|-12|4A6B9C-101010",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "1797C8-101015",
            "-1|1|189ACE-101015,-3|9|3B517B-101015,-6|12|3B5178-101015,-8|14|3A4F76-101015,-20|17|483F3F-101015,-14|14|483F3F-101015,-35|7|3D3330-101015,-39|4|3E3431-101015,-43|-2|374C72-101015",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "62DFFF-101010",
            "-1|1|5CFFFF-101010,-2|9|28B9FF-101010,1|6|45F9FF-101010,0|-1|3457B5-101010",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "2FF3FF-101015",
            "0|-1|1CF0FF-101015,-1|6|37FDFF-101015,-1|-7|38FEFE-101015,-1|-8|2FFEFE-101015,4|-1|292E39-101015,3|-1|212226-101015",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "22A4FF-101015",
            "5|3|63FFFF-101015,4|3|58FFFF-101015,6|6|20BFFF-101015,2|10|24AFFF-101015,2|11|26C8FF-101015,4|1|2E4D9D-101015",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "393DD6-101010",
            "-2|1|4242D7-101010,-5|2|4646DB-101010,-11|3|4A51DF-101010,-9|0|51545D-101010,-7|-2|51555E-101010,-2|-3|575A63-101010",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "5B5FFF-101010",
            "2|0|80858B-101010,-2|2|646665-101010,-4|1|777671-101010,-1|-4|C2CBD2-101010,-5|-2|989591-101010",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "71636C-101015",
            "-1|0|71636B-101015,-3|0|73656D-101015,-4|0|71636B-101015,9|13|7CB4F5-101015,7|15|6C9AD4-101015,3|16|6491CD-101015",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            175, 3, 1097, 581, "3B3BD9-101015",
            "-2|2|3942DD-101015,-7|3|4444DD-101015,-12|4|464ADF-101015,-7|-1|4F535B-101015,-5|3|4346DB-101015,-10|4|4B4FDB-101015,-11|4|4A51DF-101015",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "818B98-101010",
            "2|-3|8C94A2-101010,0|-3|848E98-101010,-1|-12|225CBF-101010,0|-13|2666CE-101010,0|-10|77A5EF-101010,0|-11|1EB5FE-101010",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
        ColorSchema.parse(
            174, 138, 1188, 572, "7F8591-101010",
            "3|-1|6F7378-101010,-2|-6|DAECFF-101010,1|-6|AEBCCA-101010,1|-14|3F7FEA-101010,-1|-11|689DEA-101010,-2|-16|44B7F5-101010",
            0, 0.9, "CapitalSneakFreezePoints"
        ),
    )
}
