package cn.hxy.kiora.hook.troop

import com.tencent.mobileqq.data.troop.TroopInfo
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.utils.qq.HostInfo

@HookItemAnnotation(
    "查看被封禁群聊",
    "解除被封禁群聊无法进入查看资料的限制（仅QQ）",
    HookCategory.GROUP
)
object AllowViewBlockedTroop : BaseSwitchHookItem() {

    override fun onInit() = HostInfo.isQQ

    override fun onHook() {
        TroopInfo::class.java
            .getDeclaredMethod("isUnreadableBlock")
            .returnConstant(this, false)
    }
}