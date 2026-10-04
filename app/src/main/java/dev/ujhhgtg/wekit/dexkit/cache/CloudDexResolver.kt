package dev.ujhhgtg.wekit.dexkit.cache

import cn.hxy.kiora.utils.net.HttpUtils
import dev.ujhhgtg.wekit.BuildConfig
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.utils.HostInfo
import dev.ujhhgtg.wekit.utils.WeLogger
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 云端 DexKit 结果拉取。与 WeKit 原版同构，差异在 Kiora 适配：
 * - 网络：用 Kiora 的 [HttpUtils]（HttpURLConnection）替代 OkHttp，不引入新依赖
 * - methodHash：用模块 `BuildConfig.VERSION_CODE` 替代「构建期源码 MD5」
 * - 落盘：直接写 [WxDexCache]，命中后本地扫描即可跳过
 *
 * 托管：GitHub Release（报告名 `wechat-<version>-<channel>.json`）。
 * 通过加速镜像 `gitproxy.mrhjx.cn` 拉取（国内可达，github.com 附件直连不稳）。
 * 报告需由维护者用真机扫好后发布到 `Kiora-wechat` tag；新版本无人上传则回退本地扫描。
 */
object CloudDexResolver {
    private const val TAG = "CloudDexResolver"
    /** GitHub Release 报告的原始下载地址（不含文件名）。 */
    private const val RELEASE_RAW_BASE =
        "https://github.com/Hesperyx/Kiora/releases/download/Kiora-wechat"
    /** 加速镜像前缀：`镜像/https://github.com/...` 格式，国内可达。 */
    private const val PROXY_PREFIX = "https://gitproxy.mrhjx.cn"

    /** 当前模块版本的「methodHash」—— 模块发版即失效，与本地缓存策略一致。 */
    fun methodHash(): String = BuildConfig.VERSION_CODE.toString()

    /** 最近一次云端拉取是否失败（网络/404/校验失败）。供调用方决定进度弹窗文案。 */
    @Volatile
    var lastAttemptFailed: Boolean = false
        private set

    /**
     * 尝试从云端拉取并导入 DexKit 结果。返回仍缺（云端未覆盖）的 feature 列表。
     * 任何网络/格式错误都降级为空结果（不抛异常），由调用方回退本地扫描。
     */
    fun resolve(items: List<IResolveDex>): List<IResolveDex> {
        val host = CloudDexHost(
            versionName = HostInfo.versionName,
            versionCode = HostInfo.versionCode,
            isGooglePlay = HostInfo.isHostGooglePlay,
        )

        val reportText = try {
            fetchReport(host)
        } catch (e: Exception) {
            WeLogger.w(TAG, "云端报告拉取失败，回退本地扫描：${e.message}")
            lastAttemptFailed = true
            return items
        }
        lastAttemptFailed = false

        val currentItems = items.map { item ->
            val feature = item as BaseFeature
            CurrentDexItem(
                technicalId = feature.technicalId,
                methodHash = methodHash(),
                delegateKeys = item.dexDelegates.mapTo(linkedSetOf()) { it.key },
            )
        }

        val selection = try {
            CloudDexReport.select(reportText, host, currentItems)
        } catch (e: Exception) {
            WeLogger.w(TAG, "云端报告校验失败，回退本地扫描：${e.message}")
            lastAttemptFailed = true
            return items
        }

        if (selection.entries.isEmpty()) {
            WeLogger.w(TAG, "云端报告无匹配项，回退本地扫描")
            lastAttemptFailed = true
            return items
        }

        // 导入到 WxDexCache：key 统一为 technicalId->delegateKey
        var imported = 0
        selection.entries.forEach { entry ->
            entry.descriptors.forEach { (key, value) ->
                WxDexCache.cacheMap["${entry.technicalId}->$key"] = value
                imported++
            }
        }
        WxDexCache.saveCache()

        // 返回仍然缺失的 feature（云端没覆盖到的）
        val coveredIds = selection.entries.mapTo(hashSetOf()) { it.technicalId }
        val remaining = items.filter { (it as BaseFeature).technicalId !in coveredIds }
        WeLogger.i(TAG, "云端导入 $imported 条 descriptor，覆盖 ${selection.entries.size} 个 feature，" +
            "剩余 ${remaining.size} 个需本地扫描")
        return remaining
    }

    private fun fetchReport(host: CloudDexHost): String {
        // 加速镜像格式：镜像前缀 + /https://github.com/... + 文件名
        val url = "$PROXY_PREFIX/$RELEASE_RAW_BASE/${CloudDexReport.assetName(host)}"
        val text = HttpUtils.getSync(url)
        if (text.isEmpty()) throw IllegalStateException("empty response from $url")
        return text
    }

    /**
     * 把本地扫描结果导出成「云端报告格式」的 JSON 文件，供维护者上传更新 GitHub Release。
     *
     * 只导出「有 DexKit 委托且扫出 descriptor」的功能（无委托的不需要云端），
     * 与云端报告 schema 完全一致（schemaVersion=2 / outcome=PASS / 版本三元组 / features[]），
     * 可直接替换云端报告。
     *
     * @return 导出文件路径；失败返回 null（不抛异常）。
     */
    fun exportLocalReport(): String? = runCatching {
        val host = CloudDexHost(
            versionName = HostInfo.versionName,
            versionCode = HostInfo.versionCode,
            isGooglePlay = HostInfo.isHostGooglePlay,
        )

        // 按 technicalId 聚合 delegates
        val featuresByTechnicalId = linkedMapOf<String, MutableList<JsonObject>>()
        WxDexCache.cacheMap.forEach { (fullKey, descriptor) ->
            if (descriptor.isEmpty()) return@forEach
            val idx = fullKey.indexOf("->")
            if (idx < 0) return@forEach
            val technicalId = fullKey.substring(0, idx)
            val delegateKey = fullKey.substring(idx + 2)
            featuresByTechnicalId.getOrPut(technicalId) { mutableListOf() }.add(
                JsonObject(
                    mapOf(
                        "key" to JsonPrimitive(delegateKey),
                        "status" to JsonPrimitive("SUCCESS"),
                        "descriptor" to JsonPrimitive(descriptor),
                    )
                )
            )
        }

        val features = featuresByTechnicalId.map { (technicalId, delegates) ->
            JsonObject(
                mapOf(
                    "technicalId" to JsonPrimitive(technicalId),
                    "methodHash" to JsonPrimitive(methodHash()),
                    "outcome" to JsonPrimitive("PASS"),
                    "delegates" to JsonArray(delegates),
                )
            )
        }

        val report = JsonObject(
            mapOf(
                "schemaVersion" to JsonPrimitive(2),
                "outcome" to JsonPrimitive("PASS"),
                "versionCode" to JsonPrimitive(host.versionCode),
                "versionName" to JsonPrimitive(host.versionName),
                "isGooglePlay" to JsonPrimitive(host.isGooglePlay),
                "features" to JsonArray(features),
            )
        )

        val dir = File(HostInfo.application.externalCacheDir, "wekit-dex-reports")
        dir.mkdirs()
        val file = File(dir, CloudDexReport.assetName(host))
        file.writeText(Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), report))
        WeLogger.i(TAG, "已导出本地报告：${file.absolutePath}（${features.size} 个 feature）")
        file.absolutePath
    }.getOrNull()
}
