package cn.hxy.kiora.hook.purify

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.doNothing
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.clazz
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.toClass

@HookItemAnnotation(
    "屏蔽弹出动画",
    "屏蔽某些特定文本消息弹出的与游戏相关的烦人动画",
    HookCategory.PURIFY
)
object AntiInteractivePop : BaseSwitchHookItem() {

    override fun onInit() = HostInfo.isQQ

    override fun onHook() {
        listOf(
            "com.tencent.mobileqq.springhb.interactive.ui.InteractivePopManager",
            "com.tencent.mobileqq.aio.animation.pag.PagEasterEggPopManager"
        ).forEach {
            it.clazz?.findMethod {
                returnType = void
                paramTypes(
                    "androidx.fragment.app.Fragment".toClass,
                    null,
                    null,
                    "kotlin.jvm.functions.Function0".toClass
                )
            }?.doNothing(this)
        }
    }
}