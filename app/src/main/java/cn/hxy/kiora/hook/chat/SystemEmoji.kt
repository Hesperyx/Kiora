package cn.hxy.kiora.hook.chat

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.toClass

@HookItemAnnotation(
    "强制使用系统Emoji",
    "屏蔽QQ自带的Emoji表情，强制显示系统Emoji",
    HookCategory.CHAT
)
object SystemEmoji : BaseSwitchHookItem() {
    override fun onHook() {
        val target = "com.tencent.mobileqq.text.EmotcationConstants".toClass
        target.findMethod { name = "getSingleEmoji" }.returnConstant(this, -1)
        target.findMethod { name = "getDoubleEmoji" }.returnConstant(this, -1)
    }
}