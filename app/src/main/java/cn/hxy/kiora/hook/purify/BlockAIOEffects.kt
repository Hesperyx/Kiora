package cn.hxy.kiora.hook.purify

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.findMethods
import cn.hxy.kiora.utils.reflect.toClass

@HookItemAnnotation(
    "屏蔽AIO动画特效",
    "屏蔽聊天界面中的关键词彩蛋、表情连击、超级表情等全屏动画",
    HookCategory.PURIFY
)
object BlockAIOEffects : BaseSwitchHookItem() {

    override fun onInit() = HostInfo.isQQ

    override fun onHook() {
        "com.tencent.mobileqq.aio.animation.AIOAnimationContainer".toClass
            .findMethods {
                returnType = boolean
                paramTypes(int, int, objArr)
            }
            .forEach { it.returnConstant(this, true) }
    }
}