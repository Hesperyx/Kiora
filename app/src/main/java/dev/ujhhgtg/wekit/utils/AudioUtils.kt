package dev.ujhhgtg.wekit.utils

import java.io.File

/**
 * 语音转码（native 实现）。
 *
 * 原版的 `anyToSilk` 先走 `nativeAnyToSilk` 直转，失败才退回 AndroidAudioDecoder（媒体解码库）
 * 兜底。Kiora 的 native 库不提供 `nativeAnyToSilk`，因此这里只保留兜底路径：先用
 * [AndroidAudioDecoder.decodeToPcm16] 解成 PCM，再交给同库的 `pcmToSilk` 编码成 silk。
 *
 * 整个过程捕获 [Throwable]（含 native 库缺席时的 UnsatisfiedLinkError），失败只返回 false
 * 并记录日志，不向宿主抛出。
 */
object AudioUtils {

    external fun silkToPcm(silkPath: String, pcmPath: String): Boolean
    external fun pcmToMp3(silkPath: String, pcmPath: String): Boolean
    external fun getDurationMs(path: String): Long

    fun anyToSilk(sourcePath: String, silkPath: String): Boolean {
        val silkFile = File(silkPath).absoluteFile
        var pcmFile: File? = null
        return try {
            val temporaryPcm = File.createTempFile("wekit-audio-", ".pcm", silkFile.parentFile)
            pcmFile = temporaryPcm
            val decoded = AndroidAudioDecoder.decodeToPcm16(sourcePath, temporaryPcm)
            val converted = pcmToSilk(
                temporaryPcm.absolutePath,
                silkPath,
                decoded.sampleRate,
                decoded.channelCount,
            )
            if (!converted) silkFile.delete()
            converted
        } catch (error: Throwable) {
            WeLogger.e("AudioUtils", "Android audio decoder fallback failed", error)
            silkFile.delete()
            false
        } finally {
            pcmFile?.delete()
        }
    }

    private external fun pcmToSilk(
        pcmPath: String,
        silkPath: String,
        sampleRate: Int,
        channelCount: Int,
    ): Boolean
}
