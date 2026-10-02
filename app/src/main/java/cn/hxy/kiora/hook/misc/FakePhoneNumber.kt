package cn.hxy.kiora.hook.misc

import android.os.Bundle
import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.qq.conf.PhoneConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.qq.ui.FakePhonePage
import cn.hxy.kiora.utils.dexkit.DexKitTask
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.reflect.findMethod
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseMatcher
import java.lang.reflect.Method

/**
 * 伪装手机号码（移植自 TCQT 的 FakePhone）。
 *
 * 账号与安全页面里的号码是宿主动态拼进 Bundle 再交给页面渲染的，这里在拼装前
 * 把 `phone` 换掉，页面自己读到的就是伪装值，不需要去改控件。
 *
 * 目标方法在宿主里没有稳定名字，用 DexKit 按特征找：参数是 `(int, boolean, Object)`，
 * 并且用了 `status` / `wording` / `target_desc` / `target_name` 这几个字符串。
 * 第 0 个参数是页面状态，等于 5 时才是手机号那一栏 —— 别的状态也带同样的字符串，
 * 不判状态会把号码塞进不相干的页面。
 */
@HookItemAnnotation(
    "伪装手机号码",
    "点击设置要显示的号码，伪装账号与安全页面里的手机号",
    HookCategory.MISC
)
object FakePhoneNumber : BaseClickableHookItem<PhoneConfig>(PhoneConfig.serializer()), DexKitTask {

    override val defaultConfig: PhoneConfig = PhoneConfig()

    private lateinit var fillPhone: Method

    override fun onInit(): Boolean {
        // 缓存里没有就抛异常，交给基类把这一项标记成不可用 ——
        // 用户需要先在设置页跑一次「查找方法」。
        fillPhone = requireMethod("fillPhone")
        return super.onInit()
    }

    override fun onHook() {
        fillPhone.hookBefore(this) { param ->
            if (param.args.getOrNull(0) != PHONE_PAGE_STATUS) return@hookBefore

            val bundle = param.args.getOrNull(2) as? Bundle ?: return@hookBefore
            bundle.putString(BUNDLE_KEY_PHONE, config.phone.ifEmpty { DEFAULT_PHONE })
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        FakePhonePage(
            currentConfig = config,
            onSave = ::updateConfig,
            onDismiss = onDismiss
        )
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(
        "fillPhone" to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.app")
            matcher {
                paramTypes = listOf("int", "boolean", "java.lang.Object")
                usingEqStrings("status", "wording", "target_desc", "target_name")
            }
        }
    )

    /** 页面状态：5 对应手机号那一栏。 */
    private const val PHONE_PAGE_STATUS = 5

    private const val BUNDLE_KEY_PHONE = "phone"

    /** 没填时显示什么。这个值本身没有含义，只是明显是假的，方便确认功能生效。 */
    private const val DEFAULT_PHONE = "1145141919810"
}
