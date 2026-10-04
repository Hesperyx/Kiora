package cn.hxy.kiora.hook.api

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * AIO 气泡顶部的统一标记容器。
 *
 * “已撤回”、“闪照”等标记都加入同一个水平 LinearLayout，
 * 多个标记同时触发时天然横排居中，与单个标记的位置保持一致。
 */
object AIOMarkerContainer {

    const val TAG = "KioraAioMarkerContainer"

    fun get(frameLayout: FrameLayout): LinearLayout {
        frameLayout.findViewWithTag<LinearLayout>(TAG)?.let { return it }

        val container = LinearLayout(frameLayout.context).apply {
            orientation = LinearLayout.HORIZONTAL
            tag = TAG
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = 10
            }
        }
        frameLayout.addView(container)
        return container
    }

    fun find(frameLayout: FrameLayout): LinearLayout? =
        frameLayout.findViewWithTag(TAG)
}
