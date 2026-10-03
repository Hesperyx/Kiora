package dev.ujhhgtg.wekit.dexkit.cache

import cn.hxy.kiora.utils.net.HttpUtils
import dev.ujhhgtg.wekit.BuildConfig
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.utils.HostInfo
import dev.ujhhgtg.wekit.utils.WeLogger

/**
 * 云端 DexKit 结果拉取。与 WeKit 原版同构，差异在 Kiora 适配：
 * - 网络：用 Kiora 的 [HttpUtils]（HttpURLConnection）替代 OkHttp，不引入新依赖
 * - methodHash：用模块 `BuildConfig.VERSION_CODE` 替代「构建期源码 MD5」
 * - 落盘：直接写 [WxDexCache]，命中后本地扫描即可跳过
 *
 * 托管：GitHub Release（`RELEASE_BASE_URL`），报告名 `wechat-<version>-<channel>.json`。
 * 走 github.com 主站（raw.githubusercontent.com 国内被墙，主站可达）。
 * 报告需由维护者用真机扫好后发布到 `Dex-Test` tag；新版本无人上传则回退本地扫描。
 */
object CloudDexResolver {
    private const val TAG = "CloudDexResolver"
    private const val RELEASE_BASE_URL =
        "https://github.com/Hesperyx/Kiora/releases/download/Dex-Test"

    /** 当前模块版本的「methodHash」—— 模块发版即失效，与本地缓存策略一致。 */
    fun methodHash(): String = BuildConfig.VERSION_CODE.toString()

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
            return items
        }

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
            return items
        }

        if (selection.entries.isEmpty()) {
            WeLogger.w(TAG, "云端报告无匹配项，回退本地扫描")
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
        val url = "$RELEASE_BASE_URL/${CloudDexReport.assetName(host)}"
        val text = HttpUtils.getSync(url)
        if (text.isEmpty()) throw IllegalStateException("empty response from $url")
        return text
    }
}
