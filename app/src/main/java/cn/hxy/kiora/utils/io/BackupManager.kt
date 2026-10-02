package cn.hxy.kiora.utils.io

import cn.hxy.kiora.host.HostEnv
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.hook.MainHook
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.utils.log.LogUtils
import org.json.JSONObject
import java.io.File

object BackupManager {
    private const val EXPORT_DIR_NAME = "backup"

    suspend fun performExport(context: Context, uri: Uri): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val tempRoot = File(cacheDir, "kiora_export_tmp")
                FileUtils.delete(tempRoot)
                val backupDir = File(tempRoot, EXPORT_DIR_NAME)
                FileUtils.ensureDir(backupDir)

                val configFile = File(backupDir, "config.json")
                val prefsData = JSONObject()
                BaseSwitchHookItem.prefs.all.forEach { (k, v) ->
                    prefsData.put(k, v)
                }
                // SharedPreferences 只记录被显式写过的 key，从未碰过的开关不落盘；
                // 而导入端是 clear()+写回 的覆盖语义 —— 快照里缺哪个 key，导入时就
                // 把对方机器上的同名开关抹成默认值。按注册表补齐当前宿主全部开关的
                // 有效值（含默认值），让备份成为完整快照。
                MainHook.switchHookItemList
                    .filter { it.isInTargetHost() }
                    .forEach { prefsData.put(it.name, it.isEnable) }
                prefsData.put("_version_code", BuildConfig.VERSION_CODE)
                prefsData.put("_uin", HostEnv.currentAccount)
                FileUtils.writeText(configFile, prefsData.toString())

                val currentEnvDir = File(HostEnv.currentDir)
                if (currentEnvDir.exists() && currentEnvDir.isDirectory) {
                    val dataDir = File(backupDir, "data")
                    FileUtils.ensureDir(dataDir)
                    currentEnvDir.listFiles()?.forEach { file ->
                        // crash 里是宿主崩溃转储（动辄几十 MB），与配置无关，
                        // 打进备份既拖慢导出也让导入方平白收到一堆旧崩溃。
                        if (file.name != "log" && file.name != "cache" && file.name != "crash") {
                            val dest = File(dataDir, file.name)
                            if (file.isDirectory) {
                                FileUtils.copy(file, dest)
                            } else {
                                file.copyTo(dest, true)
                            }
                        }
                    }
                }

                val zipFile = File(tempRoot, "export.zip")
                FileUtils.zip(backupDir, zipFile)

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    zipFile.inputStream().use { it.copyTo(os) }
                }
                FileUtils.delete(tempRoot)

            }.onFailure { e ->
                LogUtils.e("ExportConfig", e)
            }
        }

    suspend fun performImport(context: Context, uri: Uri): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val tempRoot = File(cacheDir, "kiora_import_tmp")
                FileUtils.delete(tempRoot)
                FileUtils.ensureDir(tempRoot)

                val zipFile = File(tempRoot, "import.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                FileUtils.unzip(zipFile, tempRoot)
                val backupDir = File(tempRoot, EXPORT_DIR_NAME)
                if (!backupDir.exists()) {
                    throw Exception("错误的备份文件格式：找不到 backup 目录")
                }

                val configFile = File(backupDir, "config.json")
                if (configFile.exists()) {
                    val jsonStr = FileUtils.readText(configFile)
                    if (jsonStr != null) {
                        val json = JSONObject(jsonStr)
                        BaseSwitchHookItem.prefs.edit(commit = true) {
                            clear()
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                if (key.startsWith("_")) continue
                                when (val value = json.get(key)) {
                                    is Boolean -> putBoolean(key, value)
                                    is String -> putString(key, value)
                                    is Int -> putInt(key, value)
                                    is Long -> putLong(key, value)
                                    is Float -> putFloat(key, value)
                                }
                            }
                        }
                    }
                }

                val dataDir = File(backupDir, "data")
                if (dataDir.exists() && dataDir.isDirectory) {
                    val targetDir = File(HostEnv.currentDir)
                    FileUtils.ensureDir(targetDir)
                    // 与导出端对称：旧备份可能带 crash 转储，跳过不还原。
                    dataDir.listFiles()?.forEach { file ->
                        if (file.name != "crash") {
                            FileUtils.copy(file, File(targetDir, file.name))
                        }
                    }
                }
                MainHook.processDataForCurrent("init")
                FileUtils.delete(tempRoot)

            }.onFailure { e ->
                LogUtils.e("ImportConfig", e)
            }
        }
}