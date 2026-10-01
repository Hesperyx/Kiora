package cn.hxy.kiora.hook.wx

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import java.lang.reflect.Method

/**
 * 自动勾选原图（移植自 WA `AutoSelectOriginalPhotoHook`）。
 *
 * 发图/发视频时微信会进一个预览选择页，页面上有「原图」开关。这里在**启动 Activity
 * 之前**给 Intent 塞 `send_raw_img = true`，让那个页面自己以勾选态初始化 ——
 * 比等页面出来再去点控件稳得多（控件名会变，Intent extra 不会）。
 *
 * ## 为什么锚点是框架类而不是微信类
 *
 * WA 的实现挂在 `IStartActivity` 上（框架统一拦 `startActivity*`）。
 * 本模块没有这类通用 API，所以直接挂 `Activity` 的四个入口方法。
 * 挂框架类而不是宿主类，恰好**完全绕开混淆**：`android.app.Activity` 的名字
 * 不会变，方法签名是公开 API。代价是每次启动 Activity 都要过一遍这段代码 ——
 * 只是一次字符串比较，可忽略。
 *
 * 四个入口都要挂：`startActivity(Intent)` 与 `startActivityForResult(Intent,int)`
 * 是两条不同的调用链，只挂一个会漏。重复命中时 `putExtra` 是幂等的。
 */
@HookItemAnnotation(
    tag = "自动勾选原图",
    desc = "发送图片/视频时默认勾选「原图」",
    category = HookCategory.CHAT,
    hosts = ["wechat"]
)
object WxAutoSelectOriginal : BaseSwitchHookItem() {

    /** Intent extra：微信自己的「原图」标记位。 */
    private const val EXTRA_SEND_RAW_IMG = "send_raw_img"

    /**
     * 需要注入的目标页面。
     *
     * 都是 `com.tencent.mm.plugin.gallery.ui` 下的类 —— 这个包名与类名
     * **不属于混淆范围**（是插件模块的稳定骨架名），并且 §17.2 已核验两者都存在。
     */
    private val targetPages = setOf(
        "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI",
        "com.tencent.mm.plugin.gallery.ui.ImagePreviewUI"
    )

    private var entries: List<Method> = emptyList()

    override fun onInit(): Boolean {
        val activityClass = Activity::class.java
        entries = listOfNotNull(
            activityClass.findMethodOrNull {
                name = "startActivity"
                paramTypes(Intent::class.java)
            },
            activityClass.findMethodOrNull {
                name = "startActivity"
                paramTypes(Intent::class.java, Bundle::class.java)
            },
            activityClass.findMethodOrNull {
                name = "startActivityForResult"
                paramTypes(Intent::class.java, Integer.TYPE)
            },
            activityClass.findMethodOrNull {
                name = "startActivityForResult"
                paramTypes(Intent::class.java, Integer.TYPE, Bundle::class.java)
            }
        )

        if (entries.isEmpty()) {
            // 框架类的方法都取不到，说明反射环境异常 —— 报出来而不是静默
            LogUtils.w("$name 未取到 Activity 启动方法（反射环境异常）")
            return false
        }
        return super.onInit()
    }

    override fun onHook() {
        entries.forEach { method ->
            method.hookBefore(this) { param ->
                val intent = param.args.getOrNull(0) as? Intent ?: return@hookBefore
                val page = intent.component?.className ?: return@hookBefore
                if (page in targetPages) {
                    intent.putExtra(EXTRA_SEND_RAW_IMG, true)
                }
            }
        }
    }
}
