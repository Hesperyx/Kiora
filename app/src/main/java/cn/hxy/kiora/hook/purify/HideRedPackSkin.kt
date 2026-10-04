package cn.hxy.kiora.hook.purify

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.reflect.clazz

/** 隐藏红包推荐皮肤，移植自 TCQT HideRedPackSkin。 */
@HookItemAnnotation(
    "隐藏红包推荐皮肤",
    "隐藏点击红包按钮后出现的红包皮肤推荐",
    HookCategory.PURIFY
)
object HideRedPackSkin : BaseSwitchHookItem() {

    override fun onHook() {
        "com.tencent.mobileqq.qwallet.hb.panel.recommend.SkinRecommendViewModel".clazz
            ?.declaredMethods
            ?.singleOrNull {
                it.parameterCount == 2 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        it.parameterTypes[1].name == "kotlin.jvm.functions.Function1"
            }
            ?.hookBefore(this) { param ->
                param.result = Unit
            }
    }
}
