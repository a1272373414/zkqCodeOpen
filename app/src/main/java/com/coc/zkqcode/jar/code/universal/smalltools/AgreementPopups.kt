package com.coc.zkqcode.jar.code.universal.smalltools

import android.graphics.Bitmap
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import kotlinx.coroutines.delay

/**
 * 协议弹窗处理（T05，源 函数10a / 函数301a / 函数316a）。
 *
 * 首次启动游戏（或切号重进）时，腾讯/谷歌版会弹出协议弹窗，bot 若卡在这里会一直停住。
 * 这里用 [ChineseTextReader.locate] 按中文文字定位按钮——与屏幕方向无关，比像素坐标更稳，
 * 也正好复用 T35 的文字按钮定位能力。
 *
 * 源脚本逻辑：
 *  - 函数301a（谷歌）：版本==谷歌 时找蓝色「全部接受」按钮点击；
 *  - 函数10a（腾讯）：找协议窗 → 点「同意」→ 滑动后再点「同意」（双确认）→ 点 QQ 登录；
 *  - 函数316a：绿色「同意腾讯协议」/「检查更新」按钮。
 */
object AgreementPopups {

    /**
     * 处理当前界面上的协议/登录类弹窗，最多轮询 [maxRounds] 轮避免死循环。
     *
     * 处理项：
     *  1. 谷歌版「全部接受」；
     *  2. 腾讯版「同意」（双确认：先点一次 → 下滑协议 → 再点一次）；
     *  3. 「QQ登录」/「登录」入口；
     *  4. 顺带点掉可能的「检查更新」。
     *
     * @return 是否处理过任意弹窗
     */
    suspend fun handleAgreementPopups(maxRounds: Int = 8): Boolean {
        // 取一次屏幕尺寸，用于计算「下滑协议」的 swipe 坐标（竖屏/横屏都按比例算，方向无关）
        val bmp = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap
        val (sw, sh) = if (bmp != null) bmp.width to bmp.height else 720 to 1280

        var handled = false
        repeat(maxRounds) {
            // 谷歌版：全部接受
            ChineseTextReader.locate("全部接受")?.let {
                TouchActions.tap(it.centerX(), it.centerY())
                handled = true
                delay(1200)
            }
            // 腾讯版：同意（双确认第 1 次）
            ChineseTextReader.locate("同意")?.let {
                TouchActions.tap(it.centerX(), it.centerY())
                handled = true
                delay(1000)
                // 下滑协议文本，露出第二个「同意」（源 swipe(10,1038,460,1038)）
                TouchActions.swipe(sw / 2, sh * 3 / 4, sw / 2, sh / 4, delayTime = 60)
                delay(800)
            }
            // 腾讯版：同意（双确认第 2 次）
            ChineseTextReader.locate("同意")?.let {
                TouchActions.tap(it.centerX(), it.centerY())
                handled = true
                delay(1200)
            }
            // 登录入口
            ChineseTextReader.locate("QQ登录")?.let {
                TouchActions.tap(it.centerX(), it.centerY())
                handled = true
                delay(1500)
            }
            ChineseTextReader.locate("登录")?.let {
                TouchActions.tap(it.centerX(), it.centerY())
                handled = true
                delay(1500)
            }
            // 可能的「检查更新」
            ChineseTextReader.locate("检查更新")?.let {
                TouchActions.tap(it.centerX(), it.centerY())
                handled = true
                delay(1500)
            }
            // 本轮没处理到任何弹窗，说明界面已无协议层，提前结束
            if (!handled) return@repeat
        }
        return handled
    }
}
