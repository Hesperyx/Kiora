package dev.ujhhgtg.wekit.dexkit.cache

import cn.hxy.kiora.utils.dexkit.DexKitCache
import java.io.File

/**
 * WeKit 血统功能的 DexKit 结果缓存。
 *
 * 已并入 Kiora 主缓存 [DexKitCache]：共用同一份 [cacheMap] 与 [cacheFile]
 * （`CacheMap_<versionCode>_<VERSION_CODE>`，不再有独立的 `_wx_` 文件）。
 * 键空间靠 `->` 前缀天然区分、互不冲突：
 * - 主框架键 = `英文任务类名 -> 查询名`（如 `WeChatDexKit->AntiRevoke1.MethodDoRevokeMsg`）
 * - WeKit 键  = `中文 technicalId -> 委托属性名`（如 `消息发送服务->classChattingDataAdapter`）
 *
 * 云端报告导出与拉取都基于这份合并后的 map，因此主框架与 WeKit 共享同一套云端同步。
 */
object WxDexCache {

    val cacheMap: MutableMap<String, String>
        get() = DexKitCache.cacheMap

    val cacheFile: File
        get() = DexKitCache.cacheFile

    /** 主框架在 Startup 已同步 initCache；此处委托，重复 load 同一文件无害。 */
    fun initCache(): Boolean = DexKitCache.initCache()

    fun saveCache() = DexKitCache.saveCache()
}
