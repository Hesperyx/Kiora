package cn.hxy.kiora.hook.wx

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.WxAutoLoginConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.wx.WxAutoLoginPage
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.findFieldOrNull
import cn.hxy.kiora.utils.reflect.findMethodOrNull
import cn.hxy.kiora.utils.reflect.toClass

/**
 * 自动点击登录（移植自 WA `AutoLoginWinHook`）。
 *
 * 微信在第二台设备登录时要过一个确认页（`ExtDeviceWXLoginUI`）：
 * 勾「同步最近消息」、可能要选设备、再点「登录」。这里把「勾什么 + 点按钮」自动化。
 *
 * ## 两个钩子
 *
 * 1. `onCreate(Bundle)` **之前**：往 Intent 塞 `intent.key.function.control` 位掩码。
 *    这是微信自己的「预先勾选」通道 —— 比页面出来再去点 CheckBox 稳，
 *    因为 CheckBox 是两个字段（`A`、`C`，见 `recon/dumpclass.py` 输出），
 *    按字段名点会绑死在混淆名上。
 * 2. `initView()V` **之后**：取该类唯一的 `Button` 字段（8.0.78 是 `o`），
 *    `callOnClick()`。用**类型**取而不是名字 —— 类里只有一个 `Button`，
 *    按类型取既唯一又不依赖混淆名；同理用 `callOnClick()` 而不是 `performClick()`：
 *    前者直接跑监听器，不受可见性/可点击性影响。
 *
 * 目标类名 `com.tencent.mm.plugin.webwx.ui.ExtDeviceWXLoginUI` 不属于混淆范围
 * （插件模块骨架名），§17.2 已核验它与两个方法都存在。
 */
@HookItemAnnotation(
    tag = "自动点击登录",
    desc = "在设备登录确认页自动勾选并点击登录",
    category = HookCategory.MISC,
    hosts = ["wechat"]
)
object WxAutoLogin : BaseClickableHookItem<WxAutoLoginConfig>(WxAutoLoginConfig.serializer()) {

    override val defaultConfig: WxAutoLoginConfig = WxAutoLoginConfig()

    private const val LOGIN_UI_CLASS = "com.tencent.mm.plugin.webwx.ui.ExtDeviceWXLoginUI"

    /** 微信的位掩码通道：三个位分别控制三个勾选项。 */
    private const val EXTRA_FUNCTION_CONTROL = "intent.key.function.control"
    private const val FLAG_AUTO_SYNC_MSG = 0b001
    private const val FLAG_SHOW_LOGIN_DEVICE = 0b010
    private const val FLAG_AUTO_LOGIN_DEVICE = 0b100

    override fun onInit(): Boolean {
        val clazz = runCatching { LOGIN_UI_CLASS.toClass }.getOrNull()
        if (clazz == null) {
            LogUtils.w("$name 未找到登录确认页 $LOGIN_UI_CLASS")
            return false
        }

        val onCreate = clazz.findMethodOrNull {
            name = "onCreate"
            paramTypes(Bundle::class.java)
        }
        if (onCreate == null) {
            LogUtils.w("$name 登录确认页没有 onCreate(Bundle)")
            return false
        }

        return super.onInit()
    }

    override fun onHook() {
        val clazz = LOGIN_UI_CLASS.toClass

        clazz.findMethodOrNull {
            name = "onCreate"
            paramTypes(Bundle::class.java)
        }?.hookBefore(this) { param ->
            val activity = param.thisObject as? Activity ?: return@hookBefore
            var mask = 0
            if (config.autoSyncMsg) mask = mask or FLAG_AUTO_SYNC_MSG
            if (config.showLoginDevice) mask = mask or FLAG_SHOW_LOGIN_DEVICE
            if (config.autoLoginDevice) mask = mask or FLAG_AUTO_LOGIN_DEVICE
            activity.intent?.putExtra(EXTRA_FUNCTION_CONTROL, mask)
        }

        clazz.findMethodOrNull {
            name = "initView"
            paramCount = 0
        }?.hookAfter(this) { param ->
            val field = clazz.findFieldOrNull {
                type = Button::class.java
                isStatic = false
            } ?: return@hookAfter
            runCatching { (field.get(param.thisObject) as? Button)?.callOnClick() }
                .onFailure { LogUtils.e(this, it) }
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        WxAutoLoginPage(config, ::updateConfig, onDismiss)
    }
}
