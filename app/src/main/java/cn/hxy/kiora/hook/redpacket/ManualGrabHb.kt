package cn.hxy.kiora.hook.redpacket

import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.kernel.nativeinterface.WalletElement
import mqq.app.MSFServlet
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.matchers.ClassMatcher
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.qq.conf.RedPacketConfig
import cn.hxy.kiora.hook.api.MenuClickListener
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.plugin.bean.MsgData
import cn.hxy.kiora.plugin.bean.RedPacketContext
import cn.hxy.kiora.utils.dexkit.DexKitTask

/**
 * 手动抢红包：为长按消息菜单添加「打开红包」按钮。
 *
 * 同时负责用 DexKit 定位 QWalletPbServlet（旧 API SSO 抢红包需要），
 * 结果缓存在 [servletClass] 供 [RedPacketHelper] 使用。
 */
@HookItemAnnotation(
    "手动打开红包",
    "为长按菜单添加打开红包按钮",
    HookCategory.RED_PACKET
)
object ManualGrabHb : BaseSwitchHookItem(), MenuClickListener, DexKitTask {

    private const val MENU_KEY = "[Kiora],ManualGrab,打开红包,,10"

    @Volatile
    var servletClass: Class<*>? = null
        private set

    override val menuKey: String = MENU_KEY

    override fun onInit(): Boolean {
        servletClass = runCatching { requireClass("servlet") }.getOrNull()
        return super.onInit()
    }

    override fun onClick(msgData: MsgData) {
        val msgRecord = msgData.data
        if (msgRecord.msgType == 10 && msgRecord.chatType == 2) {
            val walletElement = (msgRecord.elements[0] as MsgElement).walletElement
            RedPacketHelper.startGrab(
                RedPacketContext(
                    msgData = msgData,
                    listId = walletElement.billNo,
                    authKey = walletElement.authkey,
                    channel = walletElement.redChannel,
                    title = walletElement.receiver.title,
                    isAuto = false,
                    config = RedPacketConfig(),
                )
            )
        }
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(
        "servlet" to FindClass().apply {
            matcher {
                superClass(MSFServlet::class.java.name)
                usingStrings(
                    "QWalletHttp-QWalletPbServlet",
                    "qwallet_pb_handle_trpc_error"
                )
            }
        }
    )
}
