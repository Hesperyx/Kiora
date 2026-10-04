package dev.ujhhgtg.wekit.dexkit.cache

import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.io.ObjectStore
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

/**
 * WeKit 血统功能的 DexKit 结果缓存。
 *
 * 与 Kiora 主缓存 [cn.hxy.kiora.utils.dexkit.DexKitCache] 同构：JSON 序列化
 * `Map<String,String>`（key→descriptor，空串=「查过没找到」），文件落
 * `moduleDataPath/global/dexkit/`，文件名含宿主 versionCode + 模块 VERSION_CODE，
 * 因此宿主升级或模块发版都会**整体失效**。
 *
 * 与主缓存的区别只在文件名加了 `_wx_` 前缀，避免和 Kiora 主缓存（`CacheMap_...`）
 * 撞名。
 *
 * 失效策略刻意**不复刻** WeKit 原版的 methodHash（构建期对 Feature 源码做 MD5）：
 * 那需要一套构建期代码生成，而「版本整体失效」在当前阶段（几十个功能、改动不频繁）
 * 足够。改某个 feature 的 matcher 后旧缓存不自动失效 —— 删掉本文件或升模块版本即可。
 */
object WxDexCache {

    /** 全局键 → descriptor。键形如 `technicalId->propertyName`，空串=查过没找到。 */
    var cacheMap = mutableMapOf<String, String>()

    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    val cacheFile by lazy {
        File(
            "${HostInfo.moduleDataPath}/global/dexkit",
            "CacheMap_wx_${HostInfo.versionCode}_${BuildConfig.VERSION_CODE}"
        )
    }

    /** 读缓存文件。成功返回 true；文件不存在或反序列化失败返回 false，[cacheMap] 留空。 */
    fun initCache(): Boolean {
        cacheMap = ObjectStore.load(cacheFile, mapSerializer)?.toMutableMap() ?: return false
        return true
    }

    fun saveCache() = ObjectStore.save(cacheFile, cacheMap.toMap(), mapSerializer)
}
