@file:Suppress("NOTHING_TO_INLINE")

package dev.ujhhgtg.wekit.utils.android

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
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
import dev.ujhhgtg.wekit.utils.WeLogger

private const val CHOICE_STYLE_FILLED = 0
private const val CHOICE_STYLE_SECONDARY = 1
private const val CHOICE_STYLE_GHOST = 2

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
 *
 * 显式传入已确认的 [activity]（由调用方轮询拿到），避免在弹窗前二次反射查询
 * 导致 Activity 变化而退回自动路径。用 [Activity.runOnUiThread] 确保主线程弹出，
 * 不依赖协程 Main dispatcher（冷启动早期可能未就绪）。
 *
 * [onNoActivity]：Activity 变为不可用（finishing / 已销毁）时的兜底回调，
 * 由调用方决定是重试、Toast 还是延时处理——**绝不静默自动选某一项**。
 */
fun showChoiceDialog(
    title: String,
    message: String,
    positiveText: String,
    negativeText: String,
    neutralText: String? = null,
    onChoice: (Boolean) -> Unit,
    activity: Activity? = HostEnv.activity,
    onNeutral: (() -> Unit)? = null,
    onNoActivity: (() -> Unit)? = null,
) {
    val chosen = java.util.concurrent.atomic.AtomicBoolean(false)
    val guard: (Boolean) -> Unit = { cloud ->
        if (chosen.compareAndSet(false, true)) onChoice(cloud)
    }
    val neutralGuard: () -> Unit = {
        if (chosen.compareAndSet(false, true)) onNeutral?.invoke()
    }
    showChoiceDialogOnce(
        title, message, positiveText, negativeText, neutralText,
        guard, neutralGuard, activity, onNoActivity, chosen, 0,
    )
}

/**
 * 实际弹出逻辑 + 「Activity 中途销毁且用户未选择 → 重弹」兜底（最多 3 次）。
 * 微信冷启动时弹窗可能挂在一闪而过的 Activity 上（WindowLeaked），
 * 没有重弹的话 onChoice 永远不触发，解析流程直接卡死。
 */
private fun showChoiceDialogOnce(
    title: String,
    message: String,
    positiveText: String,
    negativeText: String,
    neutralText: String?,
    onChoice: (Boolean) -> Unit,
    onNeutral: () -> Unit,
    activity: Activity?,
    onNoActivity: (() -> Unit)?,
    chosen: java.util.concurrent.atomic.AtomicBoolean,
    attempt: Int,
) {
    if (chosen.get()) return

    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        if (attempt < 3) {
            WeLogger.w("NoticeDialog", "showChoiceDialog: Activity 不可用，1.5 秒后重弹（第 ${attempt + 1} 次）")
            ModuleScope.launchMainDelayed(1_500) {
                showChoiceDialogOnce(
                    title, message, positiveText, negativeText, neutralText,
                    onChoice, onNeutral, HostEnv.activity, onNoActivity, chosen, attempt + 1,
                )
            }
        } else {
            WeLogger.w("NoticeDialog", "showChoiceDialog: 重弹耗尽，走 onNoActivity")
            if (chosen.compareAndSet(false, true)) onNoActivity?.invoke() ?: onChoice(true)
        }
        return
    }

    activity.runOnUiThread {
        if (chosen.get()) return@runOnUiThread
        if (activity.isFinishing || activity.isDestroyed) {
            WeLogger.w("NoticeDialog", "showChoiceDialog: Activity 在弹出前已销毁，重弹")
            ModuleScope.launchMainDelayed(1_500) {
                showChoiceDialogOnce(
                    title, message, positiveText, negativeText, neutralText,
                    onChoice, onNeutral, HostEnv.activity, onNoActivity, chosen, attempt + 1,
                )
            }
            return@runOnUiThread
        }
        runCatching {
            val dialog = buildChoiceDialog(
                activity, title, message, positiveText, negativeText, neutralText,
                onChoice, onNeutral,
            )
            dialog.show()
            WeLogger.i("NoticeDialog", "showChoiceDialog: 选择框已弹出，等待用户选择")
            // 看门狗：弹出 2 秒后如果弹窗已随 Activity 死亡且用户没选，重弹
            ModuleScope.launchMainDelayed(2_000) {
                if (!chosen.get() && (!dialog.isShowing || activity.isFinishing || activity.isDestroyed)) {
                    WeLogger.w("NoticeDialog", "showChoiceDialog: 弹窗随 Activity 销毁且未获选择，重弹")
                    showChoiceDialogOnce(
                        title, message, positiveText, negativeText, neutralText,
                        onChoice, onNeutral, HostEnv.activity, onNoActivity, chosen, attempt + 1,
                    )
                }
            }
        }.onFailure {
            WeLogger.e("NoticeDialog", "showChoiceDialog: 弹出失败", it)
            if (chosen.compareAndSet(false, true)) onNoActivity?.invoke() ?: onChoice(true)
        }
    }
}

private fun buildChoiceDialog(
    context: Context,
    title: String,
    message: String,
    positiveText: String,
    negativeText: String,
    neutralText: String?,
    onChoice: (Boolean) -> Unit,
    onNeutral: () -> Unit,
): AlertDialog {
    val dp = { v: Float -> (v * context.resources.displayMetrics.density).toInt() }
    val accent = Color.parseColor("#07C160")

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(28f), dp(28f), dp(28f), dp(24f))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(24f).toFloat()
        }
    }

    val titleTv = TextView(context).apply {
        text = title
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor("#111111"))
    }
    container.addView(titleTv)

    val msgTv = TextView(context).apply {
        text = message
        textSize = 15f
        setTextColor(Color.parseColor("#666666"))
        setPadding(0, dp(14f), 0, 0)
        setLineSpacing(0f, 1.3f)
    }
    container.addView(msgTv)

    // 按钮行：主按钮（微信绿填充）+ 次按钮（浅灰填充）+ 中性按钮（描边幽灵），等宽平分
    val buttonRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(24f), 0, 0)
    }

    val buttons = mutableListOf<TextView>()

    fun addButton(text: String, style: Int, onClick: () -> Unit): TextView {
        val btn = TextView(context).apply {
            this.text = text
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(23f).toFloat()
                when (style) {
                    CHOICE_STYLE_FILLED -> setColor(accent)
                    CHOICE_STYLE_GHOST -> {
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(1f), Color.parseColor("#DDDDDD"))
                    }
                    else -> setColor(Color.parseColor("#F2F2F2"))
                }
            }
            setTextColor(
                when (style) {
                    CHOICE_STYLE_FILLED -> Color.WHITE
                    CHOICE_STYLE_GHOST -> Color.parseColor("#999999")
                    else -> Color.parseColor("#333333")
                }
            )
            setOnClickListener { onClick() }
        }
        val lp = LinearLayout.LayoutParams(0, dp(46f), 1f)
        buttonRow.addView(btn, lp)
        buttons += btn
        return btn
    }

    var dialog: AlertDialog? = null
    addButton(positiveText, CHOICE_STYLE_FILLED) {
        dialog?.dismiss()
        onChoice(true)
    }
    addButton(negativeText, CHOICE_STYLE_SECONDARY) {
        dialog?.dismiss()
        onChoice(false)
    }
    if (neutralText != null) {
        addButton(neutralText, CHOICE_STYLE_GHOST) {
            dialog?.dismiss()
            onNeutral()
        }
    }
    buttons.forEachIndexed { index, btn ->
        if (index < buttons.size - 1) {
            (btn.layoutParams as LinearLayout.LayoutParams).rightMargin = dp(10f)
        }
    }
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
        .also { dialog = it }
}

/**
 * 创建进度弹窗。初始为「不确定进度」（云端拉取阶段）。
 * 调用 [ProgressDialog.update] 更新文字，[ProgressDialog.updateProgress] 切换到确定进度，
 * [ProgressDialog.dismiss] 关闭。
 */
fun showProgressDialog(
    title: String,
    initialMessage: String,
    activity: Activity? = HostEnv.activity,
): ProgressDialog {
    val handle = ProgressDialog(title)
    val target = activity?.takeIf { !it.isFinishing && !it.isDestroyed }

    if (target != null) {
        // 用调用方已知的稳定 Activity 直接在其 UI 线程创建；若当前就在 UI 线程，
        // runOnUiThread 会立即执行。这里等弹窗真正 show 出来再返回，避免云端下载
        // 比弹窗创建还快，导致进度一出现就是满格。
        val shown = java.util.concurrent.CountDownLatch(1)
        target.runOnUiThread {
            try {
                if (!target.isFinishing && !target.isDestroyed) {
                    runCatching { handle.createAndShow(target, initialMessage) }
                }
            } finally {
                shown.countDown()
            }
        }
        shown.await(3, java.util.concurrent.TimeUnit.SECONDS)
    } else {
        ModuleScope.launchMain {
            val current = HostEnv.activity
            if (current != null && !current.isFinishing) {
                runCatching { handle.createAndShow(current, initialMessage) }
            } else {
                Toast.makeText(HostInfo.application, "$title：$initialMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
    return handle
}

class ProgressDialog(private val title: String) {
    @Volatile
    private var dialog: AlertDialog? = null

    @Volatile
    private var dismissed = false

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
        if (dismissed) return
        // 主题色：微信绿（符合宿主调性）
        val accent = Color.parseColor("#07C160")

        // 圆角容器
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 28f), dp(context, 28f), dp(context, 28f), dp(context, 24f))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(context, 24f).toFloat()
            }
        }

        val titleTv = TextView(context).apply {
            text = title
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111111"))
            gravity = Gravity.CENTER_VERTICAL
        }
        container.addView(titleTv)

        val track = roundedDrawable(context, Color.parseColor("#F0F0F0"))
        val fill = roundedDrawable(context, accent)
        val clip = ClipDrawable(fill, Gravity.START or Gravity.FILL_VERTICAL, ClipDrawable.HORIZONTAL)
        val progressLayer = LayerDrawable(arrayOf(track, clip)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
        val bar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            progressDrawable = progressLayer
            indeterminateDrawable?.setTint(accent)
        }
        val barLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 8f)
        ).apply { topMargin = dp(context, 20f) }
        container.addView(bar, barLp)

        val msg = latestMessage ?: message
        val text = TextView(context).apply {
            text = msg
            textSize = 14f
            setTextColor(Color.parseColor("#555555"))
            setPadding(0, dp(context, 16f), 0, 0)
        }
        container.addView(text)

        val percent = TextView(context).apply {
            setText("")
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
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

    private fun roundedDrawable(context: Context, color: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = context.resources.displayMetrics.density * 4f
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
        dismissed = true
        ModuleScope.launchMain {
            dialog?.dismiss()
            dialog = null
        }
    }
}
