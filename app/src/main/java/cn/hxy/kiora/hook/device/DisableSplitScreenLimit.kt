package cn.hxy.kiora.hook.device

import android.app.Activity
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.utils.reflect.findMethod

@HookItemAnnotation(
    "伪装处于非多窗口模式",
    "解除分屏状态下扫码等功能的使用限制",
    HookCategory.DEVICE,
    "All"
)
object DisableSplitScreenLimit : BaseSwitchHookItem() {

    override fun onHook() {
        Activity::class.java
            .findMethod {
                name = "isInMultiWindowMode"
            }
            .returnConstant(this, false)
    }
}