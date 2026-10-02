package cn.hxy.kiora.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.hook.MainHook
import cn.hxy.kiora.hook.base.BaseClickableHookItem
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.ui.pages.settings.CategoryData
import cn.hxy.kiora.ui.pages.settings.FunctionData
import cn.hxy.kiora.utils.io.BackupManager
import cn.hxy.kiora.utils.net.UpdateManager
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.qq.Toasts

class SettingViewModel : ViewModel() {

    var categories by mutableStateOf<List<CategoryData>>(emptyList())
        private set

    var showUpdateDialog by mutableStateOf(false)
        private set

    var updateInfo by mutableStateOf<UpdateManager.UpdateInfo?>(null)
        private set

    var activeConfigKey by mutableStateOf<String?>(null)
        private set

    var isSearchActive by mutableStateOf(false)

    var searchQuery by mutableStateOf("")

    val searchResults by derivedStateOf {
        if (searchQuery.isEmpty()) {
            emptyList()
        } else {
            categories.flatMap { category ->
                category.items
                    .filter { item ->
                        item.name.contains(searchQuery, ignoreCase = true) ||
                                item.description.contains(searchQuery, ignoreCase = true)
                    }
                    .map { it to category.name }
            }
        }
    }


    /**
     * 设置页要展示的功能项。
     *
     * 按当前宿主过滤：多宿主下，QQ 的功能不该出现在微信的设置页里，反之亦然。
     * 适配器未装配时（理论上不会出现在宿主进程，但设置页另有入口）
     * **不过滤** —— 宁可多显示，也不要把整个列表清空。
     */
    private val allHookItems = MainHook.switchHookItemList
        .let { items ->
            if (HostInfo.adapter == null) items
            else items.filter { it.isInTargetHost() }
        }

    /**
     * 进入设置页时，各"重启生效"功能的开关与配置快照（见 [restartState]）。
     *
     * 只记开关不够：机型伪装、伪装网络状态、侧滑栏精简这类功能的生效点主要在配置里，
     * 改完配置同样要重启，退出设置页时也该提示。
     */
    private val initialRestart = allHookItems
        .filter { it.isNeedRestart }
        .associate { it.name to it.restartState() }

    var showRestartDialog by mutableStateOf(false)

    var isRestartRequired by mutableStateOf(false)
        private set

    companion object {
        private var ignoredVersion: String? = null
    }

    init {
        refreshCategories()
        checkUpdate()
    }

    private fun checkUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            UpdateManager.checkUpdateSuspend()?.let { info ->
                if (info.latestVersion != ignoredVersion) {
                    updateInfo = info
                    showUpdateDialog = true
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        showUpdateDialog = false
    }

    fun ignoreUpdate() {
        showUpdateDialog = false
        updateInfo?.let { ignoredVersion = it.latestVersion }
    }

    fun buildVersionInfo(): String {
        return "模块版本：V${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) ${HostInfo.hostName}版本：V${HostInfo.versionName}(${HostInfo.versionCode})"
    }

    fun refreshCategories() {
        categories = HookCategory.ORDER.mapNotNull { category ->
            val itemsInCategory = allHookItems.filter { it.category == category }
            if (itemsInCategory.isNotEmpty()) {
                CategoryData(category, itemsInCategory.map { hookItem ->
                    val isClickable = hookItem is BaseClickableHookItem<*>
                    FunctionData(
                        hookItem.name,
                        hookItem.tag,
                        hookItem.desc,
                        hookItem.isEnable,
                        hookItem.isAvailable,
                        isClickable,
                        if (isClickable) hookItem.name else null,
                        lockedBy = lockedByOf(hookItem)
                    )
                })
            } else null
        }
        refreshRestartState()
    }

    /**
     * "重启生效"判定用的状态：带配置的功能连配置一起比，否则只比开关。
     *
     * 配置类都是 data class，直接结构比较；每次打开配置页都会 initData 重载，
     * 值没变时比较结果也不变，不会误报。
     */
    private fun BaseSwitchHookItem.restartState(): Pair<Boolean, Any?> =
        isEnable to (this as? BaseClickableHookItem<*>)?.config

    /** 按进入设置页时的快照重算是否要提示重启：切开关后（[toggleFunction]）、关掉配置页时（[dismissDialog]）各调一次。 */
    private fun refreshRestartState() {
        isRestartRequired = allHookItems
            .filter { it.isNeedRestart }
            .any { it.restartState() != initialRestart[it.name] }
    }

    fun toggleFunction(id: String, enabled: Boolean) {
        val item = allHookItems.find { it.name == id } ?: return
        item.isEnable = enabled

        // 互斥组内只能留一个：开启时自动关掉同组其它项，
        // 它们会在刷新后的列表里自动置灰（见 [lockedByOf]）。
        if (enabled) closeExclusiveSiblings(item)

        refreshCategories()
    }

    /** 关闭与 [item] 同组、且当前处于开启状态的其它项。 */
    private fun closeExclusiveSiblings(item: BaseSwitchHookItem) {
        val group = item.exclusiveGroup ?: return
        allHookItems
            .filter { it !== item && it.exclusiveGroup == group && it.isEnable }
            .forEach { it.isEnable = false }
    }

    /**
     * 同互斥分组里是否已有别的项开着；有的话返回它的名字。
     *
     * 用于把被占用的那项在设置页置灰 —— 只关掉开关不够，
     * 用户会以为是自己没点中，而不是这个功能被另一个功能接管了。
     */
    private fun lockedByOf(item: BaseSwitchHookItem): String? {
        val group = item.exclusiveGroup ?: return null
        return allHookItems
            .firstOrNull { it !== item && it.exclusiveGroup == group && it.isEnable }
            ?.tag
    }

    fun handleFunctionClick(id: String) {
        allHookItems.find { it.name == id }?.let { item ->
            if (item is BaseClickableHookItem<*>) {
                item.initData()
                activeConfigKey = item.name
            }
        }
    }

    fun dismissDialog() {
        activeConfigKey = null
        // 配置可能刚被改过，重算后由 SettingsScreen 在退出设置页时决定要不要弹"保存并重启"
        refreshRestartState()
    }

    fun performExport(context: Context, uri: Uri) {
        viewModelScope.launch {
            BackupManager.performExport(context, uri).fold(
                onSuccess = { Toasts.qqToast(2, "备份导出成功") },
                onFailure = { Toasts.qqToast(1, "导出失败: ${it.message}") }
            )
        }
    }

    fun performImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            BackupManager.performImport(context, uri).fold(
                onSuccess = {
                    refreshCategories()
                    Toasts.qqToast(2, "配置导入成功")
                },
                onFailure = { Toasts.qqToast(1, "导入失败: ${it.message}") }
            )
        }
    }

}