package com.coc.zkqcode.core.system.daemon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.ui.floatingwindows.UIWindowService
import com.coc.zkqcode.jar.code.universal.recognizer.PixelFontChinese
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * T34 真机采集入口（广播触发版，绕开主页设置被裁剪的 UI 按钮）。
 *
 * 发送广播 `com.coc.zkqcode.HARVEST` 即触发：先切到 COC，再用 [PixelFontChinese.harvestFromScreen]
 * 以 ML Kit 当老师自动标注当前游戏界面中文、采集像素字模，并 [PixelFontChinese.saveHarvested] 写入
 * `/sdcard/zkqFiles/chinese_font.txt`。
 *
 * 调试用法（PC 端）：
 *   adb shell am broadcast -a com.coc.zkqcode.HARVEST
 */
class HarvestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.coc.zkqcode.HARVEST") return
        ShowMessage("收到 T34 采集字模广播，开始（先切到 COC）…")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    // 悬浮窗主面板会盖在游戏上被一并截进图里，先停掉 UIWindowService
                    context.stopService(Intent(context, UIWindowService::class.java))
                    // 切到 COC，确保截屏拿到的是游戏界面中文
                    val pkg = "com.tencent.tmgp.supercell.clashofclans"
                    val pm = context.packageManager
                    if (pm.getLaunchIntentForPackage(pkg) != null) {
                        val launch = pm.getLaunchIntentForPackage(pkg)!!
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launch)
                        Thread.sleep(2500)
                    }
                }
                val n = PixelFontChinese.harvestFromScreen()
                val ok = PixelFontChinese.saveHarvested()
                ShowMessage("采集字模完成：新增 ${n} 条，写入/sdcard/zkqFiles/chinese_font.txt=${ok}")
            } catch (e: Exception) {
                ShowMessage("采集字模失败：${e.message}")
            }
        }
    }
}
