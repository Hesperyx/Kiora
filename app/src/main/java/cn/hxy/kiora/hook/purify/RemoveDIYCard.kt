package cn.hxy.kiora.hook.purify

import com.tencent.mobileqq.profilecard.activity.FriendProfileCardActivity
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.doNothing
import cn.hxy.kiora.host.HostInfo

@HookItemAnnotation(
    "屏蔽DIY名片",
    "屏蔽查看好友页面自定义的名片",
    HookCategory.PURIFY
)
object RemoveDIYCard : BaseSwitchHookItem() {

    override fun onInit() = HostInfo.isQQ
    
    override fun onHook() {
        FriendProfileCardActivity::class.java
            .getDeclaredMethod("handleSwitchVasCard")
            .doNothing(this)
    }

}