package cn.hxy.kiora.hook.msg

import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.api.AIOMarkerContainer
import cn.hxy.kiora.hook.api.AIOViewUpdateListener
import cn.hxy.kiora.hook.api.GetMsgRecordListener
import cn.hxy.kiora.hook.base.BaseSwitchHookItem

@HookItemAnnotation(
    "闪照破解",
    "以图片方式直接显示闪照",
    HookCategory.MSG
)
object FlashPicDecrypt : BaseSwitchHookItem(), GetMsgRecordListener, AIOViewUpdateListener {

    private const val MARKER = "闪照"

    override fun onGet(msgRecord: MsgRecord) {
        if (!isEnable) return
        val subMsgType = msgRecord.subMsgType
        if (subMsgType == 8194) {
            msgRecord.subMsgType = subMsgType and 8192.inv()
            msgRecord.guildName = MARKER
        }
    }

    override fun onUpdate(frameLayout: FrameLayout, msgRecord: MsgRecord) {

        if (msgRecord.msgType != 2) return

        val isFlash = MARKER == msgRecord.guildName

        val tagView = AIOMarkerContainer.find(frameLayout)
            ?.findViewWithTag<TextView>("FlashMarker")

        if (isFlash) {
            val container = AIOMarkerContainer.get(frameLayout)
            if (tagView == null) {
                container.addView(TextView(frameLayout.context).apply {
                    text = MARKER
                    textSize = 15f
                    setTextColor(Color.BLUE)
                    tag = "FlashMarker"
                })
            } else {
                tagView.visibility = View.VISIBLE
            }
        } else {
            tagView?.visibility = View.GONE
        }
    }
}