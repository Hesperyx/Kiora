package cn.hxy.kiora.hook.qrcode

import android.os.Bundle
import com.tencent.biz.qrcode.activity.QRLoginAuthActivity
import com.tencent.biz.qui.quibutton.QUIButton
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.reflect.getObjectByType

@HookItemAnnotation(
    "跳过扫码确认等待时间",
    "可忽略倒计时，直接点击确认即可",
    HookCategory.OTHER
)
object SkipScanWaitTime : BaseSwitchHookItem() {

    override fun onHook() {

        QRLoginAuthActivity::class.java
            .getDeclaredMethod("doOnCreate", Bundle::class.java)
            .hookAfter(this) { param ->
                val activity = param.thisObject
                val confirmButton = activity.getObjectByType<QUIButton>()

                ModuleScope.launchDelayed(100) {
                    confirmButton.isEnabled = true
                    confirmButton.setType(0)
                }

            }
    }
}