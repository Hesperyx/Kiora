package dev.ujhhgtg.wekit.utils

import cn.hxy.kiora.host.HostInfo

/**
 * 微信宿主的进程分类。
 *
 * 与 WeKit 原版逐项一致；区别只在进程名的来源 —— 原版走
 * `ActivityManager.runningAppProcesses` 反查，这里直接读 Kiora 已经装配好的
 * [HostInfo.processName]，少一次系统调用，也避免多进程下取错。
 */
enum class TargetProcess {
    MAIN,
    PUSH,
    APPBRAND,
    TOOLS,
    SANDBOX,
    HOTPOT,
    EXDEVICE,
    SUPPORT,
    CUPLOADER,
    PATCH,
    FALLBACK,
    DEXOPT,
    RECOVERY,
    NOSPACE,
    JECTL,
    OPENGL_DETECTOR,
    RUBBISHBIN,
    ISOLATED,
    RES_CAN_WORKER,
    EXTMIG,
    BACKTRACE,
    TMASSISTANT,
    SWITCH,
    HLD,
    PLAYCORE,
    HLDFL,
    MAGIC_EMOJI,
    OTHERS,
}

object TargetProcesses {

    val isInMain: Boolean get() = currentType == TargetProcess.MAIN

    val currentName: String
        get() = runCatching { HostInfo.processName }.getOrDefault("unknown")

    val currentType: TargetProcess
        get() {
            val parts = currentName.split(":")
            if (parts.size == 1) return TargetProcess.MAIN

            val tail = parts.last()
            return when (tail) {
                "push" -> TargetProcess.PUSH
                "sandbox" -> TargetProcess.SANDBOX
                "exdevice" -> TargetProcess.EXDEVICE
                "support" -> TargetProcess.SUPPORT
                "cuploader" -> TargetProcess.CUPLOADER
                "patch" -> TargetProcess.PATCH
                "fallback" -> TargetProcess.FALLBACK
                "dexopt" -> TargetProcess.DEXOPT
                "recovery" -> TargetProcess.RECOVERY
                "nospace" -> TargetProcess.NOSPACE
                "jectl" -> TargetProcess.JECTL
                "opengl_detector" -> TargetProcess.OPENGL_DETECTOR
                "rubbishbin" -> TargetProcess.RUBBISHBIN
                "res_can_worker" -> TargetProcess.RES_CAN_WORKER
                "extmig" -> TargetProcess.EXTMIG
                "TMAssistantDownloadSDKService" -> TargetProcess.TMASSISTANT
                "switch" -> TargetProcess.SWITCH
                "hld" -> TargetProcess.HLD
                "playcore_missing_splits_activity" -> TargetProcess.PLAYCORE
                "hldfl" -> TargetProcess.HLDFL
                "magic_emoji" -> TargetProcess.MAGIC_EMOJI
                else -> when {
                    tail.startsWith("appbrand") -> TargetProcess.APPBRAND
                    tail.startsWith("tools") -> TargetProcess.TOOLS
                    tail.startsWith("hotpot") -> TargetProcess.HOTPOT
                    tail.startsWith("isolated_process") -> TargetProcess.ISOLATED
                    tail.startsWith("backtrace") -> TargetProcess.BACKTRACE
                    else -> TargetProcess.OTHERS
                }
            }
        }
}
