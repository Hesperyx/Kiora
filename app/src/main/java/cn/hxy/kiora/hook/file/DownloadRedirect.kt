package cn.hxy.kiora.hook.file

import android.os.Environment
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.findField
import cn.hxy.kiora.utils.reflect.toClass

@HookItemAnnotation(
    "下载重定向",
    "重定向下载路径到Download/QQ(TIM)",
    HookCategory.FILE,
    "All"
)
object DownloadRedirect : BaseSwitchHookItem() {

    override val isNeedRestart: Boolean = true

    private val redirectPath =
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${HostInfo.hostName}/"

    override fun onHook() {
        if (isEnable) {
            "com.tencent.mobileqq.app.AppConstants".toClass
                .findField {
                    name = "SDCARD_FILE_SAVE_PATH"
                }.set(null, redirectPath)
        }
    }

}