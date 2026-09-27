package cn.hxy.kiora.utils.dexkit

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.utils.io.ObjectStore
import cn.hxy.kiora.utils.qq.HostInfo
import cn.hxy.kiora.utils.reflect.ClassUtils
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexMethod
import java.io.File
import java.lang.reflect.Method

object DexKitCache {

    /**
     * key -> descriptor。
     *
     * 值可能是空串，含义是「查过了，没找到」—— 与「还没查过」必须区分开，
     * 否则查不到的键会永远算缺失，每次冷启动都要重查一遍再把用户踢去重启。
     */
    var cacheMap = mutableMapOf<String, String>()

    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    val cacheFile by lazy {
        File(
            "${HostInfo.moduleDataPath}/global/dexkit",
            "CacheMap_${HostInfo.versionCode}_${BuildConfig.VERSION_CODE}"
        )
    }

    fun initCache(): Boolean {
        cacheMap = ObjectStore.load(cacheFile, mapSerializer)?.toMutableMap() ?: return false
        return true
    }

    fun saveCache() = ObjectStore.save(cacheFile, cacheMap.toMap(), mapSerializer)

    fun getClass(key: String): Class<*> =
        cacheMap[key]?.takeIf { it.isNotEmpty() }?.let {
            DexClass(it).getInstance(ClassUtils.hostClassLoader)
        } ?: throw ClassNotFoundException(key)

    fun getMethod(key: String): Method =
        cacheMap[key]?.takeIf { it.isNotEmpty() }?.let {
            DexMethod(it).getMethodInstance(ClassUtils.hostClassLoader)
        } ?: throw NoSuchMethodException(key)
}
