package cn.hxy.kiora.hook.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import cn.hxy.kiora.hook.base.BaseHookItem
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.reflect.callMethod
import cn.hxy.kiora.utils.reflect.clazz
import cn.hxy.kiora.utils.reflect.getObjectByTypeOrNull
import java.lang.reflect.Method
import java.util.WeakHashMap

/**
 * 在通知构建路径上捕获原始通知关联的会话信息与跳转 Intent，
 * 供 [MessagingNotificationBuilder] 重建 MessagingStyle 通知，移植自 TCQT（思路参考 QAuxiliary）。
 */
internal class MessagingNotificationCapture(private val owner: BaseHookItem) {

    private val notificationInfoMap = WeakHashMap<Any, Pair<Any, Intent>>()
    private val notificationElementBuilderMap = WeakHashMap<Any, Any>()

    fun take(notification: Notification): Pair<Any, Intent>? {
        return notificationInfoMap.remove(notification.contentIntent)
    }

    fun hookBuildPaths(
        notificationFacade: Class<*>,
        appRuntimeClass: Class<*>,
        commonInfoClass: Class<*>,
        recentInfoClass: Class<*>
    ): Boolean {
        val newPathHooked = hookNotificationElementBuilderPath(appRuntimeClass, recentInfoClass)
        val legacyPathHooked = hookLegacyNotificationPath(
            notificationFacade,
            appRuntimeClass,
            commonInfoClass,
            recentInfoClass
        )
        return newPathHooked || legacyPathHooked
    }

    private fun hookNotificationElementBuilderPath(
        appRuntimeClass: Class<*>,
        recentInfoClass: Class<*>
    ): Boolean {
        val elementBuilderClass = "com.tencent.qqnt.notification.struct.NotificationElementBuilder".clazz
            ?: return false

        val constructor = elementBuilderClass.declaredConstructors.firstOrNull {
            val params = it.parameterTypes
            params.size >= 2 && params[0] == appRuntimeClass && params[1] == recentInfoClass
        }?.apply { isAccessible = true } ?: return false

        val buildElement = elementBuilderClass.declaredMethods.firstOrNull {
            it.parameterCount == 0 && it.returnType.name == "com.tencent.qqnt.notification.struct.d"
        }?.apply { isAccessible = true } ?: return false

        constructor.hookAfter(owner) { param ->
            notificationElementBuilderMap[param.thisObject] = param.args.getOrNull(1) ?: return@hookAfter
        }

        buildElement.hookAfter(owner) { param ->
            val element = param.result ?: return@hookAfter
            val recentInfo = notificationElementBuilderMap[param.thisObject] ?: return@hookAfter
            val intent = runCatching { element.callMethod("f") }.getOrNull() as? Intent
                ?: element.getObjectByTypeOrNull<Intent>()
                ?: return@hookAfter
            val pendingIntent = runCatching { element.callMethod("h") }.getOrNull() as? PendingIntent
                ?: element.getObjectByTypeOrNull<PendingIntent>()
                ?: return@hookAfter
            notificationInfoMap[pendingIntent] = Pair(recentInfo, intent)
        }

        return true
    }

    private fun hookLegacyNotificationPath(
        clazz: Class<*>,
        appRuntimeClass: Class<*>,
        commonInfoClass: Class<*>,
        recentInfoClass: Class<*>
    ): Boolean {
        val recentInfoBuilder = findRecentInfoBuilderMethod(clazz, appRuntimeClass, recentInfoClass)
            ?: return false
        val buildNotification = findBuildNotificationMethod(
            clazz,
            appRuntimeClass,
            commonInfoClass,
            recentInfoClass
        ) ?: return false

        recentInfoBuilder.hookAfter(owner) { param ->
            val result = param.result ?: return@hookAfter
            val intent = result.getObjectByTypeOrNull<Intent>() ?: return@hookAfter
            notificationInfoMap[result] = Pair(param.args[1] ?: return@hookAfter, intent)
        }

        buildNotification.hookBefore(owner) { param ->
            val element = param.args.getOrNull(1) ?: return@hookBefore
            val pendingIntent = element.getObjectByTypeOrNull<PendingIntent>() ?: return@hookBefore
            notificationInfoMap[pendingIntent] = notificationInfoMap[element] ?: return@hookBefore
            notificationInfoMap.remove(element)
        }

        return true
    }

    private fun findBuildNotificationMethod(
        clazz: Class<*>,
        appRuntimeClass: Class<*>,
        commonInfoClass: Class<*>,
        recentInfoClass: Class<*>
    ): Method? {
        return clazz.declaredMethods.firstOrNull {
            val params = it.parameterTypes
            params.isNotEmpty() &&
                    params[0] == appRuntimeClass &&
                    (
                            params.size == 3 && params[2] == commonInfoClass ||
                                    params.size == 4 && params[2] == commonInfoClass && params[3] == recentInfoClass ||
                                    params.size == 5 && params[2] == commonInfoClass && params[3] == recentInfoClass && params[4] == Boolean::class.javaPrimitiveType
                            )
        }?.apply { isAccessible = true }
    }

    private fun findRecentInfoBuilderMethod(
        clazz: Class<*>,
        appRuntimeClass: Class<*>,
        recentInfoClass: Class<*>
    ): Method? {
        return clazz.declaredMethods.firstOrNull {
            val params = it.parameterTypes
            params.size >= 3 &&
                    params[0] == appRuntimeClass &&
                    params[1] == recentInfoClass &&
                    params[2] == Boolean::class.javaObjectType
        }?.apply { isAccessible = true }
    }
}

internal fun findPostNotificationMethod(clazz: Class<*>): Pair<Method, Int>? {
    clazz.declaredMethods.firstOrNull {
        val params = it.parameterTypes
        params.size == 2 &&
                params[0] == Notification::class.java &&
                params[1] == Int::class.javaPrimitiveType
    }?.let { method ->
        method.isAccessible = true
        return method to 0
    }

    clazz.declaredMethods.firstOrNull {
        val params = it.parameterTypes
        params.size == 3 &&
                params[0] == String::class.java &&
                params[1] == Notification::class.java &&
                params[2] == Int::class.javaPrimitiveType
    }?.let { method ->
        method.isAccessible = true
        return method to 1
    }

    return null
}
