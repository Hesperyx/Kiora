package cn.hxy.kiora.host

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import cn.hxy.kiora.utils.reflect.callStaticMethod
import cn.hxy.kiora.utils.reflect.getObject
import cn.hxy.kiora.utils.reflect.toClass

/**
 * 宿主运行时环境：与具体宿主无关的通用入口。
 *
 * 与 [HostInfo] 的分工：`HostInfo` 持有宿主**身份**（包名 / 进程 / 版本 / 适配器），
 * 本对象提供宿主**运行时**信息（当前 Activity、全局偏好、账号与数据目录）。
 *
 * 这些成员原先散在 `utils/qq/QQCurrentEnv` 里，导致微信侧和 `utils/io`、`utils/log`
 * 等共享代码都得 import `utils.qq`。抽出来之后，宿主专属的 QQ 工具才能干净地收进 `qq` 包。
 */
@Suppress("DEPRECATION")
object HostEnv {

    /** 全局偏好（跨账号共享）。 */
    val globalPreference: SharedPreferences by lazy {
        HostInfo.hostContext.getSharedPreferences(
            "Kiora_Config_global",
            Context.MODE_MULTI_PROCESS
        )
    }

    /** 账号级偏好：原生开关与 WeKit 功能开关统一读写这里。 */
    val accountPreference: SharedPreferences by lazy {
        HostInfo.hostContext.getSharedPreferences(
            "Kiora_Config_${HostInfo.adapter?.currentAccount ?: currentAccount}",
            Context.MODE_MULTI_PROCESS
        )
    }

    /** 当前前台 Activity；读不到返回 null。纯反射读 `ActivityThread.mActivities`，与宿主无关。 */
    val activity: Activity?
        get() = runCatching {
            val activityThreadClass = "android.app.ActivityThread".toClass
            val activityThread = activityThreadClass.callStaticMethod("currentActivityThread")
            val activities = activityThread?.getObject("mActivities") as? Map<*, *>
                ?: return@runCatching null

            activities.values.firstNotNullOfOrNull { record ->
                record ?: return@firstNotNullOfOrNull null

                val paused = record.getObject("paused") as Boolean
                if (!paused) record.getObject("activity") as Activity else null
            }
        }.getOrNull()

    /**
     * 当前账号。
     *
     * 优先问宿主适配器；模块自身进程（adapter 为 null）退回全局偏好里记录的值。
     */
    val currentAccount: String
        get() = HostInfo.adapter?.currentAccount
            ?: globalPreference.getString("currentUin", "global").orEmpty()

    /** 当前账号的数据目录。 */
    val currentDir: String
        get() = "${HostInfo.moduleDataPath}${currentAccount}/"
}
