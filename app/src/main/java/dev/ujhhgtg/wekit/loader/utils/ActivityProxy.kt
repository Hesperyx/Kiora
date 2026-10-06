package dev.ujhhgtg.wekit.loader.utils

import dev.ujhhgtg.wekit.constants.PackageNames

/**
 * Kiora 侧兼容层。
 *
 * WeKit 原版的 [ActivityProxy] 是一整套寄生 Activity 机制（改写 `IActivityManager` /
 * `IPackageManager` / `Instrumentation` / `ActivityThread.mH`，配 `CounterfeitActivityInfoFactory`
 * 与 `IntentTokenCache`）。Kiora 已经有功能等价、且与自身宿主适配器打通的一套：
 * `cn.hxy.kiora.lifecycle.Parasitics`（`initForStubActivity` / `injectModuleResources`）+
 * `CounterfeitActivityInfoFactory` + `DynamicActivityRegistry`，由 `cn.hxy.kiora.common.Startup`
 * 统一初始化。再搬一套会变成两条互相打架的 hook 通路。
 *
 * 所以这里只保留调用方真正依赖的部分 —— [ActProxyMgr] 的常量与「这是不是模块自己的
 * Activity」谓词，语义与 `cn.hxy.kiora.lifecycle.Parasitics` 的判定保持一致，供
 * `ParcelableFixer` 这类上游文件原样编译。原版的 hook 安装入口（`ActivityProxy.init`）
 * 不做兼容：Kiora 侧请走 `Parasitics`，重复安装没有意义。
 */
object ActivityProxy {

    object ActProxyMgr {

        const val ACTIVITY_PROXY_INTENT_TOKEN = "wekit_target_intent_token"

        const val SETTINGS_PROXY = "${PackageNames.WECHAT}.app.WeChatSplashActivity"

        const val TRANSPARENT_PROXY =
            "${PackageNames.WECHAT}.plugin.appbrand.ipc.AppBrandProxyTransparentUI"

        private val NON_PROXY_ACTIVITIES = listOf("MainActivity", "PipVoipActivity")

        /**
         * 模块 Activity 的两个命名空间：Kiora 自己的 `cn.hxy.kiora`，以及按上游原样保留
         * 包名、但同样编译在本 APK 内的移植层 `dev.ujhhgtg.wekit`。
         */
        private val MODULE_ACTIVITY_PREFIXES = listOf(
            PackageNames.MODULE,
            "dev.ujhhgtg.wekit.",
        )

        fun isModuleProxyActivity(className: String?): Boolean {
            if (className == null) return false
            if (MODULE_ACTIVITY_PREFIXES.none { className.startsWith(it) }) return false
            return NON_PROXY_ACTIVITIES.none { className.contains(it) }
        }
    }
}
