package com.coc.zkqcode.jar.code.colorschema.colorpackage.builderbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

interface IBuilderBaseObstaclesRemovalColors {
    val CNEditBaseButton: ColorSchema
    val GlobalEditBaseButton: ColorSchema
    val GreenEditBaseButton: ColorSchema
    val EditModeRemoveAll: ColorSchema
    val EditModeRemoveAll2: ColorSchema
}

object BuilderBaseObstaclesRemovalColors : IBuilderBaseObstaclesRemovalColors {
    override val CNEditBaseButton = ColorSchema.parse(
        1050, 630, 1118, 698, "E4F7F5", "1|-3|E4F7F5,5|-6|FFFFFF,11|-28|FFFFFF,11|-32|FFFFFF,24|-8|FFFFFF,28|-8|FFFFFF,29|-6|F5FFFF,27|-11|FFFFFF,29|-11|FFFFFF", 0, 0.9, "编辑阵型按钮"
    )
    override val GlobalEditBaseButton = ColorSchema.parse(
        1196, 412, 1262, 470, "E4F7F5", "1|-3|E4F7F5,5|-6|FFFFFF,11|-28|FFFFFF,11|-32|FFFFFF,24|-8|FFFFFF,28|-8|FFFFFF,29|-6|F5FFFF,27|-11|FFFFFF,29|-11|FFFFFF", 0, 0.9, "编辑阵型按钮"
    )
    override val GreenEditBaseButton = ColorSchema.parse(
        236, 620, 425, 701, "84F8DE", "16|-1|85F8DF,40|-3|89F9E0,60|-1|85F8DF,108|6|7AF6DA,116|48|1FBB6C,87|54|1FBD70,57|49|1FBB6D,0|51|1FBC6E,-8|48|1FBB6C", 0, 0.9, "绿色编辑阵型"
    )
    override val EditModeRemoveAll = ColorSchema.parse(
        1045, 48, 1130, 82, "F9FFEC", "1|2|9DDA45,21|2|9DDA45,-5|2|DBF4BA,30|0|F9FFEC,50|0|F9FFEC", 0, 0.9, "移除全部"
    )
    override val EditModeRemoveAll2 = ColorSchema.parse(
        1045, 48, 1130, 82, "9DDA45", "-3|0|DBF4BA,23|0|9DDA45,8|-2|F9FFEC,33|-2|F9FFEC,53|-2|F9FFEC", 0, 0.9, "灰色移除全部"
    )
}
