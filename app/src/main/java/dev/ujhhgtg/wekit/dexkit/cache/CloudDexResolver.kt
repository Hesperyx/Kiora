package dev.ujhhgtg.wekit.dexkit.cache

import cn.hxy.kiora.utils.dexkit.DexKitCache
import cn.hxy.kiora.utils.dexkit.DexKitFinder
import cn.hxy.kiora.utils.net.HttpUtils
import dev.ujhhgtg.wekit.BuildConfig
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.resolution.DexResolutionStatus
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.utils.HostInfo
import dev.ujhhgtg.wekit.utils.WeLogger
import java.io.File
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 云端 DexKit 结果拉取。与 WeKit 原版同构，差异在 Kiora 适配：
 * - 网络：用 Kiora 的 [HttpUtils]（HttpURLConnection）替代 OkHttp，不引入新依赖
 * - methodHash：用模块 `BuildConfig.VERSION_CODE` 替代「构建期源码 MD5」
 * - 落盘：直接写 [WxDexCache]，命中后本地扫描即可跳过
 *
 * 托管：GitHub Release（报告名 `wechat-<version>-<channel>.json`）。
 * 优先走加速镜像 `gitproxy.mrhjx.cn`（国内可达），失败再回落 GitHub 直连
 * （`release-assets.githubusercontent.com`）。镜像对同一 IP 会下发 429，实测过；
 * 单一镜像不可用就等于整条云端恢复链路失效，所以必须留直连兜底。
 * 报告需由维护者用真机扫好后发布到 `Kiora-wechat` tag；新版本无人上传则回退本地扫描。
 */
object CloudDexResolver {
    private const val TAG = "CloudDexResolver"
    /** GitHub Release 报告的原始下载地址（不含文件名）。 */
    private const val RELEASE_RAW_BASE =
        "https://github.com/Hesperyx/Kiora/releases/download/Kiora-wechat"
    /** 加速镜像前缀：`镜像/https://github.com/...` 格式，国内可达。 */
    private const val PROXY_PREFIX = "https://gitproxy.mrhjx.cn"

    /**
     * 云端报告体积上限（8 MB）。与上游 `CloudDexResolver.MAX_REPORT_BYTES` 同值同语义：
     * 报告只是一张 descriptor 表，正常几十 KB；超限说明响应根本不是报告（劫持页、
     * 错误页、超大文件），直接判为不可用，避免把整份响应读进内存。
     */
    private const val MAX_REPORT_BYTES = 8 * 1024 * 1024

    // 报告里的状态／结论字面量，与 [CloudDexReport] 消费侧的比较值一一对应。
    private const val STATUS_SUCCESS = "SUCCESS"
    private const val STATUS_EXPECTED_FAILURE = "EXPECTED_FAILURE"
    private const val STATUS_UNEXPECTED_FAILURE = "UNEXPECTED_FAILURE"
    private const val OUTCOME_PASS = "PASS"
    private const val OUTCOME_PASS_WITH_EXPECTED_FAILURES = "PASS_WITH_EXPECTED_FAILURES"

    /** 当前模块版本的「methodHash」—— 模块发版即失效，与本地缓存策略一致。 */
    fun methodHash(): String = BuildConfig.VERSION_CODE.toString()

    /** 最近一次云端拉取是否失败（网络/404/校验失败）。供调用方决定进度弹窗文案。 */
    @Volatile
    var lastAttemptFailed: Boolean = false
        private set

    data class CloudResolveResult(
        val remainingFeatures: List<IResolveDex>,
        val mainResolved: Boolean,
        val cloudFailed: Boolean,
    )

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

    /**
     * 微信流程的一次性云端解析：同一份报告同时恢复 WeKit feature 与主框架 DexKit 键，
     * 只拉取一次网络，避免 [resolve] 与 [tryRestoreMainDex] 各拉一次。
     *
     * @param includeMain true 时同时尝试恢复主框架缺失键；false 表示调用方只处理 WeKit feature。
     * @return 云端未覆盖、仍需本地扫描的 feature，以及主框架键是否已全部可用。
     */
    fun resolveAll(
        items: List<IResolveDex>,
        includeMain: Boolean,
        onProgress: ((message: String, current: Int, total: Int) -> Unit)? = null,
    ): CloudResolveResult {
        val host = CloudDexHost(
            versionName = HostInfo.versionName,
            versionCode = HostInfo.versionCode,
            isGooglePlay = HostInfo.isHostGooglePlay,
        )

        val reportText = try {
            fetchReport(host) { downloaded, total ->
                val pct = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                onProgress?.invoke("正在下载云端报告… $pct%", pct, 100)
            }
        } catch (e: Exception) {
            WeLogger.w(TAG, "云端报告拉取失败，转为本地扫描：${e.message}")
            lastAttemptFailed = true
            return CloudResolveResult(items, mainResolved = !includeMain, cloudFailed = true)
        }

        val featureItems = items.map { item ->
            val feature = item as BaseFeature
            CurrentDexItem(
                technicalId = feature.technicalId,
                methodHash = methodHash(),
                delegateKeys = item.dexDelegates.mapTo(linkedSetOf()) { it.key },
            )
        }
        val mainItems = if (includeMain) {
            DexKitFinder.mainDexItems().map { (tag, keys) ->
                CurrentDexItem(technicalId = tag, methodHash = methodHash(), delegateKeys = keys)
            }
        } else {
            emptyList()
        }
        val currentItems = featureItems + mainItems
        if (currentItems.isEmpty()) {
            lastAttemptFailed = false
            return CloudResolveResult(emptyList(), mainResolved = true, cloudFailed = false)
        }

        val selection = try {
            CloudDexReport.select(reportText, host, currentItems)
        } catch (e: Exception) {
            WeLogger.w(TAG, "云端报告校验失败，转为本地扫描：${e.message}")
            lastAttemptFailed = true
            return CloudResolveResult(items, mainResolved = !includeMain, cloudFailed = true)
        }
        lastAttemptFailed = false

        onProgress?.invoke("云端报告下载完成，正在解析…", 100, 100)
        var imported = 0
        selection.entries.forEach { entry ->
            entry.descriptors.forEach { (key, value) ->
                DexKitCache.cacheMap["${entry.technicalId}->$key"] = value
                imported++
            }
        }
        DexKitCache.saveCache()

        val coveredIds = selection.entries.mapTo(hashSetOf()) { it.technicalId }
        val remaining = items.filter { (it as BaseFeature).technicalId !in coveredIds }
        val mainResolved = if (includeMain) DexKitFinder.unresolvedKeys().isEmpty() else true
        WeLogger.i(TAG, "云端导入 $imported 条 descriptor，覆盖 ${selection.entries.size} 个 technicalId，" +
            "剩余 ${remaining.size} 个 feature 需本地扫描，主框架已恢复=$mainResolved")
        return CloudResolveResult(remaining, mainResolved, cloudFailed = false)
    }

    private fun fetchReport(
        host: CloudDexHost,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null,
    ): String {
        val asset = CloudDexReport.assetName(host)
        // 加速镜像格式：镜像前缀 + /https://github.com/... + 文件名
        val urls = listOf(
            "$PROXY_PREFIX/$RELEASE_RAW_BASE/$asset",
            "$RELEASE_RAW_BASE/$asset",
        )
        val noProgress: (Long, Long) -> Unit = { _, _ -> }
        var last = ""
        var sawTooLarge = false
        for ((index, url) in urls.withIndex()) {
            var tooLarge = false
            // 体积上限用进度回调实现：[HttpUtils.getSyncWithProgress] 内部吞异常返回空串，
            // 所以这里「置位 + 抛出」——抛出会立刻中断读取循环并关连接，循环外再用
            // sawTooLarge 给出可诊断的原因（否则只剩一句「响应为空」）。
            val capped: (Long, Long) -> Unit = { downloaded, total ->
                if (total > MAX_REPORT_BYTES || downloaded > MAX_REPORT_BYTES) {
                    tooLarge = true
                    throw IOException("cloud Dex report is larger than $MAX_REPORT_BYTES bytes")
                }
                (onProgress ?: noProgress)(downloaded, total)
            }
            val text = HttpUtils.getSyncWithProgress(url, capped)
            if (tooLarge) sawTooLarge = true
            // 以 schemaVersion 判定「拿到的确实是一份报告」：429/5xx 的响应体是错误 JSON，
            // 不含这个字段，会被跳过并试下一个源。
            if (text.contains("\"schemaVersion\"")) {
                if (index > 0) WeLogger.i(TAG, "前缀源不可用，已改用第 ${index + 1} 个源")
                return text
            }
            last = text
        }
        if (sawTooLarge) throw IOException("cloud Dex report is larger than $MAX_REPORT_BYTES bytes")
        if (last.isEmpty()) throw IllegalStateException("empty response from ${urls.joinToString()}")
        return last
    }

    /**
     * 主框架（Kiora 原生 hook）的 DexKit 云端恢复。
     *
     * 清空缓存后，主框架启动先走这里：云端命中则跳过 DexKitBootstrap 的本地扫描 +
     * 强制重启。与 [resolve]（WeKit）共用同一份云端报告与 [CloudDexReport.select]，
     * 因为两套缓存已并入同一个 [DexKitCache]。
     *
     * @return 主框架缺失键是否已全部恢复（true = 可跳过本地扫描直接 loadHook）。
     */
    fun tryRestoreMainDex(): Boolean {
        val host = CloudDexHost(
            versionName = HostInfo.versionName,
            versionCode = HostInfo.versionCode,
            isGooglePlay = HostInfo.isHostGooglePlay,
        )

        val reportText = try {
            fetchReport(host)
        } catch (e: Exception) {
            WeLogger.w(TAG, "主框架云端报告拉取失败，回退本地扫描：${e.message}")
            lastAttemptFailed = true
            return false
        }

        val items = DexKitFinder.mainDexItems().map { (tag, keys) ->
            CurrentDexItem(technicalId = tag, methodHash = methodHash(), delegateKeys = keys)
        }
        if (items.isEmpty()) return false

        val selection = try {
            CloudDexReport.select(reportText, host, items)
        } catch (e: Exception) {
            WeLogger.w(TAG, "主框架云端报告校验失败，回退本地扫描：${e.message}")
            lastAttemptFailed = true
            return false
        }

        var imported = 0
        selection.entries.forEach { entry ->
            entry.descriptors.forEach { (key, value) ->
                DexKitCache.cacheMap["${entry.technicalId}->$key"] = value
                imported++
            }
        }
        DexKitCache.saveCache()

        val stillMissing = DexKitFinder.missingKeys()
        WeLogger.i(TAG, "主框架云端恢复 $imported 条 descriptor，剩余 ${stillMissing.size} 键缺失")
        return stillMissing.isEmpty()
    }

    /**
     * 把本地扫描结果导出成「云端报告格式」的 JSON 文件，供维护者上传更新 GitHub Release。
     *
     * 分类口径与上游一致（`dextest/DexFeatureRunner.featureOutcome` 与
     * `DexTestWorkerTest.buildReport`）：
     * - delegate：非占位符描述符 ⇒ `SUCCESS`；占位符 ⇒ `EXPECTED_FAILURE`；其余 ⇒ `UNEXPECTED_FAILURE`。
     *   `isPlaceholder` **必须显式写出**：消费侧 [CloudDexReport.select] 按 `status` + `isPlaceholder`
     *   成对校验（`SUCCESS` 必须非占位符、`EXPECTED_FAILURE` 必须是占位符），字段缺席会被默认成
     *   `false` —— 占位符哨兵于是被当成有效结果安装，这正是本方法此前的问题所在。
     * - feature：含任一不可用 delegate ⇒ `FAIL`；仅含占位符 ⇒ `PASS_WITH_EXPECTED_FAILURES`；否则 `PASS`。
     *   根 `outcome` 只有全部 feature 都通过才是 `PASS`（上游同款），而消费侧要求根 `outcome == "PASS"`，
     *   否则整份报告被拒、**所有**功能一起失效。因此 `FAIL` 的 feature 整条不导出：那个功能回退本地
     *   扫描，其余功能照常命中。
     * - 主框架键（[DexKitFinder.mainDexItems]）：缓存值非空即 `SUCCESS`；空串表示本地扫描没找到，
     *   该 tag 整条不导出。
     *
     * 需要说明的是，委托诊断只在本地扫描与缓存恢复时写入：云端导入的 feature 委托仍是 `PENDING`，
     * 不会出现在导出结果里（维护者发布报告走完整本地扫描，不受影响）。
     *
     * @param items 当前进程参与 DexKit 的 WeKit feature（调用方用 `filterIsInstance<IResolveDex>()` 取）。
     * @return 导出文件路径；失败返回 null（不抛异常）。
     */
    fun exportLocalReport(items: List<IResolveDex>): String? = runCatching {
        val host = CloudDexHost(
            versionName = HostInfo.versionName,
            versionCode = HostInfo.versionCode,
            isGooglePlay = HostInfo.isHostGooglePlay,
        )

        var skipped = 0
        val features = mutableListOf<JsonObject>()

        // WeKit 功能：delegate 状态取自委托自身的诊断（本地扫描与缓存恢复都会写）。
        items.forEach { item ->
            val feature = item as BaseFeature
            val delegates = item.dexDelegates.map { delegate ->
                DexExportDelegate(
                    key = delegate.key,
                    status = delegate.diagnostic.status,
                    isPlaceholder = delegate.isPlaceholder,
                    descriptor = WxDexCache.cacheMap["${feature.technicalId}->${delegate.key}"]
                        ?: delegate.getDescriptorString().orEmpty(),
                    message = delegate.diagnostic.message,
                    blockedBy = delegate.diagnostic.blockedBy,
                )
            }
            val json = featureJson(feature.technicalId, delegates)
            if (json != null) features += json else skipped++
        }

        // 主框架键：与 WeKit 共用同一份缓存（键形如 `WeChatDexKit->查询名`）。
        DexKitFinder.mainDexItems().forEach { (tag, keys) ->
            val delegates = keys.map { key ->
                DexExportDelegate(
                    key = key,
                    status = DexResolutionStatus.SUCCESS,
                    isPlaceholder = false,
                    descriptor = WxDexCache.cacheMap["$tag->$key"].orEmpty(),
                )
            }
            val json = featureJson(tag, delegates)
            if (json != null) features += json else skipped++
        }

        val report = JsonObject(
            linkedMapOf(
                "schemaVersion" to JsonPrimitive(2),
                // 根 outcome 恒为 PASS：FAIL 的 feature 已在上面整条剔除。
                "outcome" to JsonPrimitive(OUTCOME_PASS),
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
        WeLogger.i(
            TAG,
            "已导出本地报告：${file.absolutePath}（${features.size} 个 feature，跳过 $skipped 个未通过）",
        )
        file.absolutePath
    }.getOrNull()

    /**
     * 校验一组 delegate 并生成报告里的 feature 对象。
     *
     * @return 全部 delegate 都可用时返回 feature 对象；任一不可用（描述符为空，或状态过不了消费侧
     *   校验）时返回 null，由调用方整条跳过。
     */
    private fun featureJson(technicalId: String, delegates: List<DexExportDelegate>): JsonObject? {
        if (delegates.isEmpty() || delegates.any { !it.usable }) return null
        val outcome = if (delegates.any { it.reportStatus == STATUS_EXPECTED_FAILURE }) {
            OUTCOME_PASS_WITH_EXPECTED_FAILURES
        } else {
            OUTCOME_PASS
        }
        return JsonObject(
            linkedMapOf<String, JsonElement>(
                "technicalId" to JsonPrimitive(technicalId),
                "methodHash" to JsonPrimitive(methodHash()),
                "outcome" to JsonPrimitive(outcome),
                "delegates" to JsonArray(delegates.map { it.toJson() }),
            )
        )
    }

    /** 导出用的 delegate 记录：把委托诊断归一成报告里的 `status` + `isPlaceholder`。 */
    private data class DexExportDelegate(
        val key: String,
        val status: DexResolutionStatus,
        val isPlaceholder: Boolean,
        val descriptor: String,
        val message: String? = null,
        val blockedBy: String? = null,
    ) {
        /**
         * 报告里的 `status`。占位符一律记成 `EXPECTED_FAILURE`：缓存恢复路径
         * （[dev.ujhhgtg.wekit.dexkit.dsl.BaseDexDelegate.loadDescriptor] → `recordDescriptorAfterSet`）
         * 会把「扫描期判定为预期失败」的委托退化记成 `SUCCESS`，只有 `isPlaceholder` 能还原它，
         * 而消费侧只接受 `EXPECTED_FAILURE` + 占位符这一组合。
         */
        val reportStatus: String
            get() = when {
                status == DexResolutionStatus.UNEXPECTED_FAILURE -> STATUS_UNEXPECTED_FAILURE
                isPlaceholder -> STATUS_EXPECTED_FAILURE
                status == DexResolutionStatus.SUCCESS -> STATUS_SUCCESS
                // PENDING / BLOCKED / INCOMPLETE：本地没扫出结果，消费侧一律不接受。
                else -> STATUS_UNEXPECTED_FAILURE
            }

        /** 描述符非空且状态能通过消费侧 `CloudDexReport` 的 `hasValidOutcome()`。 */
        val usable: Boolean
            get() = descriptor.isNotEmpty() && reportStatus != STATUS_UNEXPECTED_FAILURE

        fun toJson(): JsonObject {
            val fields = linkedMapOf<String, JsonElement>(
                "key" to JsonPrimitive(key),
                "status" to JsonPrimitive(reportStatus),
                "descriptor" to JsonPrimitive(descriptor),
                "isPlaceholder" to JsonPrimitive(isPlaceholder),
            )
            message?.let { fields["message"] = JsonPrimitive(it) }
            blockedBy?.let { fields["blockedBy"] = JsonPrimitive(it) }
            return JsonObject(fields)
        }
    }
}
