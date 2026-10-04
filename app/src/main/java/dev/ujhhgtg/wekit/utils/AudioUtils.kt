package dev.ujhhgtg.wekit.utils

/**
 * 语音转码（native 实现）。
 *
 * 原版的 `anyToSilk` 走 AndroidAudioDecoder（媒体解码库）兜底，此处已移除 ——
 * WeKit 功能子系统只用到 `silkToPcm` / `pcmToMp3` 等 native 方法，不涉及那个兜底路径。
 */
object AudioUtils {

    external fun silkToPcm(silkPath: String, pcmPath: String): Boolean
    external fun pcmToMp3(silkPath: String, pcmPath: String): Boolean
    external fun getDurationMs(path: String): Long
}
