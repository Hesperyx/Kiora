package cn.hxy.kiora.hook.notification

import android.app.Notification
import android.app.NotificationManager
import androidx.compose.runtime.Composable
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.conf.MessagingStyleConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.ui.pages.configs.MessagingStylePage
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.qq.HostInfo
import cn.hxy.kiora.utils.reflect.clazz

/** MessagingStyle 通知主控：替换通知构建产物 + 渠道治理，移植自 TCQT（思路参考 QAuxiliary）。 */
@HookItemAnnotation(
    "MessagingStyle 通知",
    "更加优雅的通知样式，致敬 QQ Helper。",
    HookCategory.NOTIFICATION,
    "All"
)
object MessagingStyleNotification :
    BaseClickableHookItem<MessagingStyleConfig>(MessagingStyleConfig.serializer()) {

    override val defaultConfig: MessagingStyleConfig = MessagingStyleConfig()

    private val notificationCapture by lazy { MessagingNotificationCapture(this) }
    private val notificationBuilder by lazy {
        MessagingNotificationBuilder(
            disableConversationSubChannel = { disableConversationSubChannel },
            disableBubble = { disableBubble }
        )
    }

    private val disableConversationSubChannel: Boolean
        get() = config.disableSubChannel || config.autoClearSubChannel

    private val disableBubble: Boolean
        get() = config.disableBubble

    private val autoClearConversationSubChannel: Boolean
        get() = config.autoClearSubChannel

    override fun onHook() {
        createNotificationChannels()

        val notificationFacade = "com.tencent.qqnt.notification.NotificationFacade".clazz
        val appRuntimeClass = "mqq.app.AppRuntime".clazz
        val commonInfoClass = "com.tencent.qqnt.kernel.nativeinterface.NotificationCommonInfo".clazz
        val recentInfoClass = "com.tencent.qqnt.kernel.nativeinterface.RecentContactInfo".clazz
        if (notificationFacade == null || appRuntimeClass == null || commonInfoClass == null || recentInfoClass == null) {
            return
        }

        val postTarget = findPostNotificationMethod(notificationFacade) ?: return

        val buildPathHooked = notificationCapture.hookBuildPaths(
            notificationFacade,
            appRuntimeClass,
            commonInfoClass,
            recentInfoClass
        )
        if (!buildPathHooked) {
            LogUtils.w("MessagingStyleNotification skipped: notification build path not found")
            return
        }

        postTarget.first.hookBefore(this) { param ->
            val oldNotification = param.args[postTarget.second] as? Notification ?: return@hookBefore
            val pair = notificationCapture.take(oldNotification) ?: return@hookBefore
            val newNotification = runCatching {
                notificationBuilder.createNotification(pair.first, pair.second, oldNotification)
            }.onFailure {
                LogUtils.w("MessagingStyleNotification replace failed", it)
            }.getOrNull() ?: return@hookBefore

            param.args[postTarget.second] = newNotification
        }

        postTarget.first.hookAfter(this) {
            if (autoClearConversationSubChannel) {
                notificationBuilder.clearRedundantConversationChannels()
            }
        }

        "com.tencent.commonsdk.util.notification.QQNotificationManager".clazz
            ?.declaredMethods
            ?.firstOrNull { it.name == "cancelAll" && it.parameterCount == 0 }
            ?.apply { isAccessible = true }
            ?.hookBefore(this) {
                notificationBuilder.clearHistory()
            }
    }

    private fun createNotificationChannels() {
        runCatching {
            val notificationManager =
                HostInfo.hostContext.getSystemService(NotificationManager::class.java)
            ensureNotificationChannels(notificationManager)
        }.onFailure {
            LogUtils.w("MessagingStyleNotification create channels failed", it)
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        MessagingStylePage(
            currentConfig = config,
            onSave = ::updateConfig,
            onDismiss = onDismiss
        )
    }
}
