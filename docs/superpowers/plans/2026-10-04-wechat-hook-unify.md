# 微信原生 Hook 与 WeKit 功能合并 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把“原生微信 hook 项”与“WeKit 血统功能”合并进 QQ/TIM 同一条 `BaseSwitchHookItem` 设置页链路，并统一到账号级 `Kiora_Config_<account>` 存储。

**Architecture:** 保留现有 `BaseHookItem`/`BaseSwitchHookItem`/KSP 注册表作为唯一设置页模型；新增一个 `WeKitFeatureHookItem` 适配器，把每个 `dev.ujhhgtg.wekit.features.core.BaseFeature` 包装成 `BaseSwitchHookItem`。`MainHook.switchHookItemList` 同时包含原生 hook 与这些适配器，`SettingViewModel` 不再维护 `wekit:` 特殊分流。WeKit 开关从 `Kiora_Config_global` 迁移到账号级偏好，`WxFeatureLoader` 继续负责 DexKit 解析与功能启动，不回到冷启动自动扫描/强制重启旧路径。

**Tech Stack:** Kotlin, Android SharedPreferences, KSP generated `HookRegistry`, existing DexKit/`WxFeatureLoader`, existing Compose settings UI。

**Spec:** 本计划直接由当前工作区代码审查产出；基线证据为 `MainHook.kt`、`SettingViewModel.kt`、`WxFeatureAdapter.kt`、`BaseSwitchHookItem.kt`、`SwitchFeature.kt`、`HostEnv.kt`。

## Global Constraints

- 不改 QQ/TIM 行为；原生微信 hook 的 `hosts = ["wechat"]` 语义保持不变。
- 必须保留当前工作区**未提交的 DexKit 冷启动修复**：删缓存重启后不自动本地扫描、不强制重启、弹窗正确关闭。
- 不引入新依赖；只用现有 `androidx.core.content.edit`、SharedPreferences 与 Compose。
- 所有“功能开关”最终必须落在账号级 `Kiora_Config_<account>`；`Kiora_Config_global` 只保留跨账号设置（主题、插件、UI 语言等）。
- 项目当前没有测试框架，验证门禁是 `assembleRelease` + 真机 adb 断言。
- 本次只改代码与计划，不执行 `git commit`/`push`；当前未提交改动是基线。

---

## Task 1: 账号级偏好 + 开放 hook 基座

**Files:**
- Modify: `app/src/main/java/cn/hxy/kiora/host/HostEnv.kt`
- Modify: `app/src/main/java/cn/hxy/kiora/hook/base/BaseHookItem.kt`
- Modify: `app/src/main/java/cn/hxy/kiora/hook/base/BaseSwitchHookItem.kt`

**Interfaces:**
- Produces: `HostEnv.accountPreference: SharedPreferences`
- Produces: `BaseHookItem.name/tag/desc/category` 可覆盖；`BaseHookItem.isInTargetHost()/isInTargetProcess()/shouldLoad()` 可覆盖
- Produces: `BaseSwitchHookItem(switchKey: String? = null)` 构造参数；`protected open fun onEnabledChange(enabled: Boolean)`

- [x] **Step 1: 在 `HostEnv` 增加账号级偏好**

`app/src/main/java/cn/hxy/kiora/host/HostEnv.kt` 中 `globalPreference` 后加：

```kotlin
    /** 账号级偏好：原生开关与 WeKit 功能开关统一读写这里。 */
    val accountPreference: SharedPreferences by lazy {
        HostInfo.hostContext.getSharedPreferences(
            "Kiora_Config_${HostInfo.adapter?.currentAccount ?: currentAccount}",
            Context.MODE_MULTI_PROCESS
        )
    }
```

- [x] **Step 2: 打开 `BaseHookItem` 的可覆盖点**

将 `app/src/main/java/cn/hxy/kiora/hook/base/BaseHookItem.kt` 改为：

```kotlin
package cn.hxy.kiora.hook.base

import cn.hxy.kiora.annotation.HookItemAnnotation
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.reflect.TAG

abstract class BaseHookItem {

    open val name: String = TAG

    open var isEnable = true

    open val exclusiveGroup: String? = null

    protected val annotation: HookItemAnnotation? by lazy {
        this::class.java.getAnnotation(HookItemAnnotation::class.java)
    }

    open fun isInTargetProcess(): Boolean {
        val target = annotation?.process ?: return false
        if (target == "All") return true
        val currentProcess = HostInfo.processName
        return currentProcess == "${HostInfo.packageName}$target"
    }

    open fun isInTargetHost(): Boolean {
        val hosts = annotation?.hosts ?: return true
        if (hosts.isEmpty()) return true
        return HostInfo.adapter?.matchesTag(hosts) ?: false
    }

    open fun shouldLoad(): Boolean = isInTargetHost() && isInTargetProcess()
}
```

- [x] **Step 3: 重写 `BaseSwitchHookItem` 的存储与状态回调**

将 `app/src/main/java/cn/hxy/kiora/hook/base/BaseSwitchHookItem.kt` 改为：

```kotlin
package cn.hxy.kiora.hook.base

import android.content.SharedPreferences
import androidx.core.content.edit
import cn.hxy.kiora.annotation.HookCategory
import cn.hxy.kiora.host.HostEnv
import cn.hxy.kiora.utils.log.LogUtils

@Suppress("DEPRECATION")
abstract class BaseSwitchHookItem(
    private val switchKey: String? = null
) : BaseHookItem() {

    open val tag: String get() = annotation?.tag ?: "Unknown"

    open val desc: String get() {
        val originalDesc = annotation?.desc ?: ""
        return if (isNeedRestart) "$originalDesc，重启生效" else originalDesc
    }

    open val category: String get() = annotation?.category ?: HookCategory.OTHER

    private val enableKey: String get() = switchKey ?: name

    override var isEnable: Boolean
        get() = prefs.getBoolean(enableKey, false)
        set(value) {
            if (prefs.getBoolean(enableKey, false) == value) return
            prefs.edit { putBoolean(enableKey, value) }
            onEnabledChange(value)
        }

    protected open fun onEnabledChange(enabled: Boolean) {}

    var isAvailable: Boolean = false

    open val isNeedRestart: Boolean = false

    fun init() {
        try {
            if (!isInTargetHost()) {
                isAvailable = false
                return
            }

            isAvailable = onInit()
            if (isAvailable && shouldLoad()) {
                if (this is BaseClickableHookItem<*>) initData()
                onHook()
            }
        } catch (t: Throwable) {
            LogUtils.e(this, t)
            isAvailable = false
        }
    }

    protected open fun onInit(): Boolean = true

    protected open fun onHook() {}

    companion object {
        val prefs: SharedPreferences
            get() = HostEnv.accountPreference
    }
}
```

- [x] **Step 4: 编译验证基座未破坏原生 hook**

Run: `.\gradlew.bat :app:assembleRelease --no-daemon`

Expected: `BUILD SUCCESSFUL`，产物在 `app/build/outputs/apk/release/app-release.apk`。

---

## Task 2: WeKit 开关迁移到账号级偏好

**Files:**
- Modify: `app/src/main/java/dev/ujhhgtg/wekit/features/core/SwitchFeature.kt`

**Interfaces:**
- Consumes: `HostEnv.accountPreference`
- Produces: `SwitchFeature.loadPersistedState()` 从账号偏好读取并迁移旧全局值；`applyToggle` 不再写全局

- [x] **Step 1: 改 `loadPersistedState` 读取账号偏好并做一次性迁移**

将 `app/src/main/java/dev/ujhhgtg/wekit/features/core/SwitchFeature.kt` 顶部 import 加上：

```kotlin
import androidx.core.content.edit
import cn.hxy.kiora.host.HostEnv
```

把方法替换为：

```kotlin
    fun loadPersistedState() {
        val accountPrefs = HostEnv.accountPreference
        val legacy = KvStore.getBoolOrDef(technicalId, defaultEnabled)
        _isEnabled = if (accountPrefs.contains(technicalId)) {
            accountPrefs.getBoolean(technicalId, defaultEnabled)
        } else {
            accountPrefs.edit { putBoolean(technicalId, legacy) }
            legacy
        }
    }
```

- [x] **Step 2: 改 `applyToggle` 不再直接写全局 `KvStore`**

把 `applyToggle` 替换为：

```kotlin
    fun applyToggle(newState: Boolean) {
        isEnabled = newState
        toggleCompletionCallback?.run()
    }
```

原因：设置页新路径由 `BaseSwitchHookItem.isEnable` setter 先写账号偏好，再通过 `onEnabledChange` 调用这里；这里只负责触发 `enable()/disable()` 与回调。

- [x] **Step 3: 编译验证**

Run: `.\gradlew.bat :app:assembleRelease --no-daemon`

Expected: `BUILD SUCCESSFUL`。

---

## Task 3: 新增 WeKit 功能适配器与注册表

**Files:**
- Create: `app/src/main/java/cn/hxy/kiora/hook/wekit/WeKitFeatureHookItem.kt`
- Create: `app/src/main/java/cn/hxy/kiora/hook/wekit/WeKitHookRegistry.kt`

**Interfaces:**
- Consumes: `BaseFeature`, `SwitchFeature`, `ClickableFeature`, `TargetProcesses`, `HostInfo`
- Produces: `WeKitFeatureHookItem(feature: BaseFeature)`
- Produces: `WeKitHookRegistry.hookItems: List<WeKitFeatureHookItem>`
- Produces: `WeKitHookRegistry.categoryOf(feature: BaseFeature): String`

- [x] **Step 1: 新建 `WeKitFeatureHookItem.kt`**

`app/src/main/java/cn/hxy/kiora/hook/wekit/WeKitFeatureHookItem.kt`：

```kotlin
package cn.hxy.kiora.hook.wekit

import androidx.activity.ComponentActivity
import cn.hxy.kiora.hook.base.BaseSwitchHookItem
import cn.hxy.kiora.host.HostInfo
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.utils.TargetProcesses

class WeKitFeatureHookItem(
    val feature: BaseFeature
) : BaseSwitchHookItem(feature.technicalId) {

    override val name: String = feature.technicalId

    override val tag: String
        get() = runCatching { HostInfo.hostContext }
            .getOrNull()?.let(feature::localizedName) ?: feature.technicalId

    override val desc: String
        get() = runCatching { HostInfo.hostContext }
            .getOrNull()?.let(feature::localizedDescription).orEmpty()

    override val category: String
        get() = WeKitHookRegistry.categoryOf(feature)

    override fun isInTargetHost(): Boolean = HostInfo.isWeChat

    override fun isInTargetProcess(): Boolean =
        TargetProcesses.currentType in feature.targetProcesses

    override fun shouldLoad(): Boolean = isInTargetHost() && isInTargetProcess()

    override fun onInit(): Boolean {
        val clickable = feature as? ClickableFeature
        return clickable?.noSwitchWidget != true
    }

    override fun onEnabledChange(enabled: Boolean) {
        (feature as? SwitchFeature)?.isEnabled = enabled
    }

    fun onClick(activity: ComponentActivity) {
        val clickable = feature as? ClickableFeature ?: return
        runCatching { clickable.onClick(activity) }
    }
}
```

- [x] **Step 2: 新建 `WeKitHookRegistry.kt`**

`app/src/main/java/cn/hxy/kiora/hook/wekit/WeKitHookRegistry.kt`：

```kotlin
package cn.hxy.kiora.hook.wekit

import cn.hxy.kiora.annotation.HookCategory
import dev.ujhhgtg.wekit.features.WxFeatureRegistry
import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds

object WeKitHookRegistry {

    private val CATEGORY_MAP = mapOf(
        FeatureCategoryIds.CHAT to HookCategory.CHAT,
        FeatureCategoryIds.CONTACTS_GROUPS to HookCategory.GROUP,
        FeatureCategoryIds.PAYMENT to HookCategory.RED_PACKET,
        FeatureCategoryIds.MOMENTS to HookCategory.SOCIAL,
        FeatureCategoryIds.SYSTEM_PRIVACY to HookCategory.PURIFY,
        FeatureCategoryIds.VOIP to HookCategory.MSG,
        FeatureCategoryIds.NOTIFICATIONS to HookCategory.NOTIFICATION,
        FeatureCategoryIds.BEAUTIFY to HookCategory.APPEARANCE,
        FeatureCategoryIds.OFFICIAL_ACCOUNTS to HookCategory.MISC,
        FeatureCategoryIds.MINIAPPS to HookCategory.MISC,
        FeatureCategoryIds.CHANNELS to HookCategory.SOCIAL,
        FeatureCategoryIds.PROFILE to HookCategory.MISC,
        FeatureCategoryIds.DEBUG to HookCategory.DEBUG,
        FeatureCategoryIds.SCRIPTING_JAVA to HookCategory.MISC,
        FeatureCategoryIds.SCRIPTING_PYTHON to HookCategory.MISC,
        FeatureCategoryIds.ENTERTAIN to HookCategory.APPEARANCE,
    )

    val hookItems: List<WeKitFeatureHookItem> by lazy {
        WxFeatureRegistry.all.map(::WeKitFeatureHookItem)
    }

    fun categoryOf(feature: BaseFeature): String =
        CATEGORY_MAP[feature.categoryIds.firstOrNull()] ?: HookCategory.OTHER
}
```

- [x] **Step 3: 编译验证**

Run: `.\gradlew.bat :app:assembleRelease --no-daemon`

Expected: `BUILD SUCCESSFUL`。

---

## Task 4: `MainHook` 合并注册表

**Files:**
- Modify: `app/src/main/java/cn/hxy/kiora/hook/MainHook.kt`

**Interfaces:**
- Consumes: `WeKitHookRegistry.hookItems`
- Produces: `MainHook.switchHookItemList` 同时包含原生 hook 与 WeKit 适配器

- [x] **Step 1: 添加 import**

在 `app/src/main/java/cn/hxy/kiora/hook/MainHook.kt` 的 import 区加：

```kotlin
import cn.hxy.kiora.hook.wekit.WeKitHookRegistry
```

- [x] **Step 2: 合并 `switchHookItemList`**

把：

```kotlin
    val switchHookItemList =
        allHookItem.filterIsInstance<BaseSwitchHookItem>()
```

改为：

```kotlin
    val switchHookItemList =
        allHookItem.filterIsInstance<BaseSwitchHookItem>() + WeKitHookRegistry.hookItems
```

不要改 `clickableHookItemList`。WeKit 点击项不由 `BaseClickableHookItem` 的 `ConfigUiRegistry` 处理，而由 `SettingViewModel` 的类型分支直接调用 `WeKitFeatureHookItem.onClick`。

- [x] **Step 3: 编译验证**

Run: `.\gradlew.bat :app:assembleRelease --no-daemon`

Expected: `BUILD SUCCESSFUL`。

---

## Task 5: `SettingViewModel` 单轨化并删除旧桥接器

**Files:**
- Modify: `app/src/main/java/cn/hxy/kiora/ui/viewmodel/SettingViewModel.kt`
- Delete: `app/src/main/java/cn/hxy/kiora/ui/viewmodel/WxFeatureAdapter.kt`

**Interfaces:**
- Consumes: `WeKitFeatureHookItem`
- Produces: 设置页分类、toggle、click 全部走统一 `BaseSwitchHookItem`

- [x] **Step 1: 替换 import**

在 `SettingViewModel.kt` 的 import 区加：

```kotlin
import cn.hxy.kiora.hook.wekit.WeKitFeatureHookItem
```

删除所有对 `WxFeatureAdapter` 的引用后，确认文件内不再 import 该类。

- [x] **Step 2: `refreshCategories` 去掉 WeKit 追加**

把 `refreshCategories` 替换为：

```kotlin
    fun refreshCategories() {
        categories = HookCategory.ORDER.mapNotNull { category ->
            val itemsInCategory = allHookItems.filter { it.category == category }
            if (itemsInCategory.isEmpty()) null
            else CategoryData(category, itemsInCategory.map { hookItem ->
                val isClickable = hookItem is BaseClickableHookItem<*> ||
                        hookItem is WeKitFeatureHookItem
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
        }
        refreshRestartState()
    }
```

- [x] **Step 3: `toggleFunction` 去掉 `wekit:` 前缀分流**

把 `toggleFunction` 替换为：

```kotlin
    fun toggleFunction(id: String, enabled: Boolean) {
        val item = allHookItems.find { it.name == id } ?: return
        item.isEnable = enabled

        if (enabled) closeExclusiveSiblings(item)

        refreshCategories()
    }
```

WeKit 适配器的 `isEnable` setter 会写账号偏好，并通过 `onEnabledChange` 触发 `SwitchFeature.enable/disable`。

- [x] **Step 4: `handleFunctionClick` 改成类型分支**

把 `handleFunctionClick` 替换为：

```kotlin
    fun handleFunctionClick(id: String, activity: ComponentActivity) {
        val item = allHookItems.find { it.name == id } ?: return
        when (item) {
            is WeKitFeatureHookItem -> item.onClick(activity)
            is BaseClickableHookItem<*> -> {
                item.initData()
                activeConfigKey = item.name
            }
        }
    }
```

- [x] **Step 5: 删除旧 `WxFeatureAdapter.kt`**

删除文件：`app/src/main/java/cn/hxy/kiora/ui/viewmodel/WxFeatureAdapter.kt`。

- [x] **Step 6: 编译验证**

Run: `.\gradlew.bat :app:assembleRelease --no-daemon`

Expected: `BUILD SUCCESSFUL`，且 `rg -n "WxFeatureAdapter|wekit:" app/src/main/java` 无匹配。

---

## Task 6: 真机回归验证

**Files:**
- 无代码改动；用当前已构建 APK 验证。

**Interfaces:**
- Consumes: `app/build/outputs/apk/release/app-release.apk`

- [x] **Step 1: 构建并安装**

```powershell
.\gradlew.bat :app:assembleRelease --no-daemon
adb install -r app\build\outputs\apk\release\app-release.apk
```

- [x] **Step 2: 清缓存冷启动**

```powershell
adb shell am force-stop com.tencent.mm
adb shell rm -f /data/data/com.tencent.mm/shared_prefs/CacheMap_*.xml
adb logcat -c
adb shell am start -n com.tencent.mm/.ui.LauncherUI
```

断言：
- 出现“云端拉取 / 本地扫描”选择框日志（`showChoiceDialog`）。
- 点击前无 `查找方法中`、无 `restartHost`/`restartApp`、无自动本地扫描。
- 无 `FATAL EXCEPTION` / `AndroidRuntime` / `WxFeatureLoader.*失败`。

- [x] **Step 3: 点击“本地扫描”**

```powershell
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml
```

解析按钮坐标后用 `adb shell input tap <x> <y>` 点击“本地扫描”。断言：
- 出现确定进度 `showProgressDialog` 与 `updateProgress`。
- 完成后 `progress.dismiss()` 日志存在。
- 无 `Kiora DexKit` 对话框残留。
- `MainHook.loadHook` 日志出现且无异常。

- [x] **Step 4: 检查合并缓存与账号级配置**

```powershell
adb shell run-as com.tencent.mm ls /data/data/com.tencent.mm/shared_prefs
```

断言：
- `CacheMap_<version>` 重新生成。
- `Kiora_Config_<account>` 同时包含原生微信键（如 `WxAntiRevoke`）与 WeKit `technicalId` 键。
- `Kiora_Config_global` 中不再新增 WeKit 功能开关键；旧全局键在首次读取后被迁移。

- [x] **Step 5: 再次清缓存验证云端路径**

重复 Step 2，选择“云端拉取”。若云端报告不可达，自动回落本地扫描属于预期；仍需断言无残留弹窗、无强制重启。

- [x] **Step 6: 备份/导入回归**

在 Kiora 设置页导出备份，再导入同一备份。断言：原生微信开关与 WeKit 开关状态在导入后保持；`BackupManager` 能通过统一 `switchHookItemList` 读出完整账号级快照。

---

## Self-Review

- Spec coverage: 设置页单列表、账号级存储、微信原生与 WeKit 合并、加载链路保留、备份/导入均有对应任务。
- Placeholder scan: 无 TBD/TODO；所有代码块为可编译 Kotlin；命令为 Windows PowerShell 实际命令。
- Type consistency: `WeKitFeatureHookItem` 构造签名、`WeKitHookRegistry.hookItems`、`onEnabledChange`、`handleFunctionClick` 在各任务中一致。
