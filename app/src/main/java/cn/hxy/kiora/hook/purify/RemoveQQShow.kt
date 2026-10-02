package cn.hxy.kiora.hook.purify

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.QQCurrentEnv
import cn.hxy.kiora.utils.reflect.toClass

@HookItemAnnotation(
    "屏蔽QQ秀",
    "屏蔽新版QQ秀头像",
    HookCategory.PURIFY
)
object RemoveQQShow : BaseSwitchHookItem() {

    override fun onInit() = HostInfo.versionCode >= 11820

    override fun onHook() {

        "com.tencent.mobileqq.ai.avatar.api.impl.AIAvatarSwitchApiImpl".toClass
            .getDeclaredMethod(
                "hasAvatarUrlOrInfo",
                Long::class.java
            )
            .hookAfter(this) {
                if (it.args[0] != QQCurrentEnv.currentUin.toLong()) it.result = false
            }
    }
}