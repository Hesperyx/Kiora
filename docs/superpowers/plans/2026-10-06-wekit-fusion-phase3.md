# WeKit → Kiora 融合收口计划（Phase 3/4）· 滚动版

> **Goal:** 把 WeKit 血统剩余 items 功能与其依赖层（工具 / 组件 / 数据 / 主题）按依赖闭包分批、可编译地迁入 Kiora。
> 上游 `D:\code\fenxi3\WeKit-master`，目标 `D:\code\fenxi3\Kiora`。
> 前置：`docs/superpowers/plans/2026-10-05-wekit-full-migration.md`（Phase 1/2，已完成）。
> 本文件为滚动计划：每完成一批就更新「进度快照」与「剩余批次」。

## 进度快照（2026-10-06 18:5x 实测）

| 维度 | Kiora | WeKit 上游 | 覆盖率 |
|---|---|---|---|
| Kotlin 文件（`dev/ujhhgtg/wekit`） | **547** | 767 | 71.3% |
| items 目录文件 | **281** | 333 | 84.4% |
| `WxFeatureRegistry.all` 注册 | **187** | — | — |
| 上游 items 功能对象已注册 | 190 | 223 | 85.2%（**未注册 33**） |
| api 层文件 | 66 | 82 | 80.5% |
| ui 层文件 | 71 | 99 | 71.7% |
| data 层文件 | 18 | 19 | 94.7% |
| utils 层文件 | 56 | 75 | 74.7% |
| i18n 层文件 | 9 | 9 | 100%（简化实现） |
| dexkit 层文件 | 8 | 10 | 80% |
| **全树缺失声明（FQ 名对齐）** | — | 2147 | 缺 **616** |

- 编译基线：`:app:compileReleaseKotlin --offline --no-daemon` → `errors: 0`；
  `:app:assembleRelease --offline --no-daemon` → `BUILD SUCCESSFUL`，APK **10,909,301 B**。
- `git diff --cached --check` → exit 0。

### 已落地批次（新→旧）

| 提交 | 内容 |
|---|---|
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
- **lsparanoid**：**仍未拍板**。当前 18 项 i18n 走简化实现（`i18n\` 9 文件同名同数），
  不引入则相关功能保持「宿主语言 + 手工字符串」形态，不阻塞其余批次。
- **WeAgent / `agent/` 全栈**：仍未拍板。排除后合计影响约 **216 个声明**
  （`agent\*` 71 文件、`ui\agent\*`、`features\api\agent\*`）。

## Global Constraints

- 不新增第三方依赖、不提交、不推送；不引入 Compose 分支到既有非 Compose 链路。
- 微信 DexKit 行为不变：冷启动不自动本地扫描、不自动强制重启，解析由 `WxFeatureLoader` 统一处理。
- 新增字符串同时补 `app/src/main/res/values/strings.xml` 与 `app/src/main/java/dev/ujhhgtg/wekit/R.kt`。
- 新增 items 必须在 `WxFeatureRegistry.kt` 注册；服务 API 进 `WeApiRegistry.dexBacked`（有 DexKit 委托）
  或 `startupBacked`（有 Hook 副作用）。
- 新增分类映射补到 `cn/hxy/kiora/hook/wekit/WeKitHookRegistry.kt`。
- 不做无关重构；不提交 `local.properties`、签名、`dex-reports/*`、`*.png`、`docs/superpowers/notes/`。
- 每批验证：`:app:compileReleaseKotlin` → `:app:assembleRelease --offline --no-daemon` → `git diff --check`。

## 剩余缺口（616 缺失声明，按上游目录聚合）

| 上游目录 | 缺失数 | 归属批次 |
|---|---|---|
| `activity\settings` | 66 | **不迁**（Kiora 自有 `cn.hxy.kiora.activity.SettingActivity` 取代；仅 `M3ListScaffold` 已被单独抽出） |
| `ui\agent` + `ui\agent\settings` | 98 | 决策项（agent） |
| `ui\content\nuke` | 54 | **P3 主题栈** |
| `activity\nuke` | 39 | **P3 主题栈** |
| `agent\*` 全栈 | ~118 | 决策项（agent） |
| `utils\monet` | 36 | **P3 主题栈**（MonetEngine 动态取色） |
| `ui\content`（DexResolver/LocalDexResolver/OsmLocationPicker/WeKitBasicDialog/DexResolver 等） | 18 | **P1 公共层** |
| `features\items\beautify` | 11 | **P3** |
| `features\items\moments` | 8 | **P2** |
| `features\items\system` | 8 | **P1/P2** |
| `features\items\scripting_python` | 23 | 决策项（Python 运行时） |
| `ui\content\m3`（SettingsComponents 等） | 10 | **P1 公共层** |
| `loader\entry\zygisk` | 5 | **不迁**（Kiora 保持纯 Xposed） |
| 其余零散 | ~120 | 随批处理 |

## 依赖拓扑

```
L0 已就绪（187 项在用）
   showComposeDialog / AlertDialogContent / TextButton / Button / DefaultColumn
   SegmentedColumn / SwitchWidget / BaseWidget / BaseItemContainer / BaseSupportingWidget
   ListItem / NumberPickerWidget / KvStore.prefOption / showToast / panel 全套
   + 服务层 WeApiRegistry（dexBacked 19 + startupBacked 17）
   + ktor 3.6.0 / okhttp3 5.5.0 / coil3 / material3 1.5.0-alpha28 / miuix 0.9.4-rc01

L1 公共层（本计划下一步）
   ui\content: DexResolver / LocalDexResolver / OsmLocationPicker / WeKitBasicDialog
   ui\content\m3: SettingsComponents / ExpressiveBackButton
   ui\agent 排除后为 *零依赖* 的其余 content 文件
   features\api\ui: WeChatSettingsManager / WeViewTreeLifecycleProvider
   dexkit: DexCacheManager / ResolutionCoordinator / DexResolutionBatch
   utils: ByteArrayUtils / CryptoManager / reflection.MethodUtils / polyfills.Stream

L2 支付 / moments / contacts 组
   支付 5 项、moments 5 项、contacts 3 项

L3 主题栈（beautify 7 项全解锁）
   ui\utils\theme 8 文件 + ui\content\{nuke 12, liquid 4, animation 2}
   activity\nuke 5 文件 + utils\monet 12 文件
   （`kyant0.backdrop` / `kyant0.shapes` 已在依赖清单，可直接承接）

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

### P1 公共层（做一次解锁多批）
- [ ] `features\api\ui\WeChatSettingsManager.kt`、`WeViewTreeLifecycleProvider.kt`
- [ ] `ui\content\DexResolver.kt`、`LocalDexResolver.kt`、`OsmLocationPicker.kt`、`WeKitBasicDialog.kt`
- [ ] `ui\content\m3\SettingsComponents.kt`
- [ ] `dexkit\{cache\DexCacheManager, resolution\ResolutionCoordinator, resolution\DexResolutionBatch}.kt`
- [ ] `utils\{ByteArrayUtils, CryptoManager, reflection\MethodUtils, polyfills\Stream}.kt`
- [ ] 附带功能：`debug\{RedirectHostLogs, ResetDexCache, Experiments}`、`system\{LinkExternalAppJump, AutoLikeSportsRank, FeatureFlagManager, FakeLocation}`

### P2 支付 / moments / contacts
- [ ] payment 5：`AutoAcceptTransfers`、`AutoOpenRedPackets`、`DisplayRedPacketDetails`、`FingerprintPay`、`OpenHistoryRedPackets`
- [ ] moments 5：`AutoRefresh`、`CustomDetails`、`DisplayDetails`、`FakeMomentsLikes`、`NoCompressUploadedImages`
- [ ] contacts 3：`AutoRemarkNewFriends`、`SplitGroupChats`、`hidecontacts\HideContactsNotifications`
- [ ] voip 1：`VirtualVoipVideo`

### P3 主题栈（beautify 全解锁）
- [ ] `ui\utils\theme` 8 文件 + `ui\content\nuke` 12 + `ui\content\liquid` 4 + `ui\content\animation` 2
- [ ] `activity\nuke` 5 文件 + `utils\monet` 12 文件
- [ ] beautify 7 项：`ApplyGlobalBackground`、`CenterProfileCard`、`CustomMessageBubbles`、`MonetEngine`、
      `MonetEngineModuleGenerator`、`ReplaceNavigationBar`、`Themes`

### P4 决策 + 收口（Phase 4）
- [ ] 依决策处理 agent / scripting_python / lsparanoid 相关项；未引入者写入「暂不迁」清单。
- [ ] 复跑全树声明对齐盘点至收敛；清理 `WeApiRegistry` / `WxFeatureRegistry` 重复项与顺序问题。
- [ ] 真机微信回归：清 DexKit 缓存冷启动，确认无自动扫描 / 自动重启、弹窗正确关闭、无崩溃；
      逐批验证 187 项注册功能的开关与设置页。
- [ ] 复核 APK 体积与 R8 规则（当前 10,909,301 B，`isMinifyEnabled=true` + `isShrinkResources=true`）。

## 当前状态：不可迁 / 永久搁置

- `features\items\chat\ForwardMessages.kt` —— 上游自身缺 `ui.content.ContactsSelector`，永久搁置。
- `features\items\chat\ChatToolbar.kt` —— 依赖 `WeAgentService` / `WeAgentOverlayController`，随 agent 决策。
- `...\wekit\activity\RootTelegramStickerSetPicker.kt` —— 依赖 `com.topjohnwu.superuser`（libsu 6.0.0）
  且需 `MainActivity` 处理 `ACTION_PICK_ROOT_STICKER_SETS`；`StickerPanelSheet` 的 zygisk 直连路径已剔除，
  ROOT 路径在 Kiora 下降级为 `Cancelled`（不崩），MANUAL 路径可用。留待「贴纸 root 导入」专项。

## 明确不在本计划范围

- `extensions/` 扩展包（13 文件）：依赖扩展包索引与 `ResourcesInjector` 栈，属独立工程。
- `loader/` 的 zygisk / frida 入口与 `libwekit_native.so` 编译：Kiora 保持纯 Xposed，不引入 Zygisk。
- `activity\settings`（66 声明）：Kiora 自有设置页取代；本计划只按需抽出其中的公共 Composable
  （已抽出 `activity\settings\M3ListScaffold.kt`）。
- `agent/` 与其 3 处衍生目录（`ui\agent`、`ui\agent\settings`、`features\api\agent`）：**待决策**；
  当前不计入分母，若决定纳入则新增约 216 声明 + 1 项功能。
