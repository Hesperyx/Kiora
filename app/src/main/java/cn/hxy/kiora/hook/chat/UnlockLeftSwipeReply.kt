package cn.hxy.kiora.hook.chat

import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.replaceFirstParam
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.toClass
import java.lang.reflect.Method

@HookItemAnnotation(
    "解除左滑回复限制",
    "解除部分类型消息无法左滑回复的限制",
    HookCategory.CHAT
)
object UnlockLeftSwipeReply : BaseSwitchHookItem() {

    private lateinit var setSwipeEnable: Method

    override fun onInit(): Boolean {
        setSwipeEnable = "com.tencent.mobileqq.aio.msglist.holder.component.leftswipearea.AIOContentLeftSwipeHelper".toClass
            .findMethod {
                returnType = void
                paramTypes(boolean)
            }
        return super.onInit()
    }

    override fun onHook() {
        setSwipeEnable.replaceFirstParam(true, this)
    }
}