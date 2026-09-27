package cn.hxy.kiora.conf

import kotlinx.serialization.Serializable

@Serializable
data class FakeNetworkConfig(
    /** 1 = WIFI，2 = 5G，3 = 4G */
    val mode: Int = MODE_WIFI
) {
    companion object {
        const val MODE_WIFI = 1
        const val MODE_5G = 2
        const val MODE_4G = 3
    }
}
