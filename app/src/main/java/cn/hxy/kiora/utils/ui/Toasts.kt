package cn.hxy.kiora.utils.ui

import android.widget.Toast
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.host.HostEnv
import cn.hxy.kiora.host.HostInfo

/**
 * 提示条。宿主无关。
 *
 * 带图标的原生 Toast 由 [cn.hxy.kiora.host.IHostAdapter.showIconToast] 提供，
 * 宿主不支持时自动回退通用 Toast —— 这里不再需要判断「我是哪个宿主」。
 */
object Toasts {

    fun toast(message: String) {
        ModuleScope.launchMain {
            HostEnv.activity?.let {
                Toast.makeText(it, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 带图标的宿主原生 Toast；宿主不支持时回退 [toast]。
     *
     * 原先这里直接引用 QQ 的 `QQToastUtil`，微信下会抛 `NoClassDefFoundError`
     * （Error，`catch Exception` 接不住），所以只能靠 `HostInfo.isWeChat` 硬分流。
     * 现在把那个 QQ 类引用挪进 `QQFamilyHostAdapter.showIconToast`，
     * 这里只问适配器「能不能处理」。
     */
    fun iconToast(icon: Int, message: String?) {
        message ?: return
        if (HostInfo.adapter?.showIconToast(icon, message) == true) return
        toast(message)
    }
}
