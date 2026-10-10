package dev.ujhhgtg.wekit.features.items.system

import androidx.activity.ComponentActivity
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.utils.HostInfo
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.android.showToastSuspend
import dev.ujhhgtg.wekit.utils.formatBytesSize
import dev.ujhhgtg.wekit.utils.formatEpoch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.milliseconds

object AutoCleanCache : ClickableFeature() {

    override val technicalId = "清理缓存垃圾"
    override val nameRes = R.string.feature_auto_clean_cache_name
    override val categoryIds = listOf(FeatureCategoryIds.SYSTEM_PRIVACY)
    override val descriptionRes = R.string.feature_auto_clean_cache_description

    private const val TAG = "AutoCleanCache"
    private const val CLEAN_INTERVAL = 30 * 60 * 1000L // 每 30 分钟清理一次

    private var cleanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 待清理目录全表。
     *
     * `filesDir.parentFile` / `externalCacheDir` 在真实设备上**可能为 null**（外置存储未挂载、
     * 被卸载，或部分 ROM 直接不返回 externalCacheDir）。旧实现用 `!!`，一旦为 null，
     * [performClean] 在后台线程抛 NPE，随后被协程的默认异常处理器抛到进程级 → 微信闪退。
     * 这里改为缺项就跳过对应目录，取不到根目录时整体退化为空表（功能静默不清理，但绝不崩）。
     */
    private val cleanPaths: List<Path> by lazy {
        val dataDir = HostInfo.application.filesDir?.parentFile?.toPath()
            ?: return@lazy emptyList<Path>()
        val storageDataDir = HostInfo.application.externalCacheDir?.toPath()?.parent

        buildList {
            add(dataDir / "cache")
            add(dataDir / "MicroMsg" / "crash")
            add(dataDir / "appbrand")
            add(dataDir / "cache" / "appbrand")
            add(dataDir / "MicroMsg" / "appbrand")
            add(dataDir / "cache" / "liteapp")
            add(dataDir / "files" / "liteapp")
            add(dataDir / "tinker")
            add(dataDir / "tinker_server")
            add(dataDir / "tinker_temp")
            if (storageDataDir != null) {
                add(storageDataDir / "cache")
                add(storageDataDir / "files" / "xlog")
                add(storageDataDir / "files" / "onelog")
                add(storageDataDir / "files" / "tbslog")
                add(storageDataDir / "files" / "Tencent" / "tbs_common_log")
                add(storageDataDir / "files" / "Tencent" / "tbs_live_log")
            }
        }
    }

    override fun onEnable() {
        startCleaningJob()
    }

    private fun startCleaningJob() {
        cleanJob?.cancel()
        cleanJob = scope.launch {
            while (isActive) {
                performClean()
                delay(CLEAN_INTERVAL.milliseconds)
            }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun performClean(): Long {
        var totalDeletedBytes = 0L
        // 目录表本身也可能因宿主状态异常在构建时抛错，整体兜底，保证清理任务永不把宿主带崩。
        runCatching {
            cleanPaths.forEach { path ->
                try {
                    WeLogger.d(TAG, "deleting $path")
                    if (path.exists()) {
                        totalDeletedBytes += calculateSize(path)
                        path.deleteRecursively()
                    }
                } catch (e: Exception) {
                    WeLogger.w(TAG, "exception during cleaning: ${path.fileName}, ${e.message}")
                }
            }
        }.onFailure { WeLogger.w(TAG, "cache path resolution failed: ${it.message}") }
        return totalDeletedBytes
    }

    private fun calculateSize(path: Path): Long {
        val file = path.toFile()
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()

        var size = 0L
        file.listFiles()?.forEach {
            size += if (it.isDirectory) calculateSize(it.toPath()) else it.length()
        }
        return size
    }

    override fun onClick(context: ComponentActivity) {
        scope.launch {
            val deletedSize = performClean()
            val sizeText = formatBytesSize(deletedSize)

            val timeText =
                if (isEnabled) context.localizedSystemString(
                    R.string.system_auto_clean_next,
                    formatEpoch(System.currentTimeMillis() + CLEAN_INTERVAL)
                )
                else ""

            showToastSuspend(
                context,
                context.localizedSystemString(R.string.system_auto_clean_complete, sizeText, timeText)
            )

            if (isEnabled) startCleaningJob()
        }
    }
}
