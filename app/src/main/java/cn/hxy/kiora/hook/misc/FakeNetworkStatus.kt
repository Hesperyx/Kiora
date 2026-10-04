@file:Suppress("DEPRECATION")

package cn.hxy.kiora.hook.misc

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.qq.conf.FakeNetworkConfig
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.qq.ui.FakeNetworkStatusPage
import cn.hxy.kiora.utils.hook.hookBefore
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.callOriginal
import androidx.compose.runtime.Composable

/** 伪装网络状态，移植自 TCQT FakeNetworkStatus。 */
@HookItemAnnotation(
    "伪装网络状态",
    "将网络类型伪装为指定的 WIFI / 5G / 4G",
    HookCategory.MISC,
    "All"
)
object FakeNetworkStatus : BaseClickableHookItem<FakeNetworkConfig>(FakeNetworkConfig.serializer()) {

    override val isNeedRestart: Boolean = true

    override val defaultConfig: FakeNetworkConfig = FakeNetworkConfig()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onHook() {
        hookConnectivityManager()
        hookTelephonyManager()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun hookConnectivityManager() {
        val cm = ConnectivityManager::class.java

        cm.getMethod("getActiveNetworkInfo").hookBefore(this) { param ->
            val real = param.method.callOriginal(param.thisObject) as? NetworkInfo
            if (real != null) {
                applyFakeNetworkInfo(real, config.mode)
                param.result = real
            }
        }

        runCatching {
            cm.getMethod("getNetworkCapabilities", Network::class.java).hookBefore(this) { param ->
                val real = param.method.callOriginal(param.thisObject, *param.args) as? NetworkCapabilities
                if (real != null) {
                    applyFakeTransport(real, config.mode)
                    param.result = real
                }
            }
        }

        runCatching {
            cm.getMethod("getDefaultNetworkCapabilitiesForActiveNetwork").hookBefore(this) { param ->
                val real = param.method.callOriginal(param.thisObject) as? NetworkCapabilities
                if (real != null) {
                    applyFakeTransport(real, config.mode)
                    param.result = real
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun hookTelephonyManager() {
        TelephonyManager::class.java.methods
            .filter { it.name == "getNetworkType" || it.name == "getDataNetworkType" }
            .forEach { method ->
                method.hookBefore(this) { param ->
                    param.result = telephonyNetworkType(config.mode)
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun telephonyNetworkType(mode: Int): Int = when (mode) {
        FakeNetworkConfig.MODE_5G -> TelephonyManager.NETWORK_TYPE_NR
        FakeNetworkConfig.MODE_4G -> TelephonyManager.NETWORK_TYPE_LTE
        else -> TelephonyManager.NETWORK_TYPE_UNKNOWN
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun applyFakeNetworkInfo(info: NetworkInfo, mode: Int) {
        val type: Int
        val subtype: Int
        val typeName: String
        val subtypeName: String
        when (mode) {
            FakeNetworkConfig.MODE_WIFI -> {
                type = ConnectivityManager.TYPE_WIFI
                subtype = 0
                typeName = "WIFI"
                subtypeName = ""
            }

            FakeNetworkConfig.MODE_5G -> {
                type = ConnectivityManager.TYPE_MOBILE
                subtype = TelephonyManager.NETWORK_TYPE_NR
                typeName = "MOBILE"
                subtypeName = "NR"
            }

            FakeNetworkConfig.MODE_4G -> {
                type = ConnectivityManager.TYPE_MOBILE
                subtype = TelephonyManager.NETWORK_TYPE_LTE
                typeName = "MOBILE"
                subtypeName = "LTE"
            }

            else -> return
        }
        try {
            setNetworkInfoField(info, "mNetworkType", type)
            setNetworkInfoField(info, "mSubtype", subtype)
            setNetworkInfoField(info, "mTypeName", typeName)
            setNetworkInfoField(info, "mSubtypeName", subtypeName)
        } catch (e: Throwable) {
            LogUtils.e("FakeNetworkStatus", e)
        }
    }

    private fun setNetworkInfoField(info: NetworkInfo, name: String, value: Any) {
        val field = NetworkInfo::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(info, value)
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun applyFakeTransport(nc: NetworkCapabilities, mode: Int) {
        try {
            val transportField = NetworkCapabilities::class.java.getDeclaredField("mTransportTypes")
            transportField.isAccessible = true
            transportField.setLong(
                nc,
                if (mode == FakeNetworkConfig.MODE_WIFI) {
                    1L shl NetworkCapabilities.TRANSPORT_WIFI
                } else {
                    1L shl NetworkCapabilities.TRANSPORT_CELLULAR
                }
            )

            val capField = NetworkCapabilities::class.java.getDeclaredField("mNetworkCapabilities")
            capField.isAccessible = true
            val caps = capField.getLong(nc)
            val notMeteredBit = 1L shl NetworkCapabilities.NET_CAPABILITY_NOT_METERED
            capField.setLong(
                nc,
                if (mode == FakeNetworkConfig.MODE_WIFI) caps or notMeteredBit else caps and notMeteredBit.inv()
            )
        } catch (e: Throwable) {
            LogUtils.e("FakeNetworkStatus", e)
        }
    }

    @Composable
    override fun ConfigContent(onDismiss: () -> Unit) {
        FakeNetworkStatusPage(
            currentConfig = config,
            onSave = ::updateConfig,
            onDismiss = onDismiss
        )
    }
}
