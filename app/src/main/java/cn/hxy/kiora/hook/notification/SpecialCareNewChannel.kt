package cn.hxy.kiora.hook.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.setObject

/** 把特别关心消息的通知改投到独立渠道，移植自 TCQT（思路参考 QAuxiliary）。 */
@HookItemAnnotation(
    "特别关心通知单独分组",
    "将特别关心好友的消息通知移动到单独的通知渠道",
    HookCategory.NOTIFICATION,
    "All"
)
object SpecialCareNewChannel : BaseSwitchHookItem() {

    override fun onHook() {
        NotificationManager::class.java.declaredMethods
            .filter { it.name == "notify" && it.parameterTypes.lastOrNull() == Notification::class.java }
            .forEach { method ->
                method.isAccessible = true
                method.hookBefore(this) { param ->
                    val notification = param.args.lastOrNull() as? Notification ?: return@hookBefore
                    val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                    if (!title.contains("[特别关心]")) return@hookBefore

                    ensureSpecialCareChannel()
                    runCatching { notification.setObject("mChannelId", CHANNEL_ID_SPECIALLY_CARE) }
                    param.args[param.args.lastIndex] = notification
                }
            }
    }

    private fun ensureSpecialCareChannel() {
        val notificationManager = HostInfo.hostContext.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID_SPECIALLY_CARE) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID_SPECIALLY_CARE,
            "特别关心",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    private const val CHANNEL_ID_SPECIALLY_CARE = "CHANNEL_ID_SPECIALLY_CARE"
}
