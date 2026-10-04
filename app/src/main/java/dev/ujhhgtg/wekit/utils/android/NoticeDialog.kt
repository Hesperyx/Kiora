@file:Suppress("NOTHING_TO_INLINE")

package dev.ujhhgtg.wekit.utils.android

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.host.HostEnv
import dev.ujhhgtg.wekit.utils.HostInfo

/**
 * 宿主无关的 DexKit 解析进度弹窗（纯原生 View，不依赖 Compose）。
 *
 * 一个弹窗贯穿「云端拉取（不确定进度条）→ 本地扫描（确定进度条）」两阶段，
 * 避免多弹窗叠加。Activity 未就绪时退化为 Toast（进度更新静默丢弃）。
 */

fun showNoticeDialog(title: String, message: String) {
    ModuleScope.launchMain {
        val activity = HostEnv.activity
        if (activity != null && !activity.isFinishing) {
            runCatching {
                AlertDialog.Builder(
                    android.view.ContextThemeWrapper(activity, android.R.style.Theme_Material_Light_Dialog_Alert)
                )
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("知道了", null)
                    .show()
            }
        } else {
            Toast.makeText(HostInfo.application, "$title：$message", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * 双选项选择框（如「云端拉取」/「本地扫描」）。UI 与进度弹窗完全一致：
 * 圆角白色卡片 + 微信绿主按钮 + 灰色次按钮。
 * 用户选择后回调 [onChoice]（true = 选 positive，false = 选 negative）。
 * Activity 未就绪时回退：默认走 [onChoice](true)（优先云端）。
 */
fun showChoiceDialog(
    title: String,
    message: String,
    positiveText: String,
    negativeText: String,
    onChoice: (Boolean) -> Unit,
) {
    ModuleScope.launchMain {
        val activity = HostEnv.activity
        if (activity != null && !activity.isFinishing) {
            runCatching {
                val dialog = buildChoiceDialog(activity, title, message, positiveText, negativeText, onChoice)
                dialog.show()
            }.onFailure {
                onChoice(true)
            }
        } else {
            onChoice(true)
        }
    }
}

private fun buildChoiceDialog(
    context: Context,
    title: String,
    message: String,
    positiveText: String,
    negativeText: String,
    onChoice: (Boolean) -> Unit,
): AlertDialog {
    val dp = { v: Float -> (v * context.resources.displayMetrics.density).toInt() }
    val accent = Color.parseColor("#07C160")

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24f), dp(24f), dp(24f), dp(20f))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(20f).toFloat()
        }
    }

    val titleTv = TextView(context).apply {
        text = title
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor("#191919"))
    }
    container.addView(titleTv)

    val msgTv = TextView(context).apply {
        text = message
        textSize = 15f
        setTextColor(Color.parseColor("#333333"))
        setPadding(0, dp(16f), 0, dp(4f))
        setLineSpacing(0f, 1.25f)
    }
    container.addView(msgTv)

    // 按钮行：主按钮（微信绿）+ 次按钮（灰色）
    val buttonRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
        setPadding(0, dp(16f), 0, 0)
    }

    fun makeButton(text: String, color: Int, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            setPadding(dp(20f), dp(10f), dp(20f), dp(10f))
            setOnClickListener { onClick() }
        }

    val negativeBtn = makeButton(negativeText, Color.parseColor("#666666")) { onChoice(false) }
    val positiveBtn = makeButton(positiveText, accent) { onChoice(true) }
    buttonRow.addView(negativeBtn)
    buttonRow.addView(positiveBtn)
    container.addView(buttonRow)

    return AlertDialog.Builder(
        android.view.ContextThemeWrapper(context, android.R.style.Theme_Material_Light_Dialog_Alert)
    )
        .setView(container)
        .setCancelable(false)
        .create()
        .apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
}

/**
 * 创建进度弹窗。初始为「不确定进度」（云端拉取阶段）。
 * 调用 [ProgressDialog.update] 更新文字，[ProgressDialog.updateProgress] 切换到确定进度，
 * [ProgressDialog.dismiss] 关闭。
 */
fun showProgressDialog(title: String, initialMessage: String): ProgressDialog {
    val handle = ProgressDialog(title)
    ModuleScope.launchMain {
        val activity = HostEnv.activity
        if (activity != null && !activity.isFinishing) {
            runCatching {
                handle.createAndShow(activity, initialMessage)
            }
        } else {
            Toast.makeText(HostInfo.application, "$title：$initialMessage", Toast.LENGTH_LONG).show()
        }
    }
    return handle
}

class ProgressDialog(private val title: String) {
    @Volatile
    private var dialog: AlertDialog? = null

    @Volatile
    private var progressBar: ProgressBar? = null

    @Volatile
    private var titleView: TextView? = null

    @Volatile
    private var textView: TextView? = null

    @Volatile
    private var percentView: TextView? = null

    // 弹窗是异步创建的，可能在 update/updateProgress 之后才真正弹出。
    // 这里缓存最新文案/进度，创建时用缓存值初始化，避免「更新早于创建」导致的 no-op 丢失。
    @Volatile
    private var latestMessage: String? = null

    @Volatile
    private var latestProgress: Int = -1 // -1 = 不确定进度

    @Volatile
    private var latestTotal: Int = 0

    private fun dp(context: Context, v: Float): Int =
        (v * context.resources.displayMetrics.density).toInt()

    internal fun createAndShow(context: Context, message: String) {
        // 主题色：微信绿（符合宿主调性）
        val accent = Color.parseColor("#07C160")

        // 圆角容器
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 24f), dp(context, 24f), dp(context, 24f), dp(context, 20f))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(context, 20f).toFloat()
            }
        }

        val titleTv = TextView(context).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#191919"))
            gravity = Gravity.CENTER_VERTICAL
        }
        container.addView(titleTv)

        val bar = ProgressBar(context).apply {
            isIndeterminate = true
            // 用自定义 drawable 染成主题色
            progressDrawable = defaultProgressTint(context, accent)
            indeterminateDrawable?.setTint(accent)
        }
        val barLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 6f)
        ).apply { topMargin = dp(context, 20f) }
        container.addView(bar, barLp)

        val msg = latestMessage ?: message
        val text = TextView(context).apply {
            text = msg
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, dp(context, 16f), 0, 0)
        }
        container.addView(text)

        val percent = TextView(context).apply {
            setText("")
            textSize = 13f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.END
            setPadding(0, dp(context, 8f), 0, 0)
        }
        container.addView(percent)

        val d = AlertDialog.Builder(
            android.view.ContextThemeWrapper(context, android.R.style.Theme_Material_Light_Dialog_Alert)
        )
            .setView(container)
            .setCancelable(false)
            .create()
        // 透明窗口背景，露出圆角卡片
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 用缓存的最新进度初始化（可能已有 updateProgress 早于创建发生）
        val lp = latestProgress
        if (lp >= 0) {
            bar.isIndeterminate = false
            bar.max = latestTotal
            bar.progress = lp
            percent.text = "$lp / ${latestTotal}"
        }

        d.show()
        dialog = d
        progressBar = bar
        titleView = titleTv
        textView = text
        percentView = percent
    }

    private fun defaultProgressTint(context: Context, color: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = context.resources.displayMetrics.density * 3f
        }

    /** 更新文字（不确定进度阶段，如「正在从云端拉取…」）。 */
    fun update(message: String) {
        latestMessage = message
        ModuleScope.launchMain { textView?.text = message }
    }

    /** 切换到确定进度条并更新（本地扫描阶段，如「本地解析 3/21」）。 */
    fun updateProgress(message: String, current: Int, total: Int) {
        latestMessage = message
        latestProgress = current
        latestTotal = total
        ModuleScope.launchMain {
            progressBar?.isIndeterminate = false
            progressBar?.max = total
            progressBar?.progress = current
            textView?.text = message
            percentView?.text = if (total > 0) "$current / $total" else ""
        }
    }

    fun dismiss() {
        ModuleScope.launchMain {
            dialog?.dismiss()
            dialog = null
        }
    }
}
