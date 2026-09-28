@file:Suppress("PropertyName")

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

    /** 源 L33304 */
    val ClanGamesTaskIcon01 = ColorSchema.parse(
        482, 110, 1082, 546, "686463",
        "3|-11|2A2A2A,26|6|747079,27|4|726E76,25|-3|222025,21|-7|3B5C81,23|-43|E8E4E8,17|-61|F2EAEC,5|-66|4D5565,-41|-3|D1915C,-41|-38|D1925C,71|4|D1935E,71|-46|D2925C",
        0, 0.9, "ClanGamesTaskIcon01"
    )

    /** 源 L33306 */
    val ClanGamesTaskIcon02 = ColorSchema.parse(
        482, 110, 1082, 546, "6DEDFF",
        "2|-4|85F1FF,4|-9|A8F9FF,7|-15|CDFFFF,14|-24|EFEDF5,21|-20|BD957D,20|-18|BA957D,14|-1|71EDFF,-8|-3|E7DDDD,-15|-20|BD957D,-9|-25|EDEDF5",
        0, 0.98, "ClanGamesTaskIcon02"
    )

    /** 源 L33309 */
    val ClanGamesTaskIcon03 = ColorSchema.parse(
        482, 110, 1082, 546, "0DC5FD",
        "5|-4|0FCEFF,8|-9|1DE2FF,15|-10|2DF3FF,8|-2|14CDFF,1|-19|55FFFF,0|-17|55FFFF,46|59|FFBD75,48|58|FFBE75,46|57|FFBF75",
        0, 0.98, "ClanGamesTaskIcon03"
    )

    /** 源 L33312 */
    val ClanGamesTaskIcon04 = ColorSchema.parse(
        482, 110, 1082, 546, "55E9FF",
        "6|-10|55FFFF,6|-8|55FFFF,6|5|0DC9FF,10|-2|16DFFF,13|-4|1FEDFF,13|13|55E6FF,21|12|55E9FF,26|8|55F5FF,28|5|55FFFF,33|18|10CDFF,39|14|1EE5FF,40|6|2EFDFF,49|-8|55FFFF,54|3|15D5FF,58|-4|29F2FF",
        0, 0.98, "ClanGamesTaskIcon04"
    )

    /** 源 L33315 */
    val ClanGamesTaskIcon05 = ColorSchema.parse(
        482, 110, 1082, 546, "75216D",
        "5|0|87217B,2|-3|94217E,0|-10|D23CA5,3|-7|C9219E,9|-10|FF21DB,4|-21|FF5AFF,5|-19|FF59FF,11|0|AF2192,18|-6|FF21DD,19|-10|FF34F5,21|-9|FF29F2,25|-14|FF2FFF,21|-24|FF4BFF",
        0, 0.98, "ClanGamesTaskIcon05"
    )

    /** 源 L33318 */
    val ClanGamesTaskIcon06 = ColorSchema.parse(
        482, 110, 1082, 546, "7D216F",
        "8|8|6B216D,3|-2|A42187,10|5|872178,10|-2|DF21AF,12|0|D621AC,13|-5|FF21D5,5|-15|FF59F5,11|-22|FF25FF,27|-2|FF59FA,32|5|FF23DD,45|6|FF2AF5,47|-4|FF34FF,49|0|FF27FF",
        0, 0.98, "ClanGamesTaskIcon06"
    )

    /** 源 L33321 */
    val ClanGamesTaskIcon07 = ColorSchema.parse(
        482, 110, 1082, 546, "D2D6D2",
        "3|0|D3D8D5,8|-1|D3D9D5,33|0|D0D5D5,33|-1|CDD5D1,38|1|D5DBD7,39|-1|D2D9D5,44|-26|D5DBD9,44|-30|CDD5D3,51|-29|D5D9D5,51|-27|D5DDD8,46|-28|D5D9D5,63|50|FFBF75,65|50|FFBF75",
        0, 0.98, "ClanGamesTaskIcon07"
    )

    /** 源 L33324 */
    val ClanGamesTaskIcon08 = ColorSchema.parse(
        482, 110, 1082, 546, "DFE4E1",
        "7|-7|D0DBE8,3|-5|268CB5,-6|-23|D3D6D4,8|-23|D5DEEB,6|-28|67E8FF,27|-14|3CBEFB,33|-23|DEE3E1,32|3|FFFFFF,63|24|ECB0B0,70|17|D1915C,-32|-55|FCB7B7,-44|-55|DEAA7F,-43|25|D2925C,69|-42|D2925C",
        0, 0.9, "ClanGamesTaskIcon08"
    )

    /** 源 L33327 */
    val ClanGamesTaskIcon09 = ColorSchema.parse(
        482, 110, 1082, 546, "3BFDFF",
        "-2|-17|5E2DA1,-12|-11|6331A1,-16|-12|FFF6FF,-22|3|5226DD,-26|7|FCE8FC,1|13|5C2A92,7|18|FDF5FE,-50|40|ECAFAF,-58|36|D1925C,50|-23|EDAFAF,56|-22|D2925C",
        0, 0.9, "ClanGamesTaskIcon09"
    )

    /** 源 L33330 */
    val ClanGamesTaskIcon10 = ColorSchema.parse(
        482, 110, 1082, 546, "CE915C",
        "1|-102|DDA97D,111|-103|DDA97D,115|3|CE9158,31|-66|8DACD3,30|-49|3F4D55,40|-49|ADD0F5,41|-47|ADD0F5,44|-43|ADD5F5,64|-33|BDC9DB",
        0, 0.95, "ClanGamesTaskIcon10"
    )

    /** 源 L33334 */
    val ClanGamesTaskIcon11 = ColorSchema.parse(
        482, 110, 1082, 546, "0D0D0D",
        "4|7|0D0D0D,5|10|0D0D0D,11|4|422B35,17|7|452E3D,22|10|36202E,16|-1|644559,25|2|614656,32|-5|7F5D6F,32|-7|7D5D75,38|-16|A66F95,36|-21|B5769A,23|-27|553546,19|-25|452B3D,-3|-17|9D698A",
        0, 0.98, "ClanGamesTaskIcon11"
    )

    /** 源 L33337 */
    val ClanGamesTaskIcon12 = ColorSchema.parse(
        482, 110, 1082, 546, "0D0D0D",
        "4|7|0D0D0D,6|8|0D0D0D,11|12|0D0D0D,19|7|4D3545,22|5|5D3F51,25|2|664C5D,22|2|65495D,20|2|65495D,16|2|654657,13|-1|654555,6|-1|482F3D,7|3|402A35,-44|6|CD915C,72|5|D0915D",
        0, 0.98, "ClanGamesTaskIcon12"
    )

    /** 源 L33341 */
    val ClanGamesTaskIcon13 = ColorSchema.parse(
        482, 110, 1082, 546, "9DA1A5",
        "-1|-18|2E6075,-1|-19|2E5F75,5|-18|1F4155,5|-16|214255,24|-5|E66ECD,27|-3|DC69C4,24|-13|FF90FD,28|-12|FF8CFB,34|-1|C84ABB,38|-3|D351C3,36|-11|FF87F5,39|-13|FF83F5,33|-19|355375,35|-20|355575,37|-21|355475",
        0, 0.98, "ClanGamesTaskIcon13"
    )

    /** 源 L33344 */
    val ClanGamesTaskIcon14 = ColorSchema.parse(
        482, 110, 1082, 546, "0DD8FF",
        "3|-13|F3D5D5,-6|-4|0DE2FF,-32|-5|0D8FD4,-31|-11|15395B,-35|14|959398,-33|10|A8B2C6,-46|-13|6F5139,-45|-29|5577A0,-67|-11|908B88",
        0, 0.98, "ClanGamesTaskIcon14"
    )

    /** 源 L33347 */
    val ClanGamesTaskIcon15 = ColorSchema.parse(
        482, 110, 1082, 546, "1D3545",
        "0|-3|1D3545,1|-5|1D3443,-5|-14|1D1B1B,-8|-30|575665,20|-21|D859CD,27|-26|554F5B,27|-16|2C282D,50|2|1D354D,59|-24|3B3438,49|-9|3D95D5,38|-12|D4E1F3,36|-26|EC83FF",
        0, 0.98, "ClanGamesTaskIcon15"
    )

    /** 源 L33350 */
    val ClanGamesTaskIcon16 = ColorSchema.parse(
        482, 110, 1082, 546, "3D48B5",
        "14|-17|3E49BA,32|-35|3F49BB,-4|-34|3E49B9,33|-3|3E49BB,26|-13|FFF1F1,6|-23|FFF1F1,-2|-47|9DDEFF,39|-48|4D6479,17|-74|FFCDCA",
        0, 0.98, "ClanGamesTaskIcon16"
    )

    /** 源 L33354 */
    val ClanGamesTaskIcon17 = ColorSchema.parse(
        482, 110, 1082, 546, "696261",
        "17|9|698198,32|2|6998C7,33|-5|719DD2,32|-11|FDEFEF,47|1|1D1C1D,51|-4|313D49,14|-60|4D2E3C,15|-63|482D3A,25|-64|5C505C,51|-53|436B95,82|-38|ECB0B0,88|-37|D2925C,-18|-42|ECB1B1,-24|-46|D1915C",
        0, 0.9, "ClanGamesTaskIcon17"
    )

    /** 源 L33357 */
    val ClanGamesTaskIcon18 = ColorSchema.parse(
        482, 110, 1082, 546, "313533",
        "3|-17|5D59E2,10|-16|5755D5,13|-24|605182,-14|-32|6160F4,-8|-39|63575A,6|-43|483D56,5|-49|6361F0,28|-33|6261ED,-45|-20|F3B9B9,-51|-20|D2945F,57|-17|F1B2B2,63|-20|D1915C",
        0, 0.9, "ClanGamesTaskIcon18"
    )

    /** 源 L33360 */
    val ClanGamesTaskIcon19 = ColorSchema.parse(
        482, 110, 1082, 546, "635FEF",
        "5|1|6461F2,11|6|2D6691,-9|7|316174,-13|-6|BBA9AB,-7|-2|BDA9A9,12|-2|BBA5A8,14|-4|B9A5A5,-17|-16|6261F1,-14|-20|B9A4A6,2|-27|6461F1,15|-22|BBA5AB,19|-17|6460F2",
        0, 0.9, "ClanGamesTaskIcon19"
    )

    /** 源 L33363 */
    val ClanGamesTaskIcon20 = ColorSchema.parse(
        482, 110, 1082, 546, "302E31",
        "36|22|313137,35|8|1D1D3F,49|-16|6ADDFF,42|-20|3D3786,22|-30|2E6DAB,17|-31|6BE4FF,44|-30|2D5D81,47|-34|6FDFFF,63|-13|1A1B3F,-21|-16|D1925C,90|-5|D2925C",
        0, 0.9, "ClanGamesTaskIcon20"
    )

    /** 源 L33366 */
    val ClanGamesTaskIcon21 = ColorSchema.parse(
        482, 110, 1082, 546, "98FFFF",
        "4|-4|B5FFFF,18|-4|A4FFFF,26|13|5EB2E1,22|7|63B0D7,-11|22|407384,38|-12|5BB5ED,48|24|469DDF,-43|22|D2925C,-42|1|D1925D,74|13|D29560",
        0, 0.9, "ClanGamesTaskIcon21"
    )

    /** 源 L33369 */
    val ClanGamesTaskIcon22 = ColorSchema.parse(
        482, 110, 1082, 546, "393535",
        "14|7|821B74,56|3|777579,74|-11|B622C3,6|-29|572648,12|-33|A126A7,14|-40|322C34,40|-51|4E5259,57|-41|9CA4B8,84|-41|EDB0B0,91|-38|D2925C,-23|-44|D2925C",
        0, 0.9, "ClanGamesTaskIcon22"
    )

    /** 源 L33373 */
    val ClanGamesTaskIcon23 = ColorSchema.parse(
        482, 110, 1082, 546, "CFE0F1",
        "-29|-20|9F9A95,25|-18|B1C1D3,-7|-26|F5E7E7,11|-31|FFF1F1,19|-38|25323E,12|-43|2D4A6D,-19|-72|2D4C73,15|-54|4775AB,-5|-66|6B92C3",
        0, 0.98, "ClanGamesTaskIcon23"
    )

    /** 源 L33376 */
    val ClanGamesTaskIcon24 = ColorSchema.parse(
        482, 110, 1082, 546, "CCDDF5",
        "0|5|C2D4E8,16|17|B5C5DD,22|22|CDE1F9,-20|16|5E81B5,25|-10|ADFFFF,21|-14|9CA0B4,52|2|C1D5ED,-43|16|D09159,69|16|D19360",
        0, 0.98, "ClanGamesTaskIcon24"
    )

    /** 源 L33379 */
    val ClanGamesTaskIcon25 = ColorSchema.parse(
        482, 110, 1082, 546, "597789",
        "21|-6|403F3E,38|28|30699C,50|17|2E6EA3,54|14|2E6DA3,56|-30|FFCACA,48|-27|504F53,38|-27|12191B,33|-23|0D1316,27|-29|182A35,21|-32|504E53,-17|-31|FCB7B7,-27|-24|D2925C,80|33|EDB2B2,88|33|D2935E",
        0, 0.9, "ClanGamesTaskIcon25"
    )

    /** 源 L33382 */
    val ClanGamesTaskIcon26 = ColorSchema.parse(
        482, 110, 1082, 546, "6585B7",
        "6|-3|3F3B3E,-8|-8|FFC370,22|-4|56A8D4,30|2|51A6D9,22|1|234059,48|-20|989DAD,-23|4|D2925C,90|0|D39763,90|-16|D2925C",
        0, 0.9, "ClanGamesTaskIcon26"
    )

    /** 源 L33385 */
    val ClanGamesTaskIcon27 = ColorSchema.parse(
        482, 110, 1082, 546, "96C3FF",
        "0|4|95C4FF,18|-31|4D4542,28|-29|6DCEFE,24|-17|365783,27|-38|514A48,19|-48|418DC1,9|-37|4D4544,3|-32|FFE3E3,25|25|84AFF8,33|30|E4C6C6,69|23|ECB1B1,75|23|D2935D,-32|-23|F3B3B3,-38|-23|D2925C",
        0, 0.9, "ClanGamesTaskIcon27"
    )

    /** 源 L33388 */
    val ClanGamesTaskIcon28 = ColorSchema.parse(
        482, 110, 1082, 546, "2E2C2F",
        "4|-2|54515A,32|23|322F32,35|14|565258,66|1|323137,57|-3|69656F,35|-6|50B0F3,36|-19|9696A4,38|-21|3F3B3C,-17|4|F3B4B4,-22|4|D1915C,85|10|EFB0B0,90|10|D2925C",
        0, 0.9, "ClanGamesTaskIcon28"
    )

    /** 源 L33392 */
    val ClanGamesTaskIcon29 = ColorSchema.parse(
        482, 110, 1082, 546, "393334",
        "34|4|3D393D,23|-34|F4E6E6,24|-41|1B235D,14|-41|302E2F,31|-41|322E2F,9|-57|E0EBFD,8|-81|DCE8F8,37|-58|D6E2F4,23|-62|6691D7,-27|-31|F0B3B3,-34|-32|D1915C,79|-21|D2925C",
        0, 0.9, "ClanGamesTaskIcon29"
    )

    /** 源 L33395 */
    val ClanGamesTaskIcon30 = ColorSchema.parse(
        482, 110, 1082, 546, "242564",
        "-1|-10|2C2B80,-6|-20|312E8A,-18|-19|343942,32|-7|2E3BE8,31|-21|3B42FD,26|-46|A96C54,17|-51|A98469,10|-55|544233,45|-19|40506F,-44|-15|D1915C,69|-19|D2925C",
        0, 0.9, "ClanGamesTaskIcon30"
    )

    /** 源 L33402 */
    val ClanGamesTaskIcon31 = ColorSchema.parse(
        482, 110, 1082, 546, "46597B",
        "15|8|40628E,27|-2|5D7AAE,35|-37|7296B5,23|-42|739DC0,9|-37|5B7D9A,2|-52|9E6248,8|-53|9A624C,18|-53|A76144,26|-51|B46E4A,-44|-29|D1915C,69|-19|D2925C",
        0, 0.9, "ClanGamesTaskIcon31"
    )

    /** 源 L33405 */
    val ClanGamesTaskIcon32 = ColorSchema.parse(
        482, 110, 1082, 546, "A64997",
        "13|3|A74D97,41|4|6A7DB1,61|2|70CAFF,80|-8|95A9F3,89|-16|9F5172,9|-41|9BC3EC,15|-48|BF51AE,83|-38|95D1FF,67|-22|7E3F5E,-13|-28|D1915C,99|-28|D2925C",
        0, 0.9, "ClanGamesTaskIcon32"
    )

    /** 源 L33410 */
    val ClanGamesTaskIcon33 = ColorSchema.parse(
        482, 110, 1082, 546, "1818BF",
        "6|-3|1C1CC1,1|-8|709EEA,38|-28|1C1CC2,36|-33|72A0EB,48|-12|6F9FE7,44|-14|1C1CC1,48|-3|1D1DC3,78|22|EDB2B2,86|16|D1925C,-20|-26|ECB1B1,-27|-28|D2925C,30|15|4C4C53",
        0, 0.9, "ClanGamesTaskIcon33"
    )

    /** 源 L33413 */
    val ClanGamesTaskIcon34 = ColorSchema.parse(
        482, 110, 1082, 546, "5C3249",
        "5|-3|7E85B7,31|1|96AAFF,35|2|5D3048,-6|-37|6C8CA0,5|-37|455C74,32|-39|95D0FF,42|-40|527692,-35|-15|F4B5B4,-40|-16|D1915C,40|-25|9B4E67,73|-24|D2925C",
        0, 0.9, "ClanGamesTaskIcon34"
    )

    /** 源 L33417 */
    val ClanGamesTaskIcon35 = ColorSchema.parse(
        482, 110, 1082, 546, "9D9D9D",
        "7|1|71645A,12|-4|14175F,28|9|89919D,27|-6|1617AB,42|0|FCFCFC,23|-16|B8C6E0,33|-19|FFF1F1,-31|-14|F4B5B4,-35|-15|D1915C,76|-15|D39663,71|-15|F0B7B7",
        0, 0.9, "ClanGamesTaskIcon35"
    )

    /** 源 L33421 */
    val ClanGamesTaskIcon36 = ColorSchema.parse(
        482, 110, 1082, 546, "C7C7DE",
        "38|30|D9E4F7,24|-1|1A1BBB,55|10|CBD8EE,36|-4|63C7FC,44|1|5DBEF8,52|-3|1A1ABF,63|-3|BDCADE,36|-21|D8E4F7,-18|13|D1915C,94|14|D2935E",
        0, 0.9, "ClanGamesTaskIcon36"
    )

    /** 源 L33424 */
    val ClanGamesTaskIcon37 = ColorSchema.parse(
        482, 110, 1082, 546, "679EC2",
        "0|-5|6273A8,9|-1|62A0CA,20|0|6CBAEF,29|0|86DFFF,28|-4|6AB6FC,29|-12|AECEFF,34|15|6FC8FF,24|13|B3CBFF,19|14|82A0E1,-5|14|5E9DC5,6|18|6D7FAF,-2|-36|496082,-3|-45|4D7B95",
        0, 0.9, "ClanGamesTaskIcon37"
    )

    /** 源 L33428 */
    val ClanGamesTaskIcon38 = ColorSchema.parse(
        482, 110, 1082, 546, "2DFFFF",
        "2|0|2DFFFF,2|-1|2DFFFF,3|-6|26D1FF,2|-7|2DD5FF,13|-8|2C8BC7,21|-1|35A6E4,21|-2|35A5E2,22|-11|31BFED,24|-10|32BFEE,33|-9|1D516D,39|-1|25C5D5,38|-8|3F3A3A,21|-19|1A191A,17|-19|232123",
        0, 0.98, "ClanGamesTaskIcon38"
    )

    /** 源 L33431 */
    val ClanGamesTaskIcon39 = ColorSchema.parse(
        482, 110, 1082, 546, "E740BF",
        "7|3|F143C8,17|9|E03ABC,39|5|F944E0,33|8|ED42CC,2|-16|FF69FF,0|-28|FF64FF,15|-9|FF76FF,19|-27|FF76FF,42|-13|FF70FF,47|-25|FF73FF,43|-49|FF6EFF,16|-20|FF8AFF,25|-20|FF79FF,33|-26|FF76FF",
        0, 0.98, "ClanGamesTaskIcon39"
    )

    /** 源 L33435 */
    val ClanGamesTaskIcon40 = ColorSchema.parse(
        482, 110, 1082, 546, "45D5FF",
        "0|-7|45D5FF,0|-13|44D5FF,-13|-37|5DE9FF,-11|-37|61EDFF,-12|-76|3DC9FF,-9|-79|41CDFF,1|-79|41D1FF,5|-79|44D5FF,16|0|8FC8FF,17|-14|85BFFF,32|-42|D0915B",
        0, 0.98, "ClanGamesTaskIcon40"
    )

    /** 源 L33438 */
    val ClanGamesTaskIcon41 = ColorSchema.parse(
        482, 110, 1082, 546, "75ACFB",
        "3|-1|75A9FD,13|-10|8DCCFF,13|-9|8DCBFF,28|3|2585FE,27|4|2A84FF,19|-25|7DB2FD,13|-30|79ADFD,19|-33|80B4FE,28|-29|85B9FF,-1|-71|A5DDFF,5|-73|ACE0FF",
        0, 0.98, "ClanGamesTaskIcon41"
    )

    /** 源 L33441 */
    val ClanGamesTaskIcon42 = ColorSchema.parse(
        482, 110, 1082, 546, "2DBD98",
        "0|2|30C09B,3|-31|7583CA,8|-26|7285D2,30|-1|2DB599,31|3|2DB195,24|-27|87A6FD,32|-37|8DA9FF,34|-38|8DA9FD,29|-54|7137CA,30|-59|7535CD,38|-55|7538D5,38|-65|7539D5,7|-64|6F36C5",
        0, 0.98, "ClanGamesTaskIcon42"
    )

    /** 源 L33451 */
    val ClanGamesTaskIcon43 = ColorSchema.parse(
        482, 110, 1082, 546, "45DCFD",
        "4|-10|57E9FF,6|-6|57E9FF,11|-2|57E9FF,-10|-29|45DBFD,-8|-38|58E9FF,-5|-34|5BE9FF,1|-40|8DFDFF,2|-39|8DFDFF,31|0|45DAFD,36|-9|5AE9FF,40|-3|57E9FF,43|-34|45D9FD,45|-29|45DBFD,53|-33|56E9FF,50|-36|55E9FF,54|-41|8DFDFF",
        0, 0.98, "ClanGamesTaskIcon43"
    )

    /** 源 L33453 */
    val ClanGamesTaskIcon44 = ColorSchema.parse(
        482, 110, 1082, 546, "97857B",
        "4|2|958175,6|0|958175,4|-4|96867B,-1|20|0D9EFC,-2|19|0DA1FA,50|-10|9D897E,53|-3|99897D,58|-1|9D8D80,57|-4|9D8F85,54|-9|9D8E85,15|-11|8D7B6D,25|13|85F1FF,22|20|5DE9FF,29|5|B8FDFF",
        0, 0.98, "ClanGamesTaskIcon44"
    )

    /** 源 L33456 */
    val ClanGamesTaskIcon45 = ColorSchema.parse(
        482, 110, 1082, 546, "47DBFD",
        "-9|-30|48DCFD,18|-48|8FFFFF,41|-27|8EFFFF,36|0|57E8FF,12|-23|D4E0ED,23|-14|D0DEEC,-40|14|825B0D,-41|-40|825B0D,75|16|825B0D,67|11|EDB2B2,75|-58|88670D,68|-56|ECB0B0",
        0, 0.94, "ClanGamesTaskIcon45"
    )

    /** 源 L33459 */
    val ClanGamesTaskIcon46 = ColorSchema.parse(
        482, 110, 1082, 546, "FFAAA4",
        "-2|14|2D1C15,4|34|664345,0|46|4B638A,8|61|435677,38|15|DBA8A5,63|18|FFD4D1,72|41|D5D5D5,71|35|4B3C3F,49|71|A28D89",
        0, 0.98, "ClanGamesTaskIcon46"
    )

    /** 源 L33462 */
    val ClanGamesTaskIcon47 = ColorSchema.parse(
        482, 110, 1082, 546, "2C3349",
        "-6|1|22293A,14|25|BBAFAF,21|20|BDB1B1,19|11|0F0E0E,18|16|3D2A2A,23|-3|1B181B,30|-3|262D41,-38|31|EFB1B1,-45|31|825B0D,58|-49|FCB7B7,69|-51|88670D,68|34|825B0D,-45|-29|825B0D",
        0, 0.9, "ClanGamesTaskIcon47"
    )

    /** 源 L33465 */
    val ClanGamesTaskIcon48 = ColorSchema.parse(
        482, 110, 1082, 546, "815A0D",
        "118|2|805A0D,113|-101|87660D,4|-101|87660D,34|-63|8AABCE,31|-48|455055,40|-49|A9CDF5,43|-45|ADD0F5,46|-41|ADD4F5,37|-67|FFE8E8",
        0, 0.95, "ClanGamesTaskIcon48"
    )

    /** 源 L33469 */
    val ClanGamesTaskIcon49 = ColorSchema.parse(
        482, 110, 1082, 546, "373D5B",
        "-7|0|B47677,-6|9|5A3A40,23|-5|26AADE,31|-14|2A32B0,31|-9|26BDEF,37|-11|1E1C22,41|-30|5A4C9F,0|-28|42316B,-26|0|EEB2B2,-32|0|815B0D,75|8|EFB1B1,80|14|825B0D",
        0, 0.94, "ClanGamesTaskIcon49"
    )

    /** 源 L33472 */
    val ClanGamesTaskIcon50 = ColorSchema.parse(
        482, 110, 1082, 546, "6BFFFF",
        "-18|-20|AC90AD,-33|-26|A59AA7,-40|-10|815F6B,-34|19|553B48,-4|20|6D5B7F,-39|-20|211B1D,-66|15|815C10,6|-50|96792C,48|0|7F5A0D",
        0, 0.98, "ClanGamesTaskIcon50"
    )

    /** 源 L33475 */
    val ClanGamesTaskIcon51 = ColorSchema.parse(
        482, 110, 1082, 546, "988AB0",
        "76|1|989CD4,44|38|8DA1DD,40|-11|12191D,27|-25|101815,44|-36|15191D,57|-21|15191D,19|-27|735955,48|-22|735955,39|39|93A2DA",
        0, 0.98, "ClanGamesTaskIcon51"
    )

    /** 源 L33478 */
    val ClanGamesTaskIcon52 = ColorSchema.parse(
        482, 110, 1082, 546, "5D484D",
        "0|7|293345,6|-2|6D5857,14|5|687A9D,-18|-8|7A6065,-19|-22|BD999D,-5|-35|B6C9F5,9|-25|8D9AC5,-6|19|557194,-9|28|91AFD5",
        0, 0.98, "ClanGamesTaskIcon52"
    )

    /** 源 L33481 */
    val ClanGamesTaskIcon53 = ColorSchema.parse(
        482, 110, 1082, 546, "553840",
        "6|-11|9D7B7F,14|-17|A17F82,23|-23|B49297,31|-26|9D7B7D,22|-30|B6C0EC,19|-4|D3999C,51|-2|E9AB96,43|5|131820,-5|12|8D819C,-41|14|825B0D,75|6|825B0D,74|29|815B0D",
        0, 0.9, "ClanGamesTaskIcon53"
    )

    /** 源 L33484 */
    val ClanGamesTaskIcon54 = ColorSchema.parse(
        482, 110, 1082, 546, "B1FFFF",
        "15|4|80737D,21|4|C0B1BB,31|11|99949B,42|12|F0BDBD,45|1|A69AA1,43|-8|B0A3AD,-18|-23|6D7099,12|-65|6292C7,3|-42|1A1415,-40|-35|F4B4B3,-47|-38|825B0D,62|-28|EDB0B0,70|-28|825B0D",
        0, 0.9, "ClanGamesTaskIcon54"
    )

    /** 源 L33488 */
    val ClanGamesTaskIcon55 = ColorSchema.parse(
        482, 110, 1082, 546, "9D6C79",
        "6|-15|2B7C44,-4|-25|745260,-2|-29|383248,27|-23|6C89CE,40|-18|293845,54|-1|71FA91,49|2|56FF9B,49|-40|6C89CC,-26|-29|825B0D,87|-14|825B0E",
        0, 0.9, "ClanGamesTaskIcon55"
    )

    /** 源 L33491 */
    val ClanGamesTaskIcon56 = ColorSchema.parse(
        482, 110, 1082, 546, "F5E2EB",
        "6|-6|714E51,-1|-8|473540,6|20|2D4776,12|15|52B6FF,33|13|221F22,27|20|A0A2A7,-33|-23|715465,-9|-37|5EB3FF,-51|1|825B0D,-51|-25|825B0D,65|-7|825B0D",
        0, 0.9, "ClanGamesTaskIcon56"
    )

    /** 源 L33496 */
    val ClanGamesHardTaskIcon01 = ColorSchema.parse(
        482, 110, 1082, 546, "95848A",
        "1|-7|3A4B5C,12|2|151519,16|7|9E848C,2|-58|904443,16|-67|A04B4C,28|-68|FFCBCA,21|-77|FF8A7C,-32|-71|FDB8B8,-42|-73|88670D,-34|8|ECAFAF,-43|8|825B0D,63|-9|EDB2B2,74|-8|825B0D,72|-70|88670D",
        0, 0.94, "ClanGamesHardTaskIcon01"
    )

    /** 源 L33499 */
    val ClanGamesHardTaskIcon02 = ColorSchema.parse(
        482, 110, 1082, 546, "585454",
        "44|32|AFAFBA,86|1|848A95,14|5|3B3431,42|22|393839,58|-8|201C1E,54|-5|262629,56|1|646C80,30|-25|6A84A3,13|-33|6D86A4,-7|-33|ECAFAF,-16|-37|825B0D,93|-18|EDB0B0,98|-17|825B0D",
        0, 0.94, "ClanGamesHardTaskIcon02"
    )

    /** 源 L33502 */
    val ClanGamesHardTaskIcon03 = ColorSchema.parse(
        482, 110, 1082, 546, "B77C7E",
        "15|-21|2D4669,19|-45|D7C1C5,46|9|928C9E,51|7|928EC0,56|-11|DBBCBA,56|-23|4084B7,52|-46|CDBDC1,55|-36|4285B8,-11|-49|FCB8B8,-21|-38|825B0D,84|28|EDB0B0,92|25|866015,-21|28|825B0D",
        0, 0.94, "ClanGamesHardTaskIcon03"
    )

    /** 源 L33505 */
    val ClanGamesHardTaskIcon04 = ColorSchema.parse(
        482, 110, 1082, 546, "8F6C6B",
        "6|-3|44323C,13|-16|555876,28|-38|95625D,34|14|4A3742,55|-1|544447,53|-9|506580,51|-12|1D1D26,48|-40|97615D,68|4|899489,-19|-28|815B0D,94|10|825B0E",
        0, 0.94, "ClanGamesHardTaskIcon04"
    )

    /** 简单任务（源 v==1 通用段，先试这一组） */
    val SIMPLE_TEMPLATES = listOf(
        ClanGamesTaskIcon01,
        ClanGamesTaskIcon02,
        ClanGamesTaskIcon03,
        ClanGamesTaskIcon04,
        ClanGamesTaskIcon05,
        ClanGamesTaskIcon06,
        ClanGamesTaskIcon07,
        ClanGamesTaskIcon08,
        ClanGamesTaskIcon09,
        ClanGamesTaskIcon10,
        ClanGamesTaskIcon11,
        ClanGamesTaskIcon12,
        ClanGamesTaskIcon13,
        ClanGamesTaskIcon14,
        ClanGamesTaskIcon15,
        ClanGamesTaskIcon16,
        ClanGamesTaskIcon17,
        ClanGamesTaskIcon18,
        ClanGamesTaskIcon19,
        ClanGamesTaskIcon20,
        ClanGamesTaskIcon21,
        ClanGamesTaskIcon22,
        ClanGamesTaskIcon23,
        ClanGamesTaskIcon24,
        ClanGamesTaskIcon25,
        ClanGamesTaskIcon26,
        ClanGamesTaskIcon27,
        ClanGamesTaskIcon28,
        ClanGamesTaskIcon29,
        ClanGamesTaskIcon30,
        ClanGamesTaskIcon31,
        ClanGamesTaskIcon32,
        ClanGamesTaskIcon33,
        ClanGamesTaskIcon34,
        ClanGamesTaskIcon35,
        ClanGamesTaskIcon36,
        ClanGamesTaskIcon37,
        ClanGamesTaskIcon38,
        ClanGamesTaskIcon39,
        ClanGamesTaskIcon40,
        ClanGamesTaskIcon41,
        ClanGamesTaskIcon42,
        ClanGamesTaskIcon43,
        ClanGamesTaskIcon44,
        ClanGamesTaskIcon45,
        ClanGamesTaskIcon46,
        ClanGamesTaskIcon47,
        ClanGamesTaskIcon48,
        ClanGamesTaskIcon49,
        ClanGamesTaskIcon50,
        ClanGamesTaskIcon51,
        ClanGamesTaskIcon52,
        ClanGamesTaskIcon53,
        ClanGamesTaskIcon54,
        ClanGamesTaskIcon55,
        ClanGamesTaskIcon56
    )

    /** 困难任务（源 v==2 追加段，简单组没命中时再试） */
    val HARD_TEMPLATES = listOf(
        ClanGamesHardTaskIcon01,
        ClanGamesHardTaskIcon02,
        ClanGamesHardTaskIcon03,
        ClanGamesHardTaskIcon04
    )

    /** 全部任务图标模板（简单 + 困难） */
    val ALL_TEMPLATES = listOf(
        ClanGamesTaskIcon01,
        ClanGamesTaskIcon02,
        ClanGamesTaskIcon03,
        ClanGamesTaskIcon04,
        ClanGamesTaskIcon05,
        ClanGamesTaskIcon06,
        ClanGamesTaskIcon07,
        ClanGamesTaskIcon08,
        ClanGamesTaskIcon09,
        ClanGamesTaskIcon10,
        ClanGamesTaskIcon11,
        ClanGamesTaskIcon12,
        ClanGamesTaskIcon13,
        ClanGamesTaskIcon14,
        ClanGamesTaskIcon15,
        ClanGamesTaskIcon16,
        ClanGamesTaskIcon17,
        ClanGamesTaskIcon18,
        ClanGamesTaskIcon19,
        ClanGamesTaskIcon20,
        ClanGamesTaskIcon21,
        ClanGamesTaskIcon22,
        ClanGamesTaskIcon23,
        ClanGamesTaskIcon24,
        ClanGamesTaskIcon25,
        ClanGamesTaskIcon26,
        ClanGamesTaskIcon27,
        ClanGamesTaskIcon28,
        ClanGamesTaskIcon29,
        ClanGamesTaskIcon30,
        ClanGamesTaskIcon31,
        ClanGamesTaskIcon32,
        ClanGamesTaskIcon33,
        ClanGamesTaskIcon34,
        ClanGamesTaskIcon35,
        ClanGamesTaskIcon36,
        ClanGamesTaskIcon37,
        ClanGamesTaskIcon38,
        ClanGamesTaskIcon39,
        ClanGamesTaskIcon40,
        ClanGamesTaskIcon41,
        ClanGamesTaskIcon42,
        ClanGamesTaskIcon43,
        ClanGamesTaskIcon44,
        ClanGamesTaskIcon45,
        ClanGamesTaskIcon46,
        ClanGamesTaskIcon47,
        ClanGamesTaskIcon48,
        ClanGamesTaskIcon49,
        ClanGamesTaskIcon50,
        ClanGamesTaskIcon51,
        ClanGamesTaskIcon52,
        ClanGamesTaskIcon53,
        ClanGamesTaskIcon54,
        ClanGamesTaskIcon55,
        ClanGamesTaskIcon56,
        ClanGamesHardTaskIcon01,
        ClanGamesHardTaskIcon02,
        ClanGamesHardTaskIcon03,
        ClanGamesHardTaskIcon04
    )

}
