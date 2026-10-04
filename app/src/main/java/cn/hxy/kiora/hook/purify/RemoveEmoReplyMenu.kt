package cn.hxy.kiora.hook.purify

import com.tencent.mobileqq.aio.msg.AIOMsgItem
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.doNothing
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.findMethod
import cn.hxy.kiora.utils.reflect.toClass
import java.lang.reflect.Method

@HookItemAnnotation(
    "移除表情回应",
    "移除长按出现的表情回应菜单以及消息下方表情回应",
    HookCategory.PURIFY
)
object RemoveEmoReplyMenu : BaseSwitchHookItem() {

    private lateinit var handleUIState: Method

    private lateinit var getMenuView: Method

    override fun onInit(): Boolean {
        handleUIState = "com.tencent.mobileqq.aio.msglist.holder.component.msgtail.AIOGeneralMsgTailContentComponent".toClass
            .findMethod {
                returnType = void
                paramTypes(int, AIOMsgItem::class.java, list)
            }
        getMenuView = "com.tencent.qqnt.aio.api.impl.AIOEmoReplyMenuApiImpl".toClass
            .findMethod {
                name =
                    if (HostInfo.versionCode >= 9898) "getSeparateEmoReplyMenuView" else "getEmoReplyMenuView"
            }
        return super.onInit()
    }

    override fun onHook() {
        getMenuView.doNothing(this)
        handleUIState.doNothing(this)
    }

}