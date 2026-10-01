package cn.hxy.kiora.hook.wx

import android.widget.Button
import androidx.core.view.isVisible
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.wx.WeChatDexKit
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * 自动查看原图 / 原视频（移植自 WA `AutoViewOriginalPhotoHook`）。
 *
 * 打开图片（视频）大图页时，页面上会有一个「查看原图 / 查看原视频」的按钮，
 * 这里在页面刷新后把它点掉。
 *
 * ## 为什么不按字段名取按钮
 *
 * WA 自己也**没按字段名**取，而是「找所有 `Button` 类型的字段，看文字里有没有
 * 关键词」。这是对的：该页（`ImageGalleryUI`）在 8.0.78 上有 **4 个 `Button` 字段**，
 * 字段名混淆过（`a`/`b`/`c`…），但按钮文字要给人看、不可能变。
 * 所以判据是「类型 + 可见 + 文案」，三个都是语义信息，跨版本稳。
 *
 * 8.0.54 之前的方法串是 `setImageHdImgBtnVisibility`，之后换成了
 * `setHdImageActionDownloadable` —— 8.0.78 的字符串池里**完全不存在**旧串，
 * 所以只登记新串，不做版本分支（见 [WeChatDexKit.AUTO_VIEW_ORIGINAL_HD]）。
 */
@HookItemAnnotation(
    tag = "自动查看原图",
    desc = "打开图片/视频时自动点击「查看原图 / 查看原视频」",
    category = HookCategory.CHAT,
    hosts = ["wechat"]
)
object WxAutoViewOriginal : BaseSwitchHookItem() {

    /**
     * 按钮文案关键词。
     *
     * 中英各两组：微信国内版与 PLAY 版的文案不同，而两版的字段结构一致 ——
     * 这是一个「按文案识别」天然优于「按版本分支」的例子。
     */
    private val keywords = listOf(
        "查看原图", "Full Image",
        "查看原视频", "Original quality"
    )

    private var methods: List<Method> = emptyList()

    override fun onInit(): Boolean {
        methods = listOf(WeChatDexKit.AUTO_VIEW_ORIGINAL_HD, WeChatDexKit.AUTO_VIEW_ORIGINAL_VIDEO)
            .mapNotNull { key ->
                runCatching { WeChatDexKit.requireMethod(key) }.getOrNull()
            }
        if (methods.isEmpty()) {
            LogUtils.w("$name 未取到图库页方法，通常是还没跑过「查找方法」")
            return false
        }
        return super.onInit()
    }

    override fun onHook() {
        methods.forEach { method ->
            method.hookAfter(this) { param ->
                val target = param.thisObject
                buttonsOf(target.javaClass).forEach { field ->
                    val button = runCatching { field.get(target) as? Button }.getOrNull()
                        ?: return@forEach
                    if (!button.isVisible) return@forEach

                    val text = button.text?.toString() ?: return@forEach
                    if (keywords.any { text.contains(it, ignoreCase = true) }) {
                        button.performClick()
                    }
                }
            }
        }
    }

    /**
     * 收集类及其父类上声明的所有 `Button` 字段。
     *
     * 沿继承链而不是只看本类：图库页的按钮有一部分声明在父类里
     * （这正是 WA 的 `firstField` 会栽的同一个坑，此处直接按继承链遍历绕开它）。
     */
    private fun buttonsOf(clazz: Class<*>): List<Field> =
        generateSequence(clazz) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .filter { Button::class.java.isAssignableFrom(it.type) }
            .onEach { it.isAccessible = true }
            .toList()
}
