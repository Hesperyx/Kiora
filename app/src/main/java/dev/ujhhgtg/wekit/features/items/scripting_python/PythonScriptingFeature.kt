package dev.ujhhgtg.wekit.features.items.scripting_python

import androidx.activity.ComponentActivity
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.extensions.ExtensionPackDialogs
import dev.ujhhgtg.wekit.extensions.PythonRuntimePack
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.features.items.scripting_python.plugin.PythonPluginManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PythonScriptingFeature : ClickableFeature() {
    override val technicalId = "Python 插件引擎"
    override val nameRes = R.string.feature_python_scripting_name
    override val descriptionRes = R.string.feature_python_scripting_description
    override val categoryIds = listOf(FeatureCategoryIds.SCRIPTING_PYTHON)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onEnable() {
        scope.launch {
            PythonPluginManager.discover()
            PythonPluginManager.activateDesired()
        }
    }

    override fun onDisable() {
        scope.launch { PythonPluginManager.deactivateAll() }
    }

    /**
     * 上游打开的是 `PythonScriptsSettingsActivity`（脚本编辑器 + 插件管理屏）。
     * 该屏依赖 `scripta` 编辑器，而 `scripta` 是上游未发布的复合构建
     * （`settings.gradle.kts:76 includeBuild("libs/common/scripta")`，submodule 未随源码分发）
     * —— 本地无制品、离线缓存无缓存，因此编辑器屏整体搁置。
     *
     * 退化为打开 Python 运行时扩展包安装屏：它同样是本功能的前置条件，
     * 且插件引擎自身的发现/启停（[onEnable] / [onDisable]）不受影响。
     */
    override fun onClick(context: ComponentActivity) {
        ExtensionPackDialogs.openExtensions(context, PythonRuntimePack, autoDownload = false)
    }
}
