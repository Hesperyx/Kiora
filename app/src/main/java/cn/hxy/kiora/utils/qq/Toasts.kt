package cn.hxy.kiora.utils.qq

import android.widget.Toast
import com.tencent.util.QQToastUtil
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.common.ModuleScope

object Toasts {

    fun toast(message: String) {
        ModuleScope.launchMain {
            QQCurrentEnv.activity?.let {
                Toast.makeText(it, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun qqToast(icon: Int, message: String?) {
        message ?: return
        // QQToastUtil 是 QQ 专属类，微信等非 QQ 宿主没有，直接引用会抛
        // NoClassDefFoundError（Error，catch Exception 接不住）。按宿主分流：
        // 只有 QQ/TIM 才用 QQ 图标 Toast，其余走通用 [toast]。
        if (HostInfo.isWeChat || !HostInfo.isInHostProcess) {
            toast(message)
            return
        }
        try {
            QQToastUtil.showQQToastInUiThread(icon, message)
        } catch (_: Throwable) {
        }
    }
}
