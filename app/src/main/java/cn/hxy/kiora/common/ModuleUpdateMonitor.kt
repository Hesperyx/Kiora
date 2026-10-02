package cn.hxy.kiora.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.utils.qq.HostInfo

object ModuleUpdateMonitor {

    private var isInitialized = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REPLACED) {
                val packageName = intent.data?.schemeSpecificPart ?: return
                if (packageName == BuildConfig.APPLICATION_ID) {
                    // 重启通路各宿主不通用，交给 adapter 分发（见 IHostAdapter.restartHost）。
                    HostInfo.adapter?.restartHost(context, "模块更新中...")
                }
            }
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }
}