package cn.hxy.kiora.qq.util

import com.tencent.mobileqq.app.BusinessHandler
import com.tencent.mobileqq.app.QQAppInterface
import com.tencent.mobileqq.qroute.QRoute
import com.tencent.mobileqq.qroute.QRouteApi
import com.tencent.qqnt.kernel.api.IKernelService
import com.tencent.qqnt.kernel.api.impl.KernelServiceImpl
import cn.hxy.kiora.host.HostEnv
import mqq.app.MobileQQ
import mqq.app.api.IRuntimeService

/**
 * QQ / TIM 专属的宿主环境。
 *
 * 宿主无关的部分（当前 Activity、全局偏好、账号与数据目录）已抽到
 * [cn.hxy.kiora.host.HostEnv]；这里只留 QQ 独有的：`QQAppInterface`、Uin、昵称、
 * 内核服务，以及三个反射入口。
 */
@Suppress("DEPRECATION")
object QQCurrentEnv {

    val qQAppInterface
        get() = MobileQQ.getMobileQQ().peekAppRuntime() as QQAppInterface

    val currentUin: String
        get() = runCatching {
            qQAppInterface.currentUin
        }.getOrElse {
            HostEnv.globalPreference.getString("currentUin", "global")!!
        }

    val currentNickName: String?
        get() = qQAppInterface.currentNickname

    val kernelMsgService
        get() = runCatching {

            val kernelService = runtime<IKernelService>() as KernelServiceImpl
            kernelService.msgService.service

        }.getOrNull()

    val kernelGroupService
        get() = runCatching {

            val kernelService = runtime<IKernelService>() as KernelServiceImpl
            kernelService.groupService.service

        }.getOrNull()

}


internal inline fun <reified T : QRouteApi> api(): T {
    return QRoute.api(T::class.java)
}

internal inline fun <reified T : IRuntimeService> runtime(): T {
    return QQCurrentEnv.qQAppInterface.getRuntimeService(T::class.java, "")
}

internal inline fun <reified T : BusinessHandler> handler(): BusinessHandler {
    return QQCurrentEnv.qQAppInterface.getBusinessHandler(T::class.java.name)
}
