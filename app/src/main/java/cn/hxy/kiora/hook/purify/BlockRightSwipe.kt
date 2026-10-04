package cn.hxy.kiora.hook.purify

import com.tencent.aio.frame.drawer.DrawerFrameViewGroup
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.replaceFirstParam
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.findMethod
import java.lang.reflect.Method

@HookItemAnnotation(
    "屏蔽聊天右滑",
    "屏蔽聊天界面右滑显示的界面",
    HookCategory.PURIFY
)
object BlockRightSwipe : BaseSwitchHookItem() {

    private lateinit var index: Method

    override fun onInit(): Boolean {
        if (HostInfo.isTIM) return false
        index = DrawerFrameViewGroup::class.java
            .findMethod {
                visibility = private
                returnType = void
                paramTypes(int)
            }
        return super.onInit()
    }

    override fun onHook() {
        index.replaceFirstParam(0, this)
    }

}