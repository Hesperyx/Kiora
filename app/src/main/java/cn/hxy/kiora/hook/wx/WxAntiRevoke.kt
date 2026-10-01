package cn.hxy.kiora.hook.wx

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.hook.hookReplace
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.callMethod
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import cn.hxy.kiora.utils.reflect.getObjectOrNull
import cn.hxy.kiora.utils.reflect.toClass
import cn.hxy.kiora.utils.wx.WeChatDexKit
import java.util.concurrent.ConcurrentHashMap

/**
 * 防撤回（对齐 QQ 侧 [cn.hxy.kiora.hook.msg.AntiRevoke] 的「消息保留 + 红字标记」）。
 *
 * ## 两步
 *
 * 1. **拦撤回**：hook 撤回处理方法 [WeChatDexKit.ANTI_REVOKE_1]
 *    （`Lb41/t;->c(String,J,p0,String,String,String)V`）直接吞掉，撤回指令
 *    落不了地，消息原样保留；同时记下被撤回消息的 `msgSvrId`。
 * 2. **打标记**：hook 普通聊天列表的 onBindViewHolder `xl5/g.h(...)`，每次消息
 *    渲染都经它（holder.itemView=item 根 view、item.d.b=msg），若该消息在撤回
 *    集合里，就在 item 顶部补一个红色「已撤回」。
 *
 * ## 版本脆弱性
 *
 * 撤回方法走 DexKit 字符串锚定，不脆；但聊天 Adapter `xl5/g` 是 8.0.78 的
 * 混淆名、无日志串可锚定，只能写死反射。微信更新后类名变了就在 [hookRender]
 * 里 miss（落到 `WxAntiRevoke` 标签日志），届时按新混淆名改一行。
 *
 * ## 真机验证点
 *
 * - 红字位置：`xl5/g.h` 的 holder.itemView 是 item 根 view（含头像+气泡），红字
 *   「消息上方」的精确位置可能需要在真机上微调 [markIfRevoked] 里的布局参数。
 * - 撤回集合目前只存内存，微信进程重启后已打的标记会丢失（消息本身还在，
 *   只是不再补红字）；如需跨重启保留，再加 ObjectStore 持久化（对齐 QQ 侧）。
 */
@HookItemAnnotation(
    tag = "防撤回",
    desc = "撤回消息保留，并在消息上方显示红色「已撤回」标记",
    category = HookCategory.CHAT,
    hosts = ["wechat"]
)
object WxAntiRevoke : BaseSwitchHookItem() {

    /** 被拦下的撤回消息 msgSvrId 集合，渲染时据此补红字。 */
    private val revokedSvrs = ConcurrentHashMap.newKeySet<Long>()

    /** 最近一次渲染时的聊天 adapter 实例（`xl5/g`），撤回后用它触发刷新。 */
    @Volatile
    private var lastAdapter: Any? = null

    /** 诊断用：前 N 次渲染打日志，确认渲染入口是否触发、svrId 是否对得上。 */
    @Volatile
    private var renderLogCount = 0

    /** 红字标记的 tag，用于幂等（同一容器只加一次）。 */
    private const val MARK_TAG = "KioraRevokeMark"

    override fun onInit(): Boolean =
        runCatching { WeChatDexKit.requireMethod(WeChatDexKit.ANTI_REVOKE_1) }
            .onFailure { LogUtils.w("$name 未取到撤回处理方法，通常是还没跑过「查找方法」") }
            .isSuccess

    override fun onHook() {
        val revoke = WeChatDexKit.requireMethod(WeChatDexKit.ANTI_REVOKE_1)

        // 1. 记录被撤回消息的 svrId
        revoke.hookBefore(this) { param ->
            // c(String talker, long xmlSvrMsgId, p0, ...)
            val svrId = param.args.getOrNull(1) as? Long ?: run {
                LogUtils.w("$name 撤回参数解析失败: ${param.args.toList()}")
                return@hookBefore
            }
            revokedSvrs.add(svrId)
            LogUtils.i("$name 拦截撤回 svrId=$svrId")
            // 消息被拦下保留，微信不会刷新这条消息，红字无从补上；主动触发
            // 一次列表刷新，让消息重新走渲染入口（xl5/g.h），再补红字。
            refreshChattingList()
        }
        // 2. 吞掉撤回，消息保留
        revoke.hookReplace(this) { null }

        // 3. 渲染时补红字
        hookRender()
    }

    /** 撤回后延迟刷新聊天列表，让被拦下的消息重新渲染、补上红字。 */
    private fun refreshChattingList() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val adapter = lastAdapter
            if (adapter == null) {
                LogUtils.w("$name refresh: 还没有聊天 adapter 实例")
                return@postDelayed
            }
            LogUtils.i("$name refresh: notifyDataSetChanged (adapter=${adapter.javaClass.name})")
            runCatching { adapter.callMethod("notifyDataSetChanged") }
                .onFailure { LogUtils.e("$name notify", it) }
        }, 300)
    }

    private fun hookRender() {
        // 普通聊天列表的 onBindViewHolder：xl5/g.h(holder, item, position, ...)。
        // 逆向确认：holder.itemView = 消息 item 根 view（其 tag 就是 h0 ViewHolder）；
        // item.d.b = e9(msg)。这是普通聊天（非小程序服务号）真正的渲染入口。
        // 同时 thisObject 就是 adapter 实例，暂存下来供撤回后触发刷新。
        val adapterClass = runCatching {
            "xl5.g".toClass
        }.getOrNull() ?: run {
            LogUtils.w("$name 未取到聊天 Adapter xl5/g，红字标记跳过")
            return
        }
        val bind = adapterClass.findMethodOrNull {
            name = "h"
            paramCount = 6
        } ?: run {
            LogUtils.w("$name 未取到 xl5/g.h 渲染方法，红字标记跳过")
            return
        }
        bind.hookAfter(this) { param ->
            // thisObject 是 xl5/g（渲染委托者，继承 Object，不是 Adapter）；它的
            // h 字段才是真正的聊天 RecyclerView.Adapter（chatting/adapter/k）。
            lastAdapter = runCatching { param.thisObject.getObjectOrNull("h") }.getOrNull()
            val holder = param.args.getOrNull(0) ?: return@hookAfter
            val item = param.args.getOrNull(1) ?: return@hookAfter
            val itemView = runCatching { holder.getObjectOrNull("itemView") as? View }.getOrNull()
            val msg = runCatching { item.getObjectOrNull("d")?.getObjectOrNull("b") }.getOrNull()
            markIfRevoked(itemView, msg)
        }
        LogUtils.i("$name 红字标记 hook 已挂载: ${adapterClass.name}.h")
    }

    /**
     * 解析「气泡 view」，用于给红字定位。
     *
     * 不同消息类型的气泡存在不同字段：单图消息在 `d` 字段（ChattingImgMvvmView），
     * 多图消息在 `b` 字段（ChattingMediaGroupMvvmView），文本/表情等经典消息的
     * `getMainContainerView()` 直接返回气泡。注意单图消息的 `b` 字段是
     * `ChattingMsgSourceView`（消息来源、0x0 空 view），所以用类名含 `MvvmView`
     * 来过滤，避免误取到它。
     */
    private fun resolveBubble(itemView: View): View? {
        val vh = itemView.getTag() as? Any ?: return null
        // 单图消息：d 字段 = ChattingImgMvvmView（图片气泡）
        val d = runCatching { vh.getObjectOrNull("d") as? View }.getOrNull()
        if (d != null && d.javaClass.name.contains("MvvmView")) return d
        // 多图消息：b 字段 = ChattingMediaGroupMvvmView（多图气泡）
        val b = runCatching { vh.getObjectOrNull("b") as? View }.getOrNull()
        if (b != null && b.javaClass.name.contains("MvvmView")) return b
        // 经典消息（文本/表情等）：getMainContainerView() = 气泡
        return runCatching { vh.callMethod("getMainContainerView") as? View }.getOrNull()
    }

    private fun markIfRevoked(itemView: View?, msg: Any?) {
        if (itemView == null || msg == null) return
        // 消息的 svrId 没有标准 getter（8.0.78 混淆成 F0() 之类），直接读
        // 可读字段 field_msgSvrId —— 它与撤回方法入参的 xmlSvrMsgId 是同一个值。
        val svrId = runCatching {
            msg.getObjectOrNull("field_msgSvrId") as? Long
        }.getOrNull() ?: run {
            LogUtils.w("$name 读不到 field_msgSvrId: ${msg.javaClass.name}")
            return
        }
        if (renderLogCount < 30) {
            renderLogCount++
            LogUtils.i("$name 渲染#$renderLogCount svrId=$svrId inSet=${svrId in revokedSvrs} set=$revokedSvrs")
        }
        if (svrId !in revokedSvrs) return
        LogUtils.i("$name 命中撤回消息 svrId=$svrId")

        // 幂等：同一 item 只加一次红字
        if (itemView.findViewWithTag<View>(MARK_TAG) != null) return

        // onBindViewHolder 刚回调时气泡还没完成测量/布局，直接读坐标会拿到 0
        // （重新打开聊天页时就会乱标）。post 到主线程，等下一帧 layout 完成后
        // 再定位气泡坐标、补红字。
        itemView.post {
            if (itemView.findViewWithTag<View>(MARK_TAG) != null) return@post
            val bubble = resolveBubble(itemView) ?: return@post
            // 气泡 view 的 top 是相对直接父容器的；累加父链得到相对 itemView 的绝对位置
            var absTop = 0
            var absLeft = 0
            var cursor: View? = bubble
            while (cursor != null && cursor !== itemView) {
                absTop += cursor.top
                absLeft += cursor.left
                cursor = cursor.parent as? View
            }

            val tv = TextView(itemView.context).apply {
                tag = MARK_TAG
                text = "已撤回"
                setTextColor(Color.RED)
                textSize = 14f
            }
            tv.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val tvW = tv.measuredWidth
            val tvH = tv.measuredHeight
            val bubbleCenterX = absLeft + bubble.width / 2

            val lp = FrameLayout.LayoutParams(tvW, tvH)
            lp.leftMargin = (bubbleCenterX - tvW / 2).coerceAtLeast(0)
            lp.topMargin = (absTop - tvH).coerceAtLeast(0)
            (itemView as? ViewGroup)?.addView(tv, lp)
        }
    }
}
