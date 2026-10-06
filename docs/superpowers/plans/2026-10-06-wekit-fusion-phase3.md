# WeKit → Kiora 融合收口计划（Phase 3/4）· 滚动版

> **Goal:** 把 WeKit 血统剩余 items 功能与其依赖层（工具 / 组件 / 数据 / 主题）按依赖闭包分批、可编译地迁入 Kiora。
> 上游 `D:\code\fenxi3\WeKit-master`，目标 `D:\code\fenxi3\Kiora`。
> 前置：`docs/superpowers/plans/2026-10-05-wekit-full-migration.md`（Phase 1/2，已完成）。
> 本文件为滚动计划：每完成一批就更新「进度快照」与「剩余批次」。

## 进度快照（2026-10-06 实测，P1+P2+P3+P4-1+P4-1b+P4-2+P4-2b+P4-2c+P4-2d+P4-2e 已落地）

口径：**上游相对路径存在性**（不再用单一声明名/正则口径，理由见 P4-2 节）。分母 = 上游 `app\src\main\java\dev\ujhhgtg\wekit` 下 767 个 .kt。

| 维度 | Kiora | WeKit 上游 | 覆盖率 |
|---|---|---|---|
| `dev/ujhhgtg/wekit` 路径命中 | **600**（Kiora 实际 641 个 .kt，多 41 个自有） | 767 | **78.2%** |
| features\items | **326** | 333 | 97.9%（余 7 = 3 未注册 + agent 3 + CustomConversationNotifications） |
| features\api | **47** | 82 | 57.3%（20 个 `core\*`/`core\models\*` 被 Kiora 拍平成 `features\api\*.kt`，15 个属 agent） |
| ui | **76** | 99 | 76.8%（余 23 = agent 13 + nuke 11 等） |
| data | **18** | 19 | 94.7%（余 = `MmkvReadonlyReader`） |
| utils | **72** | 75 | 96.0%（余 = `AppUpdater` / `DebugUtils` / `hook_status\HookStatus`） |
| loader | **5** | 21 | 23.8%（`entry\*` zygisk/frida 不迁、`startup\*` 由 Kiora 自有链路取代） |
| activity | **5** | 23 | 21.7%（`activity\nuke` 不迁；`activity\settings` 只按需抽件） |
| i18n | **9** | 9 | 100%（仅去掉 lsparanoid 解码层，宿主资源注入已恢复） |
| dexkit | **10** | 10 | 100% |
| extensions | **12** | 13 | 92.3%（余 = `ArchLinuxPack`，随 agent 决策） |
| **非排除区真缺件** | **22** | — | 逐个已取证，见「当前状态：不可迁 / 永久搁置」 |
| `WxFeatureRegistry.all` 注册 | **217** | — | — |
| `WeApiRegistry`（dexBacked 19 + startupBacked 19） | **38** | — | — |
| 上游 items 功能对象已注册 | **222** | 225 | 98.7%（**未注册 3**：`ChatToolbar` / `ForwardMessages` / `WeAgent`） |

- 编译基线：`:app:compileReleaseKotlin --offline --no-daemon` → `errors: 0`；
  `:app:assembleRelease --offline --no-daemon` → `BUILD SUCCESSFUL`，APK **14,493,895 B**
  （P4-2e 后 sha256 `28D9323F…`；体积与 P4-2c/P4-2d 相同属 zip 压缩吸收，`classes2.dex` 逐批微增）。
- `git diff --cached --check` → exit 0。

### 已落地批次（新→旧）

| 提交 | 内容 |
|---|---|
| `P4-2e`（本次） | 差分清单闭合：澄清 89 个「两侧都在但报差异」文件里 **20 个是 `features/api/core/*`→`features/api/*` 的 `0 0` 纯拍平改名**（逐字节一致），真实内容差异 **69 项至此全判完**；本轮 7 项（还原 `IResolveDex.kt` 两处 KDoc 使其与上游逐字节一致；`QuickOpenMoments` 把内联的 `"wekit_folder_"` 改回 `ConversationAggregation.FOLDER_PREFIX`（值实测相同）；`HomeSidePanelActions`/`AutoCleanCache`/`ForceTabletMode`/`Stream.kt`/`TargetProcesses` 判为等价改写并逐环节取证） |
| `P4-2d` | 第二轮差分分诊（90 个「两侧都在但仍有差值」文件全量派子代理核）：控件层按上游恢复 2 文件（`BaseWidget` 117→166 行补齐 `onTrailingClick`/`clickHaptic`/`trailingDivider`/`remember` 化 interactionSource + `foreContent()` 叠层归位；`SwitchWidget` 43→129 行恢复触感 / `separateClickAreas` 判据 / `Role.Switch` 语义 / 拇指图标）+ 修 **2 处真回归**（`SwitchFeature.applyToggle` 丢持久化；`HideHomeScreenSwipeDownPage` 过期注释导致分组态高度硬编码 48dp，改回上游 `if (!ConversationGrouping.isEnabled) 48 else 94` 后与上游逐字节一致）+ 3 处 KDoc 订正（`BaseFeature`/`KvStore`/`WeLogger`）+ 记 1 项能力缺口（python 脚本设置页依赖未迁入的 `scripta`） |
| `P4-2c` | 差分三分复检（对上游全树 .kt 逐文件比对，再对「两侧都在但仍有差值」的 68 个文件分类）：修 **1 处真回归** `features\items\chat\ChatFooterHooks.kt`（过期注释顶替了上游两段长按绑定，恢复后与上游逐字节一致）+ **1 处漏迁** `features\items\miniapps\ErudaConsole.kt` 补回 `ResourcesInjector.injectModuleRes(resources)` + 1 处 KDoc 订正（`WeChatInputBarMenuApi`）；注册表 217 不变；APK 13,562,308→**14,493,895 B** |
| `P4-2b` | `i18n\LocalizedContextFactory.kt` 恢复宿主资源注入（上游 `InjectedHost` 分支的 `ResourcesInjector.injectModuleRes(it.resources)` 曾随 lsparanoid 一起被删），24 个调用点受益；注册表 217 项 technicalId 撞键自查通过；APK 仍 13,562,308 B |
| `P4-2` | 全树对齐复测 + 补两处漏迁行为：新增 `features\items\system\SafeMode.kt`（2 进程级「安全模式」开关，3 条字符串资源）；`WxFeatureLoader.load()` 接线 ① 安全模式门控（只加载 API 层，跳过全部 items）② `ConversationGrouping.migrateTabStyle(...)`（原先函数存在但零调用 = 死代码）；APK 13,561,264→13,562,308 B |
| `P4-1` | scripting_python 核心 + extensions 扩展包栈：`python\api` 10 接口（vendoring）、`scripting_python` 12 文件、`extensions` 6 文件（`ExtensionSupport`/`ExtensionPackRegistryValidation`/`ScriptDepsPack`/`ExtensionPackDialogs`/`PythonRuntimeArchive`/`PythonRuntimePack`）、`activity\settings\ExtensionsSettingsActivity.kt`、`loader\utils\HybridClassLoader.kt`、`ClassLoaders` 补 `BOOT`/`HYBRID`；`ExtensionPacksProvider.ALL_PACKS` 由 `emptyList()` 改为登记两包；注册 215→216；APK 13,414,588→13,474,920 B |
| `P3` | 主题栈：beautify 7（`ApplyGlobalBackground` / `CenterProfileCard` / `CustomMessageBubbles` / `MonetEngine`→`WeApiRegistry` / `MonetEngineModuleGenerator` / `ReplaceNavigationBar` / `Themes`）+ `utils\monet` 12 文件；引入 ARSCLib 1.4.0（经 `prepareAndroidArsclib` Jar task 剔除 `android/**`、`org/xmlpull/v1/**`）/ apksig 9.3.3 / bouncycastle 1.86 prov+pkix；`NumberPickerWidget.kt` 换上游完整版；注册 209→215；APK 12,253,200→13,414,588 B |
| `4c7db17` | P2：payment 5 + moments 7（含 `AutoMomentsBase` 派生两位）+ contacts 3（含 `HideContactsNotifications`→`WeApiRegistry`）+ voip 1，注册 194→209；引入 biometric 1.2.0-alpha05 / fragment 1.5.4，`TransparentActivity` 改基类为 `FragmentActivity`，`KvStore` 补 nullable `prefOption` |
| `dbeca51` | P1 公共层：`DexResolver` / `LocalDexResolver` / `OsmLocationPicker` / `WeKitBasicDialog` / `SettingsComponents` / `DexCacheManager` / `ResolutionCoordinator` / `DexResolutionBatch` / `WeChatSettingsManager` / `WeViewTreeLifecycleProvider`；附带 debug 3 + system 4，注册 187→194 |
| `1304d9b` | chat 功能波：ReadReceipts / MarkdownRendering / FloatingChatFooter+Header / SwipeConversation+MessageOperations / MessageTimeEnhancements / HalfScreenAlbumPicker / VoiceMessagePlaybackOptimization / ForwardFavoriteVoices / AddToAggregationFolder / BlockAtAllNotifications / AutoCacheFiles+Images / 群成员实名三件套 / 反撤回 / 拟造记录 / 引用直达 / 安全消息 / 会话置顶 / 消息入场动画 等 **26 项注册**（注册 161→187） |
| `f24ff25` | ApiServer REST+MCP + WeChatService，引入 `io.modelcontextprotocol:kotlin-sdk-server:0.15.0`，proguard 加 `-dontwarn java.lang.management.**` |
| `8d28216` | 贴纸/语音面板 UI 层（`ui\panel\` 10 文件）+ EdgeTtsClient，引入 ktor 3.6.0 |
| `1dd8725` | 贴纸/语音面板后端（FunBox 服务 / Telegram 贴纸 / 语音仓库 16 文件） |
| `13ddccc` | notifications 分类 NotificationsEvolved |
| `9392484` | batch 分类 + chat_input_bar_menu |
| `c6a2fc9` | 数据层与会话/联系人簇（65 文件 WeKit 批次） |
| `27dfa22` | coil3 3.6.3 + okhttp3 5.5.0（上游精确版本） |
| `6eaf224` | material3 1.5.0-alpha28 + materialkolor + 主题层 |
| `fe490f3`/`7d0ab89`/`3b1b99f`/`764b444` | 主题色与字段 / 本地化资源助手 / M3 控件层 / 11 项消息菜单功能 |

### 待决策项的当前处置

- **okhttp3**：已引入（5.5.0），StickerPanel / VoicePanel 全量可用。
- **lsparanoid**：**已定案——不引入**。证据：Kiora 全树零代码引用（唯一提及是
  `i18n\LocalizedContextFactory.kt` 的注释）；上游全树也仅 3 个文件引用
  `LspBootstrap`/`LspResourceContext`，其中 `application\ModuleApplication.kt` 与
  `loader\utils\NativeLoader.kt` 都未迁。它买到的只有 release 变体的资源名/字符串混淆
  （反分析硬化），**不解锁任何功能**；成本却是 mavenLocal group 白名单 + NDK 29.0.14206865
  + omvll + arm64-only + 变体保护逻辑。Kiora 路线：WeKit 血统资源明文放进模块
  `strings.xml`，解码这一步按定义是空操作。
  - **连带发现并已修**：Kiora 版 `LocalizedContextFactory` 当初连**宿主注入**一并删了，
    而 `LocaleResourceMode.InjectedHost` 有 **24 个调用点**（19 个 `*LocalizedResources.kt`
    + `WeSettingsInjector` / `KillHostUtils` / `HostLocalizedStrings` / `InjectedUiTheme` /
    `WeKitBasicDialog`），这些上下文用于在微信自己的界面里取字符串；未注入时按模块 id
    取值会抛 `Resources.NotFoundException`。已恢复 `ResourcesInjector.injectModuleRes(it.resources)`，
    与上游逐句一致（注入本身幂等：`injectModuleRes` 先 `hasModuleRes()` 判定）。
- **scripting_python**：**已迁（核心），编辑器屏搁置**。Chaquopy 只存在于
  `extension-packs\python-runtime\runtime` 那个独立打包子构建，app 侧 12 文件零 Chaquopy 引用；
  Python 运行时是运行时下载的扩展包（`PythonRuntimePack` ← `ExtensionPacks.BASE_URL`）。
  搁置的是 `activity\scripting_python\{PythonHighlighter, PythonScriptsSettingsActivity}.kt`：
  它们依赖 `scripta` 编辑器，而 `scripta` 是上游未发布的复合构建
  （`settings.gradle.kts:76 includeBuild("libs/common/scripta")`，submodule 内容未随源码分发，实测 0 条目），
  mavenLocal 与离线缓存均无制品。
- **WeAgent / `agent/` 全栈**：**仍未拍板**。离线依赖全齐（ktor / MCP SDK / okhttp3 / coil3 /
  `com.github.mwiede:jsch` 2.28.7 / `org.jsoup:jsoup` 1.23.2 / material-symbols 全在缓存），
  真缺口是 `third_party/proot-static`、Arch Linux rootfs、`libs/common/scripta` 三个未分发的 submodule。
  排除后合计影响约 **216 个声明**（`agent\*` 71 文件、`ui\agent\*`、`features\api\agent\*`）。

## Global Constraints

- 不新增第三方依赖、不提交、不推送；不引入 Compose 分支到既有非 Compose 链路。
- 微信 DexKit 行为不变：冷启动不自动本地扫描、不自动强制重启，解析由 `WxFeatureLoader` 统一处理。
- 新增字符串同时补 `app/src/main/res/values/strings.xml` 与 `app/src/main/java/dev/ujhhgtg/wekit/R.kt`。
- 新增 items 必须在 `WxFeatureRegistry.kt` 注册；服务 API 进 `WeApiRegistry.dexBacked`（有 DexKit 委托）
  或 `startupBacked`（有 Hook 副作用）。
- 新增分类映射补到 `cn/hxy/kiora/hook/wekit/WeKitHookRegistry.kt`。
- 不做无关重构；不提交 `local.properties`、签名、`dex-reports/*`、`*.png`、`docs/superpowers/notes/`。
- 每批验证：`:app:compileReleaseKotlin` → `:app:assembleRelease --offline --no-daemon` → `git diff --check`。

## 剩余缺口（616 缺失声明旧口径；P1/P2/P3 已消化左列各项）

| 上游目录 | 缺失数 | 归属批次 |
|---|---|---|
| `activity\settings` | 66 | **不迁**（Kiora 自有 `cn.hxy.kiora.activity.SettingActivity` 取代；仅 `M3ListScaffold` 已被单独抽出） |
| `ui\agent` + `ui\agent\settings` | 98 | 决策项（agent） |
| `ui\content\nuke` | 54 | **已定案（P3）**：只保留 `NukeTheme.kt`/`NukeMotion.kt`，其余与 `activity\nuke` 同属上游设置 App 的屏幕栈，不迁 |
| `activity\nuke` | 39 | **已定案（P3）**：不迁 |
| `agent\*` 全栈 | ~118 | 决策项（agent） |
| `utils\monet` | 36 | **已完成（P3）** |
| `ui\content`（DexResolver/LocalDexResolver/OsmLocationPicker/WeKitBasicDialog 等） | 18 | **已完成（P1）** |
| `features\items\beautify` | 11 | **已完成（P3）** |
| `features\items\moments` | 8 | **已完成（P2）** |
| `features\items\system` | 8 | **已完成（P1/P2）** |
| `features\items\scripting_python` | 23 | **已完成（P4-1）**；`activity\scripting_python` 编辑器屏因 `scripta` 缺失搁置 |
| `ui\content\m3`（SettingsComponents 等） | 10 | **已完成（P1）** |
| `loader\entry\zygisk` | 5 | **不迁**（Kiora 保持纯 Xposed） |
| 其余零散 | ~120 | 随批处理；P4 复跑 `_gap.ps1` 后重新聚合 |

## 依赖拓扑

```
L0 已就绪（220 项在用）
   showComposeDialog / AlertDialogContent / TextButton / Button / DefaultColumn
   SegmentedColumn / SwitchWidget / BaseWidget / BaseItemContainer / BaseSupportingWidget
   ListItem / NumberPickerWidget（上游完整版）/ KvStore.prefOption / showToast / panel 全套
   + 服务层 WeApiRegistry（dexBacked 19 + startupBacked 19）
   + ktor 3.6.0 / okhttp3 5.5.0 / coil3 / material3 1.5.0-alpha28 / miuix 0.9.4-rc01
   + ARSCLib 1.4.0 / apksig 9.3.3 / bouncycastle 1.86（P3 主题栈）

L1 公共层 —— 已完成（`dbeca51`）
   ui\content: DexResolver / LocalDexResolver / OsmLocationPicker / WeKitBasicDialog
   ui\content\m3: SettingsComponents / ExpressiveBackButton
   features\api\ui: WeChatSettingsManager / WeViewTreeLifecycleProvider
   dexkit: DexCacheManager / ResolutionCoordinator / DexResolutionBatch
   utils: ByteArrayUtils / CryptoManager / reflection.MethodUtils / polyfills.Stream

L2 支付 / moments / contacts 组 —— 已完成（`4c7db17`）
   支付 5 项、moments 7 项、contacts 3 项、voip 1 项

L3 主题栈 —— 已完成（P3 提交）
   utils\monet 12 文件；beautify 7 项全解锁
   ui\utils\theme 8 / ui\content\{liquid 4, animation 2} 经逐目录 diff 确认 Kiora 早已齐备
   ui\content\nuke 只留 Theme/Motion 两件（`kyant0.backdrop` / `kyant0.shapes` 已在依赖清单）

L4 需决策
   agent 全栈（~216 声明 + WeAgent 功能 1 项）
   scripting_python 编辑器屏（23 声明，需未发布的 scripta 复合构建）
   ~~lsparanoid~~ —— 已定案：不引入，且已补齐其替代路径的宿主资源注入
```

## 剩余批次

### P0 收口（无编译风险，优先）
- [x] 重导 DexKit 云端报告 —— **摸清上限，重出无增益，改由真机重扫**。实测：树内目标
      DexKit technicalId **132**，合并 4 份历史导出后覆盖 **60**，缺 **72**；线上资产
      （`Kiora-wechat` release id `402951057`，资产 `wechat-8.0.78-3180-domestic.json` 76043 B）
      与本地重生成结果 **同为 61 features / 276 descriptors**，仅条目顺序不同（查表不敏感）⇒
      线上已含全部可得数据；77 个本地 JSON 里对 72 个缺口命中 **0**。`gh` CLI 未登录、无
      `GH_TOKEN` ⇒ 也无法上传，且上传无增益。覆盖率不足的后果只是冷启动走本地扫描，功能不受损。
- [x] 复检 P0 风控 —— **已确认落地**：`cn\hxy\kiora\common\Startup.kt:149` 只剩注释（原
      `LogUtils.logEnvironment()` 调用已删）；`cn\hxy\kiora\wx\host\WeChatHostAdapter.kt:79`
      `accountAnchor` 仍为 `null`，消费点 `hook\MainHook.kt:111` 安全退化。
- [x] 离线自查：注册表 **217 项 technicalId 全distinct、零撞键**（DexKit 缓存键
      `"${feature.technicalId}->$key"`，双血统注册表最易在此出问题；逐个按 `object <Entry>`
      块作用域提取，0 处回退到文件级）。

### P1 公共层（做一次解锁多批）—— 已完成 `dbeca51`
- [x] `features\api\ui\WeChatSettingsManager.kt`、`WeViewTreeLifecycleProvider.kt`
- [x] `ui\content\DexResolver.kt`、`LocalDexResolver.kt`、`OsmLocationPicker.kt`、`WeKitBasicDialog.kt`
- [x] `ui\content\m3\SettingsComponents.kt`
- [x] `dexkit\{cache\DexCacheManager, resolution\ResolutionCoordinator, resolution\DexResolutionBatch}.kt`
- [x] `utils\{ByteArrayUtils, CryptoManager, reflection\MethodUtils, polyfills\Stream}.kt`
- [x] 附带功能：`debug\{RedirectHostLogs, ResetDexCache, Experiments}`、`system\{LinkExternalAppJump, AutoLikeSportsRank, FeatureFlagManager, FakeLocation}`
- 额外：新建 `loader\utils\ActivityProxy.kt`（`ActProxyMgr` 兼容层）；`DexResolutionContext` 换上上游完整版
  （`DexHostMetadata` + `ResolutionCoordinator` 会话）；`cn\hxy\kiora\lifecycle\Parasitics.kt` 的
  `isTargetActivity` 拓宽到 `dev.ujhhgtg.wekit.` 命名空间（否则移植 Activity 无法寄生启动）。
- `DexCacheManager.methodHash` **不移植 buildSrc**，改用 `CloudDexResolver.methodHash()`（= 模块 VERSION_CODE），
  与云端报告的 `"28"` 口径对齐；否则 `isItemCacheValid` 永远失配、云端缓存整份白拉。

### P2 支付 / moments / contacts —— 已完成 `4c7db17`
- [x] payment 5：`AutoAcceptTransfers`、`AutoOpenRedPackets`、`DisplayRedPacketDetails`、`FingerprintPay`、`OpenHistoryRedPackets`
- [x] moments 7：`AutoRefresh`、`CustomDetails`、`DisplayDetails`、`FakeMomentsLikes`、`NoCompressUploadedImages`，
  外加计划漏列的 `AutoLikeMoments` / `AutoRepostMoments`（基类 `AutoMomentsBase` 不注册）
- [x] contacts 3：`AutoRemarkNewFriends`、`SplitGroupChats`、`hidecontacts\HideContactsNotifications`（→`WeApiRegistry`）
- [x] voip 1：`VirtualVoipVideo`

### P3 主题栈 —— 已完成（本次提交）
- [x] `ui\utils\theme` 8 文件 —— 逐目录 diff 后确认 **Kiora 已 100% 具备，无缺**
- [x] `ui\content\liquid` 4 / `ui\content\animation` 2 —— 同上，**无缺**
- [x] `ui\content\nuke` —— 只保留 `NukeTheme.kt` + `NukeMotion.kt`（换上上游精确版）。
      其余 10 个组件与 `activity\nuke` 5 个一起**删去**：`activity.nuke` 的消费方只有自身 +
      已删除的 `activity\settings\SettingsActivity.kt`，且 `NukeScreens`/`NukeSecondaryScreens`
      依赖 KSP 生成的 `FeaturesProvider`/`FeatureCategoryState` 栈，物理上无法编译；
      Kiora 侧 `ui\utils\theme\ThemeSettings.kt` 只用 `NukePopupAnimationMode`（在 `NukeTheme.kt`）。
- [x] `utils\monet` 12 文件 —— 全量迁入
- [x] beautify 7 项：`ApplyGlobalBackground`、`CenterProfileCard`、`CustomMessageBubbles`、`MonetEngine`
      （`ApiFeature` → `WeApiRegistry.startupBacked`）、`MonetEngineModuleGenerator`、`ReplaceNavigationBar`、`Themes`
- 附带：`ui\content\m3\NumberPickerWidget.kt` 换上游完整版（Kiora 精简版缺 `icon`/`iconPlaceholder`/
  `description`/`showTooltip` 与 `SliderState` API，`ApplyGlobalBackground.kt:311` 传 `icon` 直接编译失败；
  该文件全部 25 处调用点均来自上游，`ImageRotation.kt` 是唯一行号有偏移的）。
- 新增依赖（离线缓存已齐）：ARSCLib 1.4.0（上游接法：自定义 `arsclibSource` configuration +
  `prepareAndroidArsclib` Jar task 剔除 `android/**`、`org/xmlpull/v1/**`，避免 R8 把
  `AttributeSet::class` 改写成 ARSCLib 自带的混淆副本）、apksig 9.3.3、bcprov/bcpkix 1.86。
- 打包排除：`META-INF/LICENSE.md`（bcprov/bcpkix/bcutil 三件套各带一份，直接撞
  `mergeReleaseJavaResource`）、`META-INF/BCRSA204.SF|RSA`、`META-INF/versions/**`（JDK9+ MR-JAR
  覆盖层，Android 不读）、`frameworks/android/**`、`org/bouncycastle/pqc/crypto/picnic/**`。
- **R8 无需新增 `-dontwarn`**：上游没有 `WeKit-master\app\proguard-rules.pro`，
  Kiora 侧 `assembleRelease` 在现有规则下直接通过。

### P4 决策 + 收口（Phase 4）
- [x] **lsparanoid** → 不引入（见「待决策项的当前处置」）。**scripting_python** → 核心已迁（P4-1）。
      **agent** → 仍未拍板。
- [x] **scripting_java（P4-1b）** —— 3 个根因全部落地，871 错 → 812 错 → **0 错**：
      ①`me.hd.wauxv\data\bean` 8 个 bean 复制（与上游逐字节一致）；②`bsh\NameSpace.java` 加
      `setVariable(String, Object)` 双参重载（依据 `Interpreter.java:1065 globalNameSpace.setVariable(name, value, false)`，
      即 bsh 自身 2 参约定就是 `strictJava = false`）；③`bsh\BshClassManager.java` 加
      `addClassLoader(ClassLoader)` + `additionalClassLoaders`，并在 `classForName` 的「3. 尝试外部加载器」
      之后插「3b」块（`ClassManagerImpl.classForName:145` 首句就是 `super.classForName(name)`，故基类插桩必被走到）。
      真正的阻塞是第 ④ 项：`JavaEngine.kt` 有 **141 处** `BshMethod(name, Class<?>[], lambda)` 三参构造调用，
      而 Kiora 的 bsh 只有 4 个包私有/Java-method 构造器。
- [x] **权威源确认**：`D:\code\fenxi3\_tmp_bsh` 是上游 bsh fork 的完整克隆（163 文件 / 2,158,237 B，
      含 `.git`），`_tmp_bsh\src\main\java\bsh\BshMethod.java` 28837B、`Interpreter.java` 63263B。
      两侧共有文件字节数几乎全不同（`Parser.java` 222664 vs 279082）⇒ **不可整树替换**，只按符号取增量。
      曾自造 `bsh\BshMethodHandler.java`（`throws EvalError` + 在 `invokeImpl` 基数校验后分派）取得
      812→20 的中间结果，**已回滚**，改照上游精确形态实现：
      - `BshMethod` 内嵌 `@FunctionalInterface public interface MethodCallback { Object invoke(Object[] args); }`（**无 `throws`**）
        + `private transient MethodCallback methodCallback;`
      - 9 参构造器补 `else if (paramTypes != null) this.paramCount = paramTypes.length;`（上游同款分支）
      - `public BshMethod(String name, Class<?>[] paramTypes, MethodCallback callback)` → 转调 9 参后存字段
      - 分派点在 `invoke(...)` 的 null 元素检查之后、`javaMethod` 分支之前：
        `if (methodCallback != null) return invokeMethodCallback(argValues, callerInfo, callstack);`
      - `invokeMethodCallback` 逐参 `argValues[i] = Primitive.unwrap(Types.castObject(argValues[i], paramType, Types.ASSIGNMENT))`，
        `paramType == null` 跳过，`UtilEvalError` → `EvalError("Invalid argument: `paramName' for method: name : ...")`
- [x] `bsh\Interpreter.java` 快照 API 照搬上游：`compileSnapshot(Reader, NameSpace, String)` /
      `compileSnapshot(String[, String])` / `compileSnapshot(String inputPath, String outputPath, SecretKey)` /
      `evalSnapshot(BshSnapshot[, NameSpace, String])` / `evalSnapshot(InputStream, SecretKey[, String])` /
      `evalSnapshot(File, SecretKey)` / `evalSnapshot(String, SecretKey)` + 私有 `readSource` / `preprocessScript` /
      `stripSnapshotRuntimeState`。配套复制 `bsh\preprocess` 4 文件
      （`AnnotationIgnorePreprocess` 7974B、`DefaultArgsDesugar` 27260B、`GenericPreprocessor` 34493B、`KtStringTemplate` 11290B，
      均为 `java.util` 自闭环）；`BshSnapshotHelper.kt` 的 `writeEncrypted`/`readEncrypted` 加 `@JvmStatic`
      （Java 侧无需 `.INSTANCE`）。Kiora 侧前提件全在：`get_jjtree()`、`terminatedScript`、`pathToFile`、
      `readLine()`、7 参 `Interpreter(Reader, PrintStream, PrintStream, boolean, NameSpace, Interpreter, String)`。
- [ ] 剩余 3 项未注册功能：`ChatToolbar`、`ForwardMessages`、`WeAgent`（处置见「当前状态」）。
- [ ] 已知上游缺陷（**未修，按原样保留**）：`JavaEngine.kt:609` 脚本 API `compileSnapshot(path)` 以
      `compileSnapshot(resolved, snapPath, null)` 传 **null** SecretKey，而 `BshSnapshotHelper.writeEncrypted`
      对 null key 会 `InvalidKeyException`（Kotlin 侧更早触发 `Intrinsics` 非空检查）；同族 `evalSnapshot(path)`
      却用 `BshSnapshotDecompiler.SECRET_KEY` 读取。上游 Java 版同样没有 null-key 分支 ⇒ 该脚本 API 在上游也是
      静默失效（被 `runCatching` 吞掉只打日志）。若日后要修，正确改法是把第三参换成 `BshSnapshotDecompiler.SECRET_KEY`。

### P4-2 全树声明对齐（已复测收敛）

判定方法（单一、可复现）：对上游 `app\src\main\java` 下逐个 .kt 做
①**相对路径存在性** → ②缺失者再按**顶层声明名**在 Kiora 全树（.kt + .java，1116 文件）索引里查一次
→ ③仍缺者按排除区切分。不看单一口径。

- 上游 .kt 总数 **776**；相对路径已存在 **608**，缺失 **168**。
- 168 中属排除区 **121**：`agent\*`、`ui\agent\*`、`ui\agent\settings\*`、`features\api\agent\*`、
  `features\items\system\agent\*`、`activity\agent\*`、`activity\nuke\*`、`ui\content\nuke\*`、
  `activity\settings\*`、`loader\entry\{zygisk,frida}\*`。
- 非排除区缺失 **47**，其中顶层声明名已在 Kiora 别处存在（**Kiora 拍平/改名/自有实现**）**25**：
  `features\api\core\*`(15) 与 `features\api\core\models\*`(5) 是 Kiora 把 `core` 层拍平
  （实际在 `features\api\WeMessageApi.kt`、`features\api\models\MessageInfo.kt`），
  `activity\MainActivity.kt` 由 `cn.hxy.kiora.activity.MainActivity` 取代，
  `loader\entry\common\ModuleLoader.kt` 由 Java 版 `cn\hxy\kiora\common\ModuleLoader.java:59-60` 取代，
  `utils\hook_status\HookStatus.kt` 由 `cn\hxy\kiora\utils\hook\hookstatus\HookStatus.java` 取代，
  `loader\abc\IHookBridge.kt` 由 `cn.hxy.kiora.loader.hookapi.IHookBridge` 取代。
- **真正缺件的 22 个**（本批处理掉 `SafeMode.kt` 后剩 22），逐个取证理由见「当前状态」节。

> 注：`_gap.ps1`（`D:\code\fenxi3\_gap.ps1`）报 greenfield 119 / greenfield leaves 58 / adapted 56 /
> unresolved imports 11，**该口径是上界**，实测会漏报两类已存在文件：
> ① 属性委托写法（`val X by lazy { }` / `by dexMethod`）——`$valRe` 要求名字后紧跟 `:` 或 `=`，
>    例：`ui\utils\ComposableIcons.kt` 与上游逐字节一致却被列为 greenfield；
> ② 换行 getter 的扩展属性 —— `utils\strings\WxIdUtils.kt`。
> 故「已迁/未迁」以上面的三段式判定为准。

### P4-2c 差分三分复检（全树逐文件比对）

方法：对上游每个 .kt 跑 `git --no-pager diff --no-index --ignore-cr-at-eol`，先取出**两侧都存在且仍有差值**的 68 个文件，
再按差值绝对值聚类；**负差值（删除行）必须回读 Kiora 文件本体才能定性**——Kiora 系统性以「原生控件 / 原生 `AlertDialog`」
替换上游 Compose，只看 `^-` 行必然误报，本轮由此产生的 **4 个误报已全部纠正**（`DexMethodDescriptor` / `ModifySportsStepCount` /
`QrCodeRecord` / `OpenConversation`）。

- ① **真回归 1 项（已修）**：`features\items\chat\ChatFooterHooks.kt`（−311 B）。上游同位置的两段长按绑定
  （`if (VoicePanel.isEnabled) { imgButtons.first().setOnLongClickListener { VoicePanel.openPanel(it); true } }` 与
  `if (StickerPanel.isEnabled) { imgButtons[1].setOnLongClickListener { StickerPanel.openPanel(it); true } }`）
  被换成了「面板子系统尚未迁入」的过期注释；实测面板子系统早已完整迁入（`ui\panel\VoicePanelSheet.kt` 140525 B、
  `ui\panel\PanelShell.kt` 41050 B 与上游逐字节一致，`ui\panel\StickerPanelSheet.kt` 149311 B 的唯一差值是 zygisk 多实例选择器）。
  关键佐证：`VoicePanel` / `StickerPanel` 在 Kiora 全树**只有 `ChatFooterHooks` 一个引用点**（`grep` 命中仅该文件
  `:37/:40/:45/:48`）⇒ 入口被删后 R8 把整个面板子系统判为不可达并剥离——恢复后 **APK +931,587 B**，
  删除的从来不是「注释占位」，而是这两个面板**唯一的打开入口**。
- ② **漏迁 1 项（已修）**：`features\items\miniapps\ErudaConsole.kt` 缺上游 `:25 ResourcesInjector.injectModuleRes(resources)`。
  `erudaScript` 读 `R.raw.eruda`（模块自身资源 id），宿主 `Resources` 不认，必须先注入模块 APK；补回后与上游该函数体一致。
- ③ **设计差异 10 项（无需改，逐条读 Kiora 本体确认）**：
  `WeChatInputBarMenuApi`（−2599：Compose 菜单弹窗改原生 `AlertDialog.setItems`；被删的 `performSend` 在上游全树零调用点）、
  `OpenConversation`（−2431：Compose 对话框迁到同包 `OpenConversationDialog.kt`）、
  `CrashInterceptorUtils`（−2427：改写为原生 `AlertDialog.Builder` 链）、
  `UriUtils`（−1297：CustomTabs 降级为系统浏览器——`androidx.browser` 只在离线缓存里、未进 `app\build.gradle.kts`，
  且 `ForwardIcon`/`toBitmap`/`toDp` 在 Kiora 不存在）、
  `WeSettingsInjector`（−13121：上游是「微信设置页注入器 + `openSettingsDialog`」；Kiora 只留后者，
  入口改由 `features\items\home_screen_menu\ModuleSettings.kt:32` 提供）、
  `SegmentedColumn`（−12727：上游独有的 `SegmentedItemData`/`expandableItem`/`bouncy*` 无调用点）、
  `DexDelegates`（−786：DSL 面 `dexClass`/`dexField`/`dexMethod`/`dexConstructor`/`findClassData` 全在）、
  `ComposeUtils`（−1068：见下）、`WeLogger`（−11043）/`KvStore`（−12776）（架构自有实现替换）。
- ④ **遗留可见差异 1 项（本轮不改，留 P4-3 目视）**：`ui\utils\ComposeUtils.kt` 的 `showComposeDialog` 用裸 `MaterialTheme`，
  上游是 `CommonContextWrapper(context)` + `WeKitLocaleProvider(mode = InjectedHost)` + `ModuleTheme`/`NukeModuleTheme` 分支。
  **资源解析不受影响**（`ResourcesInjector.injectModuleRes` 原地改写宿主 `Resources` 实例，见 `loader\utils\ResourcesInjector.kt:33-49`）；
  差异只是约 150 处调用点的弹窗用 Material3 基线配色而非用户主题色。单点修法存在（该处 `MaterialTheme` → `InjectedUiTheme`，
  一处生效全覆盖），但会叠加影响已自行包裹主题的 6 处（`ReplaceNavigationBar.kt:591`、`ConversationGrouping.kt:391`、
  `HomeSidePanel.kt:484/1240`、`AddMainScreenFab.kt:425`、`PanelShell.kt:262`），故作独立批次。
- 待办提示（已在 P4-2d 收口）：`WeLogger`/`KvStore` 与 `SwitchWidget`/`BaseWidget` 已逐个核完，结论见下节。

### P4-2d 控件层对齐 + 第二轮差分分诊收口

**方法**：单进程全树 `git diff --no-index --numstat` 得 **90 个**「两侧都存在但仍有差值」的文件（P4-2c 只处理了 top 项），
按目录切三份派只读子代理逐文件核（`ui/**` 12 文件；`dexkit`+`loader`+`data`+`utils` 11 文件；`utils`+`features` 26 文件），
每条要求「读 Kiora 本体 + 给依据」——避免重犯 P4-2c 的「只看删除行」假警报（本轮该法又产生 4 个假警报，均已纠正）。

**① 控件层按上游恢复（2 文件）**
- `ui\content\m3\BaseWidget.kt`（117→166 行）：补齐 4 个「声明却失效」的参数——`onTrailingClick` 的尾部独立 `Modifier.clickable`、
  `clickHaptic` 触感、`trailingDivider` 的 `VerticalDivider(Modifier.height(32.dp))`、`remember` 化的 `MutableInteractionSource`
  （原来 `trailingContent(MutableInteractionSource())` 每次重组新建实例），并把 `foreContent()` 移回 headline 叠层（原来错放进尾部 Box）。
  未采纳上游 `alpha = 0.38f` 禁用态与 `primaryContainer` 选中色：Kiora 是色彩式禁用，改动波及约 150 处调用点观感，留 P4-3 目视。
- `ui\content\m3\SwitchWidget.kt`（43→129 行）：恢复 ToggleOn/ToggleOff 触感、`leftClickAction`、
  `separateClickAreas = onClick != null || trailingDivider`、`Role.Switch` + `toggleableState` 语义、`clearAndSetSemantics` 与 Check/Close 拇指图标。
  实测 `SwitchWidget(` 无一调用点传 `trailingDivider = true` ⇒ 原判据漏项当前不可达，但仍按上游补齐。

**② 真回归 2 处（已修）**
- `features\core\SwitchFeature.kt:69-76` `applyToggle` 丢持久化：Kiora 版只改 `isEnabled`，重启即丢。上游写 `KvStore.putBool` **不可照抄**——
  Kiora `loadPersistedState`（`:26-35`）以 `HostEnv.accountPreference` 为准，`KvStore` 只在键缺席时作一次性迁移源；
  而设置页开关（`cn\hxy\kiora\hook\base\BaseSwitchHookItem.kt:29-36`，`hook\wekit\WeKitFeatureHookItem.kt:19-21` 传 `technicalId` 作 switchKey）
  写的正是账号偏好 ⇒ 改为 `HostEnv.accountPreference.edit { putBoolean(technicalId, newState) }`。
  受益调用点 7 处：`FingerprintPay.kt:299`、`AutoOpenRedPackets.kt:447`、`AutoAcceptTransfers.kt:175`、`ModifySportsStepCount.kt:216`、
  `ForceTabletMode.kt:105`、`PreventXposedDetection.kt:38`、`UseLegacyOfficialAccountsView.kt:38`（自检失败会自动关闭 ⇒ 原行为是每次启动重复开启再自关）。
- `features\items\beautify\HideHomeScreenSwipeDownPage.kt`：`:21-23` 的「ConversationGrouping 尚未迁入」为过期注释，
  把分组态 TaskBarContainer 占位硬编码成 48dp（上游 `:33-34`/`:48-49` 为 `val heightDp = if (!ConversationGrouping.isEnabled) 48 else 94`）。
  恢复后与上游**逐字节一致**（`git diff --no-index --ignore-cr-at-eol` exit 0）。原后果：开启会话分组后主页下滑「最近」页与任务栏重叠。

**③ 文档订正 3 处**：`features\core\BaseFeature.kt:24-27`（原称 reflekt 重载「切片未迁」，实际 `:156-180` 已实现，差异只是 `@JvmName` 改名）、
`data\KvStore.kt`、`utils\WeLogger.kt`（见子代理①）。

**④ 能力缺口 1 项（不可离线回滚，待决策）**：`features\items\scripting_python\PythonScriptingFeature.kt:43-45` 的 `onClick`
由「打开 Python 脚本设置页」降级为「打开 Python 运行时扩展包安装屏」。上游目标
`dev.ujhhgtg.wekit.activity.scripting_python.PythonScriptsSettingsActivity` 在 Kiora 不存在，其依赖 `libs\common\scripta`
（上游 `settings.gradle.kts:74-76 includeBuild`）在两边检出都无源码 ⇒ 现状无脚本编辑器 / 无插件启停 UI。
可复用基础件已在：`plugin\PythonPluginManager.kt:29`（被 `PythonRuntimePack.kt:129` 调用），将来可据此重写精简管理屏。

**⑤ 判 BENIGN 的 41 文件要点**：`DrawableIcons` 纯重排（去注释后逐字符一致）；`DropDownMenuWidget` 因 M3 1.4.0 无
`DropdownMenuPopup`/`SelectableDropdownMenuItem` 而折叠分组形状，绑定逻辑保留；`CloudDexResolver` 是 330 行重写且 3 个调用方均已迁移；
`DexCacheManager.methodHash` 改读 `BuildConfig.VERSION_CODE`（读写自洽，代价是版本号变动全量失效）；`BshSnapshotDecompiler` 反射字段名逐个对
`bsh\*` 声明核验通过；`HookUtils` 7 个 hook 扩展全在（原语改 `HookEngineManager.engine as? IHookBridge`）；`BaseFeature` 上游入口齐备；
`ActivityProxy` 是 48 行兼容层（Kiora 用自有 `cn.hxy.kiora.lifecycle.Parasitics`）；`AddMainScreenFab` 改开 Kiora 模块设置页属寄生启动架构非断链。

**⑥ advisories（留 P4-2f）**：`dexkit\cache\CloudDexResolver.kt:202` 下载无 8MB 上限（上游有 `MAX_REPORT_BYTES`）；
`:284-299 exportLocalReport()` 未按 `DexDelegates.isPlaceholder` 过滤即写 `"status":"SUCCESS"`。

### P4-2e 差分清单闭合（89 项全判完）

**口径修正**：`git diff --no-index --numstat` 把「路径不同」也算差异。实测 **89 个两侧都在但报差异**的文件中，
**20 个是 `features/api/core/*` → `features/api/*` 的拍平改名**（numstat `0 0`，**逐字节一致**，无需审判），
其余 **69 个**才是真实内容差异——至此 **69 项全部有结论**（分 P4-2c / P4-2d / P4-2e 三批完成，逐项依据见各节）。

**本轮新判 7 项（1 项修正 + 6 项 BENIGN）**
- `dexkit\abc\IResolveDex.kt`：Kiora 自行加的「与 WeKit 原版逐字一致」注释与事实不符（同文件另一处不含有 `DexCacheManager` 的 KDoc 链接），
  两处一并还原 ⇒ 现在与上游**逐字节一致**（`--ignore-cr-at-eol` exit 0）。
- `features\items\contacts\QuickOpenMoments.kt`：Kiora 把 `ConversationAggregation.FOLDER_PREFIX` 内联成私有常量
  `CONVERSATION_FOLDER_PREFIX = "wekit_folder_"`。实测上游 `ConversationAggregation.kt:110 const val FOLDER_PREFIX = "wekit_folder_"`，
  值完全一致（`const val` 编译期内联，改回引用不产生类初始化）⇒ 还原为上游写法，行为不变。
- `features\items\beautify\home_screen_panel\HomeSidePanelActions.kt`（−1/+1）：`WEKIT_SETTINGS` 改开
  `cn.hxy.kiora.activity.SettingActivity`（Kiora 模块设置页），与 `AddMainScreenFab.kt:443` 同类处置，属寄生启动架构非断链。
- `features\items\system\AutoCleanCache.kt`（2/2）：`private val cleanPaths = run { …; return@run paths }` → `by lazy { …; paths }`，
  把 `HostInfo.application.filesDir` 的探查从 object 初始化推迟到首次清理，属启动性能改良。
- `features\items\system\ForceTabletMode.kt`（2/2）：`import android.widget.Button as AndroidButton` 避开同文件的 Compose `Button`，
  `args[0] as? AndroidButton` 语义与上游一致（该处必须解析到 android 控件）。
- `utils\polyfills\Stream.kt`（0/1）：仅少一个文件末尾空行。

**`utils\TargetProcesses.kt`（17/27）——重写但等价，已逐环节核**
Kiora 把进程名来源从 `ActivityManager.runningAppProcesses` 反查（带 3 次重试 + `by lazy`）改为直接读 `HostInfo.processName`，
`currentName`/`currentType` 也由 `by lazy` 改为 getter。等价性依据：`cn\hxy\kiora\common\ModuleLoader.java:18-34`
`initialize(…, processName)` 由加载器入口传入**当前进程的限定名**（`:30 if (packageName.equals(processName))` 即主进程判据），
在 `Startup.init` 之前 `HostInfo.bind(packageName, processName, adapter)` 一次性写入（`host\HostInfo.kt:64-69`）；
模块自身进程不 bind ⇒ Kiora 侧 `runCatching { HostInfo.processName }.getOrDefault("unknown")`（上游同样退化成 `"unknown"` → MAIN）。
消费方是**功能分进程加载的判定**：`features\WxFeatureLoader.kt:51 TargetProcesses.isInMain`、`:55-57 val currentProcess = TargetProcesses.currentType`，
`:90-94` 与 `:350` 取 `currentName` 打日志 ⇒ 该路径已核，结论为等价。

### P0-1 云端 DexKit 报告（摸清上限，重出无增益）

- Release `Kiora-wechat`（`id=402951057`）线上唯一资产 `wechat-8.0.78-3180-domestic.json`
  （`id=609841432`，76043 B，2026-10-04 发布）。资产名由 `CloudDexReport.assetName`
  拼为 `wechat-<HostInfo.versionName>-<HostInfo.versionCode>-<domestic|google-play>.json`。
- `D:\code\fenxi3\_dexmerge.ps1` 重跑（3 份历史真机导出取并集）：276 descriptor / 61 feature /
  0 冲突 / `SELF-CHECK OK`，与线上资产同尺寸同 feature 数（SHA 不同仅因条目顺序，`select` 按 key 查表，顺序无关）。
- **真实缺口是覆盖率，不是重出**：当前代码树里有 **132 个 DexKit technicalId**，该报告只覆盖 **60**，
  **缺 72**。对 `D:\code\fenxi3` 下 77 个本地 JSON 全量解析，72 个缺口 **0 命中** ⇒
  只能靠真机重扫补齐，离线无法合成。覆盖率不足的后果是冷启动要做本地扫描（功能不受损）。
- `methodHash` 口径已核对：`CloudDexResolver.methodHash() = BuildConfig.VERSION_CODE`，
  Kiora `app\build.gradle.kts` 的 `versionCode = 28` ⇒ 合并报告里写死的 `"methodHash": "28"` 正确。
- 上传通道：本机 SSH 对 GitHub 认证可用（`Hi Hesperyx!`），但 release asset 必须走 REST API，
  `gh` CLI 未登录、环境无 `GH_TOKEN` ⇒ 当前不可执行；且如上所述**重传无增益**，留到真机重扫后一并做。

### P0-2 风控复检（已确认落地）

- `cn\hxy\kiora\common\Startup.kt:149` —— 原 `LogUtils.logEnvironment()` 调用已移除（改为注释说明来源），
  不再向宿主可读路径写环境信息（含 Xposed 框架指纹）。
- `cn\hxy\kiora\wx\host\WeChatHostAdapter.kt:79` —— `override val accountAnchor: AnchorSpec? = null`；
  消费点 `cn\hxy\kiora\hook\MainHook.kt:111 val anchor = HostInfo.adapter?.accountAnchor ?: return` 安全退化。

### 待办（离线之外）

- [ ] **P4-3 真机回归**：清 DexKit 缓存冷启动，确认无自动扫描 / 自动重启、弹窗正确关闭、无崩溃；
      逐批验证 222 项注册功能的开关与设置页；顺带在真机扫全 132 个 DexKit 键后**导出新报告并替换线上资产**
      （需要 `GH_TOKEN` 或 `gh auth login`）。
- [ ] 复核 APK 体积与 R8 规则（当前 14,493,895 B，`isMinifyEnabled=true` + `isShrinkResources=true`；
      P4-2c 前为 13,562,308 B，增量来自面板子系统重回可达集，属预期）。

## 当前状态：不可迁 / 永久搁置

以下 22 个文件是（排除区之外）真正尚未迁入的全部残留，逐个已取证。

**上游自身的问题（迁了也是死代码/残缺）**
- `features\items\notifications\CustomConversationNotifications.kt`（38227 B）—— 首行 `// TODO` /
  `// Claude has been going insane while writing this` / `// needs more review`，**全文件逐行 `//` 注释**
  （含 `Disabled feature metadata`），全树零引用（只被自己命中）⇒ 不迁。
  上游 `notifications` 分类实际只有 `NotificationsEvolved.kt` + `NotificationLocalizedResources.kt`，均已迁。
- `utils\DebugUtils.kt`（2370 B）—— 上游全树**零引用**（`debugCursor` / `debugViewTree` 等调试辅助）⇒ 不迁。
- `features\items\chat\ForwardMessages.kt` —— 上游自身缺 `ui.content.ContactsSelector`，永久搁置。
- `utils\AppUpdater.kt`（12166 B）—— Kiora 全树零调用；上游消费点只在 `NukeSecondaryScreens.kt:459` 与
  `SettingsPager.kt:863` 两个不迁的 KSP 设置页（`checkForUpdate` / `downloadAndInstall`）⇒ 无消费点，不迁。

**已有等价实现（Kiora 自有/已改名/已适配）**
- `utils\hook_status\HookStatus.kt`（1384 B）—— Kiora 自有 `cn\hxy\kiora\utils\hook\hookstatus\HookStatus.java`
  （同样基于 `io.github.libxposed.service.XposedService`），上游唯一消费点是 `activity\MainActivity.kt`。
- `application\ModuleApplication.kt`（575 B）—— Kiora 自有 Application 取代。
- `loader\entry\common\ModuleLoader.kt` —— Kiora 用 Java 版 `cn\hxy\kiora\common\ModuleLoader.java:59-60`
  （`StartupInfo.kt` 已重写为 `ModuleLoader.getMODULE_PATH()` 桥接）。
- `loader\abc\{IClassLoaderHelper 250 B, ILoaderService 428 B}` —— 无消费点，不迁。
  `IHookBridge` 的 typealias 垫片方案**已否决**：Kotlin 不支持经 typealias 访问嵌套类
  （`IHookBridge.MemberUnhookHandle` 等一律 `Unresolved reference`），改为直连
  `cn.hxy.kiora.loader.hookapi.IHookBridge`。
- `features\items\system\QrCodeRecordSettingsActivity.kt`（14278 B）—— Kiora 已把 `QrCodeRecord.kt`
  改接自有 `QrCodeRecordDialog.kt`（`onClick` → `showQrCodeRecordDialog(context)`），不再需要该 Activity。
- `data\MmkvReadonlyReader.kt`（8647 B）—— 上游唯一消费点是 `data\KvStore.kt:395` 的「旧 WeKit MMKV 迁移」
  路径；Kiora 的 `KvStore.kt` 是 SharedPreferences 重写版（`HostEnv.globalPreference` + `Kiora_Config_global`），
  自有 `requireMigrationKeys`（KvStore.kt:90），全树无 `mmkv` / `wekit_prefs` 引用 ⇒ 不迁。

**无消费点（消费方都是不迁的 KSP 设置壳）**
- `features\core\FeatureCategoryOrdering.kt`（1342 B）—— `fun featureCategoryComparator(nameComparator)`
  的消费点只有 `activity\nuke\NukeScreens.kt`、`activity\settings\FeaturesPager.kt`、其单测。
- `activity\ManagerLaunchContract.kt`（1919 B）—— `REQUEST_OPEN_LSPOSED_MANAGER = 0x574B` /
  `ACTION_OPEN_LSPOSED_MANAGER` / `EXTRA_ERROR`，消费点只有 `activity\MainActivity.kt:107`/`:191`。
  若日后要在 Kiora 设置页加「打开 LSPosed 管理器」入口，再单独接线。
- `loader\startup\{WeLauncher 1602 B, UnifiedEntryPoint 2505 B, StartupAgent 5257 B}` —— Kiora 自有
  bootstrap / 入口链路，不迁。

**依赖缺失或策略排除**
- `...\wekit\activity\scripting_python\{PythonHighlighter 7612 B, PythonScriptsSettingsActivity 44510 B}` ——
  依赖 `scripta` 编辑器，是上游未发布的复合构建（`includeBuild("libs/common/scripta")`，submodule 0 条目），
  离线无制品。`PythonScriptingFeature.onClick` 已退化为打开 Python 运行时扩展包屏。
- `loader\utils\{NativeLoader, ZygiskNativePayload 3939 B}` —— zygisk / frida 入口，Kiora 保持纯 Xposed。
- `features\items\chat\ChatToolbar.kt`（48110 B）—— 依赖 `WeAgentService` / `WeAgentOverlayController`，随 agent 决策。
- `extensions\ArchLinuxPack.kt` —— 随 agent 决策（依赖 Arch rootfs）。
- `...\wekit\activity\RootTelegramStickerSetPicker.kt`（15654 B）—— 依赖 `com.topjohnwu.superuser`（libsu 6.0.0）
  且需 `MainActivity` 处理 `ACTION_PICK_ROOT_STICKER_SETS`；`StickerPanelSheet` 的 zygisk 直连路径已剔除，
  ROOT 路径在 Kiora 下降级为 `Cancelled`（不崩），MANUAL 路径可用。留待「贴纸 root 导入」专项。

**已在本批（P4-2）补齐**
- `features\items\system\SafeMode.kt`（2097 B）—— **已迁**（含 3 条字符串资源）。
- `features\core\FeaturesLoader.kt`（171 行）—— 主体由 `WxFeatureLoader.kt` 取代，但其中**两条行为曾漏掉，
  本批已补进 `WxFeatureLoader.load()`**：
  ① 安全模式门控（上游 :43-55：`SafeMode.isEnabled` 时只 `filterIsInstance<ApiFeature>()`）；
  ② `ConversationGrouping.migrateTabStyle(BeautifyConversationList.isLayoutBeautificationEnabled)`
     （上游 :37-41；Kiora 侧 `ConversationGrouping.kt:211` 早有函数定义但**零调用点 = 死代码**，已接线）。
  未迁的 `loadDescriptorsFromCache` / `handleBrokenItems` 由 Kiora 的 `collectMissing` + 用户选择式
  `promptAndResolve` 取代（Kiora 明确不静默自动扫描 / 自动云端）。

## 明确不在本计划范围

- `extensions/` 扩展包：**已完成（P4-1）**，除 `ArchLinuxPack.kt`（随 agent 一并决策）。
- `loader/` 的 zygisk / frida 入口与 `libwekit_native.so` 编译：Kiora 保持纯 Xposed，不引入 Zygisk。
- `activity\settings`（66 声明）：Kiora 自有设置页取代；本计划只按需抽出其中的公共 Composable
  （已抽出 `activity\settings\M3ListScaffold.kt`）。
- `agent/` 与其 5 处衍生目录（`ui\agent`、`ui\agent\settings`、`features\api\agent`、
  `features\items\system\agent`、`activity\agent`）：**待决策**。规模实测上游 71+ 文件，
  含 `WeAgentService.kt` 57484 B、`WeAgentToolBindings.kt` 18263 B；且在 Kiora 侧会与
  **已有的一套自有 agent** 并存（`cn\hxy\kiora\plugin\agent\*` 9 文件 + `plugin\net\AgentService.kt` 30250 B
  + `ui\pages\plugin\AgentPage.kt` 78153 B + `ui\viewmodel\PluginViewModel.kt` 53295 B，架构完全不同：
  上游是 shell/JVM/MCP 工具调用 + proot 环境，Kiora 自有的是插件/脚本生成型 agent）。
  真实依赖缺口：`third_party/proot-static`（原生二进制）、Arch Linux rootfs、`libs/common/scripta`，
  三者离线均无制品 ⇒ **即便决定纳入，也只能迁到「能编译但运行依赖用户自备」的状态**。
