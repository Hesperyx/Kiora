# WeKit → Kiora 融合收口计划（Phase 3/4）· 滚动版

> **Goal:** 把 WeKit 血统剩余 items 功能与其依赖层（工具 / 组件 / 数据 / 主题）按依赖闭包分批、可编译地迁入 Kiora。
> 上游 `D:\code\fenxi3\WeKit-master`，目标 `D:\code\fenxi3\Kiora`。
> 前置：`docs/superpowers/plans/2026-10-05-wekit-full-migration.md`（Phase 1/2，已完成）。
> 本文件为滚动计划：每完成一批就更新「进度快照」与「剩余批次」。

## 进度快照（2026-10-06 实测，P1+P2+P3+P4-1 已落地）

| 维度 | Kiora | WeKit 上游 | 覆盖率 |
|---|---|---|---|
| Kotlin 文件（`dev/ujhhgtg/wekit`） | **637** | 767 | 83.1% |
| items 目录文件 | **324** | 333 | 97.3% |
| `WxFeatureRegistry.all` 注册 | **216** | — | — |
| `WeApiRegistry`（dexBacked 19 + startupBacked 19） | **38** | — | — |
| 上游 items 功能对象已注册 | **221** | 225 | 98.2%（**未注册 4**） |
| api 层文件 | 68 | 82 | 82.9% |
| ui 层文件 | 76 | 99 | 76.8% |
| data 层文件 | 18 | 19 | 94.7% |
| utils 层文件 | 73 | 75 | 97.3% |
| i18n 层文件 | 9 | 9 | 100%（简化实现） |
| dexkit 层文件 | 11 | 10 | 100%+（Kiora 多 1 个自有文件） |
| extensions 层文件 | 13 | 13 | 100%（两侧集合有差：Kiora 无 `ArchLinuxPack`（随 agent），上游无 `ExtensionPacksProvider`） |
| python 层文件（vendored） | 10 | 0（上游在 `libs/python-runtime-api`） | — |
| loader 层文件 | 5 | 21 | 23.8%（`entry\*` zygisk/frida 不迁） |
| activity 层文件 | 6 | 23 | 26.1%（`activity\nuke` 不迁；`activity\settings` 只按需抽件） |
| 全树缺失声明（FQ 名对齐） | — | 2147 | 616 → **待 P4-2 复测**（旧口径基于 547/767） |

- 编译基线：`:app:compileReleaseKotlin --offline --no-daemon` → `errors: 0`；
  `:app:assembleRelease --offline --no-daemon` → `BUILD SUCCESSFUL`，APK **13,414,588 B**。
- `git diff --cached --check` → exit 0。

### 已落地批次（新→旧）

| 提交 | 内容 |
|---|---|
| `P4-1`（本次） | scripting_python 核心 + extensions 扩展包栈：`python\api` 10 接口（vendoring）、`scripting_python` 12 文件、`extensions` 6 文件（`ExtensionSupport`/`ExtensionPackRegistryValidation`/`ScriptDepsPack`/`ExtensionPackDialogs`/`PythonRuntimeArchive`/`PythonRuntimePack`）、`activity\settings\ExtensionsSettingsActivity.kt`、`loader\utils\HybridClassLoader.kt`、`ClassLoaders` 补 `BOOT`/`HYBRID`；`ExtensionPacksProvider.ALL_PACKS` 由 `emptyList()` 改为登记两包；注册 215→216；APK 13,414,588→13,474,920 B |
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
- **lsparanoid**：**已定案——不引入**。证据：Kiora 全树零 import、仅 1 处注释提及
  （`i18n\LocalizedContextFactory.kt:16`）；上游全树也仅 3 个文件引用 `LspBootstrap`/`LspResourceContext`。
  Kiora 的 `i18n\` 9 文件与上游逐字节等价（`LocalizedContextFactory.kt` 唯一差 44 B = 那段注释），
  说明替代路径已运行。收益只有 release 变体的类名/字符串混淆（反分析硬化），**不解锁任何功能**；
  成本却是 mavenLocal group 白名单 + NDK 29.0.14206865 + omvll + arm64-only + 变体保护逻辑。
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
   scripting_python（23 声明，需 Python 运行时）
   lsparanoid（18 项 i18n 完整形态）
```

## 剩余批次

### P0 收口（无编译风险，优先）
- [ ] 重导 DexKit 云端报告：注册数已 60 → **187**，报告与 Release `Kiora-wechat` 资产需重出并上传
      （`uploads.github.com/repos/{o}/{r}/releases/{id}/assets?name=xxx` 直传 + `gitproxy.mrhjx.cn` 镜像校验）。
- [ ] 复检 P0 风控：`common/Startup.kt:134` 的 `LogUtils.logEnvironment()` 不再写宿主可读的
      `/sdcard/Android/data/com.tencent.mm/Kiora/global/log/environment_info.txt`；
      核对 `WeChatHostAdapter.kt:75 accountAnchor` 仍为 null（`MainHook.hookAccountChange()` 因此整体 return）。

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
- [x] **lsparanoid** → 不引入（见「待决策项的当前处置」）。**scripting_python** → 核心已迁（本次）。
      **agent** → 仍未拍板。
- [ ] 剩余 4 项未注册功能：`ChatToolbar`、`ForwardMessages`、`JavaScriptingHook`、`WeAgent`。
      `JavaScriptingHook` + `JavaEngine` + `JavaPlugin` 已从上游复制但**未纳入本次提交**：编译出
      871 错误，全部收敛到 3 个 Kiora 侧差异 ——
      ①`me.hd.wauxv.data.bean.{MsgInfoBean,ContactLabelBean}` 与 `.info.{FriendInfo,GroupInfo}` 缺件；
      ②`bsh.NameSpace.setVariable(String, Object)` 双参重载缺失（Kiora 的 bsh 只认三参 `strictJava`）；
      ③`bsh.classpath.ClassManager.addClassLoader(ClassLoader)` 缺失。三者均为加性兼容垫片，
      单独成批（P4-1b）处理。
- [ ] 复跑全树声明对齐盘点至收敛；清理 `WeApiRegistry` / `WxFeatureRegistry` 重复项与顺序问题。
- [ ] 真机微信回归：清 DexKit 缓存冷启动，确认无自动扫描 / 自动重启、弹窗正确关闭、无崩溃；
      逐批验证 221 项注册功能的开关与设置页。
- [ ] 复核 APK 体积与 R8 规则（当前 13,474,920 B，`isMinifyEnabled=true` + `isShrinkResources=true`）。

## 当前状态：不可迁 / 永久搁置

- `features\items\chat\ForwardMessages.kt` —— 上游自身缺 `ui.content.ContactsSelector`，永久搁置。
- `...\wekit\activity\scripting_python\{PythonHighlighter,PythonScriptsSettingsActivity}.kt` ——
  依赖 `scripta` 编辑器，而 `scripta` 是上游未发布的复合构建（`includeBuild("libs/common/scripta")`，
  submodule 0 条目），离线环境无制品。`PythonScriptingFeature.onClick` 已退化为打开 Python 运行时扩展包屏。
- `features\items\scripting_java\{JavaEngine,JavaPlugin,JavaScriptingHook}.kt` —— 待 P4-1b
  （bsh 双参 `setVariable` / `addClassLoader` 垫片 + `me.hd.wauxv` 四个数据 bean）。
- `...\wekit\loader\abc\{IClassLoaderHelper,ILoaderService}.kt` —— 无消费点，不迁。
  `IHookBridge` 由 typealias 垫片方案**否决**：Kotlin 不支持经 typealias 访问嵌套类
  （`IHookBridge.MemberUnhookHandle` 等一律 `Unresolved reference`），改为直连
  `cn.hxy.kiora.loader.hookapi.IHookBridge`。
- `features\items\chat\ChatToolbar.kt` —— 依赖 `WeAgentService` / `WeAgentOverlayController`，随 agent 决策。
- `...\wekit\activity\RootTelegramStickerSetPicker.kt` —— 依赖 `com.topjohnwu.superuser`（libsu 6.0.0）
  且需 `MainActivity` 处理 `ACTION_PICK_ROOT_STICKER_SETS`；`StickerPanelSheet` 的 zygisk 直连路径已剔除，
  ROOT 路径在 Kiora 下降级为 `Cancelled`（不崩），MANUAL 路径可用。留待「贴纸 root 导入」专项。

## 明确不在本计划范围

- `extensions/` 扩展包：**已完成（P4-1）**，除 `ArchLinuxPack.kt`（随 agent 一并决策）。
- `loader/` 的 zygisk / frida 入口与 `libwekit_native.so` 编译：Kiora 保持纯 Xposed，不引入 Zygisk。
- `activity\settings`（66 声明）：Kiora 自有设置页取代；本计划只按需抽出其中的公共 Composable
  （已抽出 `activity\settings\M3ListScaffold.kt`）。
- `agent/` 与其 3 处衍生目录（`ui\agent`、`ui\agent\settings`、`features\api\agent`）：**待决策**；
  当前不计入分母，若决定纳入则新增约 216 声明 + 1 项功能。
