package com.coc.zkqcode.jar.code.mainbase.attack

import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.recognizer.TextRecognizer

// Fixed screen layout of the event reward popup (1280x720):
// - Red title ribbon "选择一项奖励！" with two vertical variants: battle y:90-132, settlement y:175-217
// - Three reward cards centered at x≈385/640/897, both variants inside y:219-575
// - Card name text at y:445-467 (battle) / y:475-497 (settlement), "活动货币" subtitle below the name
// - A fixed OCR zone y:435-525 covers the name + subtitle rows of both variants
/**
 * Detects and dismisses the "选择一项奖励" event reward popup that can appear
 * 0-3 times during an attack (on destruction milestones and on the settlement screen).
 *
 * Selection rule: if one of the three cards carries the "活动货币" (event currency)
 * subtitle, pick that card; otherwise always pick the middle card. Other reward
 * names are deliberately NOT matched — only the popup title and "活动货币" are
 * special-cased, everything else falls back to the middle card.
 *
 * @return true if the popup was found and a card was tapped.
 */
suspend fun handleRewardPopup(): Boolean {
    // 1. Find the red title ribbon (battle variant first, then settlement variant)
    val anchor = findMultiColors(schema = MyColors.RewardPopupTitle)
        ?: findMultiColors(schema = MyColors.RewardPopupTitle2)
        ?: return false

    // 2. OCR-verify the ribbon title to rule out false positives from red battlefield elements.
    //    The anchor y tells which vertical variant is showing (battle ≈90, settlement ≈175).
    val titleTop = if (anchor.y < 140) 86 else 170
    val titleText = TextRecognizer.recognize(404, titleTop, 890, titleTop + 54)
        .joinToString("") { it.text }
    if (!titleText.contains("选择") && !titleText.contains("奖励")) return false

    // 3. OCR the fixed card text zone (name + subtitle rows of all three cards, both variants)
    val lines = TextRecognizer.recognize(285, 435, 1005, 525)
    val eventLine = lines.firstOrNull { it.text.replace(" ", "").contains("活动货币") }

    // 4. Pick the card: the one showing "活动货币", otherwise the middle card
    val cardX = eventLine?.position?.let { pos ->
        when (285 + pos.centerX()) {
            in 0 until 490 -> 385   // left card
            in 490 until 780 -> 640 // middle card
            else -> 897             // right card
        }
    } ?: 640
    TouchActions.tap(cardX, 390, delayTime = 1200)
    return true
}
