package cn.hxy.kiora.hook.msg

import com.tencent.qqnt.aio.adapter.api.impl.RichMediaBrowserApiImpl
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.returnConstant
import cn.hxy.kiora.utils.reflect.findMethod

@HookItemAnnotation(
    "以图片方式打开表情",
    "将表情包作为普通图片打开，方便下载",
    HookCategory.MSG
)
object EmotionToPic : BaseSwitchHookItem() {

    override fun onHook() {
        RichMediaBrowserApiImpl::class.java
            .findMethod {
                name = "checkIsFavPicAndShowPreview"
            }
            .returnConstant(this, false)
    }
}