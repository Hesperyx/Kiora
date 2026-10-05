# WeKit → Kiora 全量迁移计划

> **Goal:** 在已完成基础架构合并的前提下，把 `WeKit-master` 的 WeKit 血统功能按依赖闭包分批、可编译地迁入 Kiora，直至上游 items 与 UI 服务层全量可用。

## 基线

- 上游：`D:\code\fenxi3\WeKit-master`
- 目标：`D:\code\fenxi3\Kiora`
- 盘点：`D:\code\fenxi3\.workbuddy\wekit_feature_classification.csv`
- 盘点结果：上游特征对象 259，Kiora 已有 98；缺 items 文件 248；缺 UI API 文件 17；缺 Compose 特征 113、i18n 特征 18、非 Compose/i18n 特征 31。
- 当前 `assembleRelease --offline --no-daemon` 已通过。

## Global Constraints

- 只迁移与任务直接相关的文件，不做无关重构、格式化或“顺手修复”。
- 不新增第三方依赖、不提交、不推送；不引入 Compose 分支到当前非 Compose UI 链路。
- 微信 DexKit 行为保持不变：冷启动不自动本地扫描、不自动强制重启，解析由 `WxFeatureLoader` 统一处理。
- 新增字符串同时补 `app/src/main/res/values/strings.xml` 与 `app/src/main/java/dev/ujhhgtg/wekit/R.kt`。
- 新增 items 必须在 `WxFeatureRegistry.kt` 注册；服务 API 按需进 `WeApiRegistry.dexBacked` 或 `startupBacked`。
- 新增分类映射补到 `cn/hxy/kiora/hook/wekit/WeKitHookRegistry.kt`。
- 每批验证 `.\gradlew.bat :app:assembleRelease --offline --no-daemon` + `git diff --check`。

## 阶段划分

- [ ] **Phase 1: 共享工具与 UI 服务底座**
  - [ ] `ui/utils/ViewUtils.kt`
  - [ ] `utils/UriUtils.kt`（裁掉 Compose/图标依赖）
  - [ ] `WeAlertDialogApi`
  - [ ] `WeCurrentConversationApi`
  - [ ] 低风险 icons 子集（`CameraIcon`、`QrCodeIcon`、`DownloadIcon`、`LinkIcon` 等）
- [ ] **Phase 2: 纯逻辑 items（非 Compose/i18n）**
  - 按 `system` → `chat` → `contacts` → `moments` → `official_accounts` → `profile` → `shortvideos` → `voip` 顺序迁移，依赖满足一批迁一批。
- [ ] **Phase 3: Compose/i18n 依赖底座**
  - 先补 Compose UI 公共层（`ui/content`、`ui/utils`、`activity` 必要的 settings 入口），再迁 Compose items。
- [ ] **Phase 4: 全量收口**
  - 复跑 CSV 盘点至缺失 0（或明确保留不迁的 Compose/i18n 清单）。
  - 清理 `WeApiRegistry`/`WxFeatureRegistry` 重复项与顺序问题。
  - 真机微信回归：清 DexKit 缓存冷启动，确认无自动扫描/自动重启、弹窗正确关闭、无崩溃。

## 执行批模板

- 复制源码或切片到 Kiora 对应目录。
- 注册 items/API，补 `strings.xml` + `R.kt`。
- 编译并修错，记录本轮迁入清单。

## 当前未提交改动保护

- 不提交 `local.properties`、签名、`dex-reports/*`、`*.png`、mipmap 等本地产物。
- 后续如提交，使用显式路径排除 `.idea`、`AndroidManifest.xml`、图标资源等无关改动。

## 进度记录（2026-10-05 会话）

- 每批均执行 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`，最近一次 `BUILD SUCCESSFUL`；`git diff --check` 仅有既有 CRLF 警告，无空白错误。
- 本轮已迁入并注册、验证：
  - `shortvideos/DownloadMedia.kt`
  - `scripting_java/JavaHookApi.kt`（新增 `me.hd.wauxv.hook.HookHandle` 兼容包装）
  - `miniapps/ErudaConsole.kt`（切片直接读打包后的 `raw/eruda.js`，不依赖 `ResourcesInjector`）
  - `system/RemoveQrCodeScanLimit.kt`（切片内联 `QrCodeRecord.methodQBarString` dex 委托，先解除对 `QrCodeRecord` 的依赖）
  - `contacts/QuickOpenMoments.kt`（切片用字面量 `wekit_folder_` 替代 `ConversationAggregation.FOLDER_PREFIX`）
  - `beautify/HideHomeScreenSwipeDownPage.kt`（切片固定 48dp，后续迁入 `ConversationGrouping` 后恢复 48/94dp 分支）

- 当前 `Compose=False` 且 `I18n=False` 的未迁 items 还剩 8：
  `DisplayGroupMemberRealName`, `StickerPanel`, `VoicePanel`,
  `HideContactsNotifications`, `RepostMoments`, `DecompileBeanShellSnapshot`,
  `PythonScriptingFeature`, `WeAgent`

- 主要阻塞按依赖分类：
  - Compose/数据层：`ConversationGrouping`、`ConversationAggregation`、`HideContacts`、`NotificationsEvolved`、`QrCodeRecordSettingsActivity`、`SettingsActivity`
  - 大型服务层：`WeChatInputBarMenuApi`、`WeMomentsApi`、`WeMomentsContextMenuApi`、`WeSettingsInjector`
  - 原生/资源/运行时：`NativeCrashHandler`（JNI）、`BshSnapshotDecompiler`（bsh.snapshot）、`PythonRuntimeLoader`、`WeAgentSettingsActivity`
  - 下一步建议优先迁 `WeMomentsContextMenuApi` + `WeMomentsApi` 或先补 Compose UI 公共层，再按依赖闭包继续。

- 本批继续迁入并注册、验证：
  - `system/PredictiveBackGestures.kt`（切掉 Compose `ThemeSettings`，改为直接读 `KvStore` + `Preferences.THEME_PREDICTIVE_BACK_ENABLED`）
  - `system/QrCodeRecord.kt` + `QrCodeRecordDialog.kt`（用原生 `AlertDialog` 记录列表/复制/清空/打开，替代上游 Compose `QrCodeRecordSettingsActivity`）
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 24s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。
- 继续迁入并注册、验证：
  - `home_screen_menu/ModuleSettings.kt`
  - `features/api/ui/WeSettingsInjector.kt`（精简原生宿主内开关列表，复用 `WeKitFeatureHookItem` 账号偏好与即时启停）
  - `ui/utils/DrawableIcons.kt` 增加 `ExtensionIcon` 别名
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 30s`。
- 继续迁入并注册、验证：
  - `features/api/ui/WeChatInputBarMenuApi.kt`（原生 `AlertDialog` 版服务 API，替代上游 Compose 菜单）
  - `chat/ChatFooterHooks.kt`（切片版：菜单/发送按钮长按入口已通；VoicePanel/StickerPanel 长按入口待面板子系统迁移后恢复）
  - `WeApiRegistry.startupBacked` 按依赖顺序注册上述两个 API feature
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 27s`。
- 继续迁入并注册、验证：
  - `scripting_java/DecompileBeanShellSnapshot.kt`
  - `utils/BshSnapshotDecompiler.kt`（用反射读取 `bsh` AST 包内私有字段，避免改动既有 bsh Java 源码）
  - `utils/BshSnapshotDecompileLaunchers.kt`
  - `activity/TransparentActivity.kt`（`ComponentActivity` 原生壳）
  - `bsh/snapshot/BshSnapshot.kt` + `bsh/snapshot/BshSnapshotHelper.kt`（保持与上游 Java 序列化字段/`serialVersionUID` 一致）
  - Manifest 注册 `TransparentActivity`；`strings.xml`/`R.kt` 补齐；`WxFeatureRegistry.kt` 注册
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 10s`。
- 剩余非 Compose/i18n 未迁 items 降为 7：`DisplayGroupMemberRealName`、`StickerPanel`、`VoicePanel`、`HideContactsNotifications`、`RepostMoments`、`PythonScriptingFeature`、`WeAgent`。
- 继续迁入并注册、验证 Moments 批：
  - `features/api/ui/WeMomentsApi.kt`（上游完整朋友圈服务 API）
  - `features/api/ui/WeMomentsContextMenuApi.kt`（朋友圈长按菜单扩展）
  - `features/items/moments/RepostMoments.kt`（转发/一键转发）
  - `WeApiRegistry.dexBacked` 增加 `WeMomentsApi`；`startupBacked` 增加 `WeMomentsContextMenuApi`
  - `WxFeatureRegistry` 注册 `RepostMoments`；`strings.xml`/`R.kt` 补齐 33 个 Moments 字符串
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 13s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 10 批（补上已复制但未收口的钱包余额显示项）：
  - `payment/ModifyWalletBalanceDisplay.kt`（修改显示余额；修复 `KvStore.getStringOrDef` 可空类型，避免表达式迁移时 `String?` 传入）
  - `WxFeatureRegistry` 注册 `ModifyWalletBalanceDisplay`；`strings.xml`/`R.kt` 补齐 7 个字符串（含 `formatted="false"` 的表达式提示）
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 30s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 11 批：
  - `chat/HideMessagesAvatars.kt`（隐藏消息头像，复用 `WeChatMessageViewApi` 与 m3 开关组件）
  - `contacts/ModifyFriendsCount.kt`（修改显示好友数量，复用 `TextView.setText` 反射 hook 与 `BaseSupportingWidget`）
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 10 个字符串
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 32s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。
- 剩余非 Compose/i18n 未迁 items 降为 6：`DisplayGroupMemberRealName`、`StickerPanel`、`VoicePanel`、`HideContactsNotifications`、`PythonScriptingFeature`、`WeAgent`。

- 继续迁入并验证 Compose 基础层（为 96 个 `Compose=True` items 铺路，不新引入第三方依赖）：
  - `ui/utils/ComposeUtils.kt`（精简版 `showComposeDialog` + `ShowComposeDialogScope`，去掉 Nuke/i18n/主题依赖，保留 ComponentDialog + ComposeView）
  - `ui/utils/ListItem.kt`
  - `ui/content/DefaultColumn.kt`
  - `ui/content/AlertDialogContent.kt`
  - `ui/content/ExpressiveWrappers.kt`（`Button`/`TextButton`/`IconButton`/`FilledIconButton`，适配 Kiora 当前 Material3 单数 `shape` API）
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 22s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。
- 注意：Kiora 当前 Material3 API 与 WeKit 上游 BOM 不一致（`ButtonShapes`/`ListItemElevation` 等已改名/移除），后续 Compose items 批迁入时需逐处适配这些包装器。

- 继续迁入并验证首个 Compose item：
  - `chat/FakeVoiceDuration.kt`（伪装语音时长，`showComposeDialog` + `AlertDialogContent` + `TextField`）
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 `dialog_cancel`/`dialog_confirm` 及功能文案
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 15s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。
- 至此已验证 Compose 基础层可承载真实 item；后续可按 `Missing` 闭包继续批量迁入仅依赖 `AlertDialogContent`/`Button`/`TextButton`/`showComposeDialog` 的 Compose items。

- 继续迁入并验证 Compose items 第 2 批：
  - `contacts/LimitGroupMemberNicknameLength.kt`
  - `system/CustomDpi.kt`
  - `system/ForceTabletMode.kt`（将 `android.widget.Button` 别名化，避免与 Compose `Button` 冲突）
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 12 个功能文案
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 10s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 3 批：
  - `debug/SendPacket.kt`（发包调试，复用现有 `WePacketHelper`/`WeProtoData`）
  - `debug/TriggerCrash.kt`（触发 Java/Native 测试崩溃，复用 `NativeCrashHandler` 与 `ListItem`）
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 34 个字符串及 `debug_send_packet_byte_count` plurals 转发
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 10s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 4 批：
  - `moments/CustomSourceApp.kt`（自定义朋友圈来源应用）
  - `payment/ModifyTransferWalletBalanceDisplay.kt`（修改转账钱包余额显示）
  - `payment/PaymentLocalizedResources.kt`（简化版：`HostInfo.application.getString` / `Context.getString`，无 LocalizedContextFactory 依赖）
  - `WxFeatureRegistry` 注册 `CustomSourceApp` / `ModifyTransferWalletBalanceDisplay`；`strings.xml`/`R.kt` 补齐 15 个字符串
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 9s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 5 批（避免 m3/图标库闭包，先吃掉三个已满足依赖的 items）：
  - `profile/SetProfileNickname.kt`（设置微信昵称，复用 `OpLog` + `WePacketHelper.sendCgi`）
  - `entertain/ClearProfileDetails.kt`（清空资料信息，复用 `ModProfileProto`）
  - `debug/LaunchInternalUrls.kt`（启动微信内部 URL）
  - `utils/unsafe/TheUnsafe.kt`（补齐上游 Unsafe 工具）
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 21 个字符串，含公共 `dialog_close` / `unknown`
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 11s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 6 批（铺最小 m3 基础层，解锁开关型设置项）：
  - `ui/content/m3/M3Shape.kt` / `SegmentedColumn.kt` / `BaseWidget.kt` / `SwitchWidget.kt`（适配 Kiora Material3 的精简版，不含动画/Coil/图标库）
  - `system/AutoApproveDeviceLogin.kt`
  - `voip/BlockVoipRingtone.kt`
  - `beautify/HideMeTabPageItems.kt`
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 21 个字符串
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 12s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 7 批（补数值选择基础件）：
  - `ui/content/m3/BaseItemContainer.kt` / `NumberPickerWidget.kt`（精简 `IntNumberPickerWidget`）
  - `entertain/ImageRotation.kt`
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 4 个字符串
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 11s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 8 批：
  - `beautify/ApplyDialogBackgroundBlur.kt`（窗口级背景模糊）
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 5 个字符串
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 11s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 继续迁入并验证 Compose items 第 9 批（补表达式引擎与支持内容组件）：
  - `utils/math/DecimalExpression.kt`（上游纯 Java 表达式引擎）
  - `ui/content/m3/BaseSupportingWidget.kt`
  - `system/ModifySportsStepCount.kt`（上传图标改为 `TextButton`，移除 MaterialSymbols 依赖）
  - `WxFeatureRegistry` 注册；`strings.xml`/`R.kt` 补齐 12 个字符串（含 `system_success` / `system_failure` / `system_invalid_format`）
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 13s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。

- 复核：`.\gradlew.bat :app:assembleRelease --offline --no-daemon` → `BUILD SUCCESSFUL in 16s`（全部 up-to-date），工作区与上一批收口一致。
- 继续迁入并验证 `chat/panel` 数据层第 1 批（无 Compose、零新增依赖）：
  - `utils/polyfills/ByteArrayInputStream.kt`（`ByteArrayInputStream.readBytes(len)` polyfill，供 FunBox 二进制编解码使用）
  - `chat/panel/PanelCoroutineUtils.kt` / `PanelCustomOrders.kt` / `PanelModels.kt` / `PanelPaths.kt` / `PanelSettings.kt`
  - `chat/panel/service/FunBoxCrypto.kt` / `service/FunBoxBinaryCodec.kt`
  - `chat/panel/sticker/StickerOnlineSourceRecovery.kt` / `sticker/TelegramStickerDatabase.kt`
  - `chat/panel/PanelImportUtils.kt`（切片：`FragmentActivity` → `ComponentActivity`，与已切片的 `TransparentActivity` 保持一致，不引入 androidx.fragment）
  - 无 items/API 注册变更（均为面板支撑层，暂未被 registry 直接引用）
- 依赖复核：`utils/fs/PathUtils.kt`（含 `moveReplacing`/`copyTo`/`copyFrom`/`asPath`）、`utils/serialization/JsonUtils.kt`（`DefaultJson`）、`data/KvStore.prefOption`、`activity/TransparentActivity.kt` Kiora 均已存在且与上游一致。
- 最近一次 `.\gradlew.bat :app:assembleRelease --offline --no-daemon`：`BUILD SUCCESSFUL in 1m 46s`；`git diff --check` 无空白错误（仅既有 LF/CRLF 警告）。
- `chat/panel` 其余文件阻塞点（未迁）：`FunBoxServiceClient.kt` / `TelegramStickerApiClient.kt` 依赖 okhttp3（Kiora 无该依赖）；`FunBoxRepositories.kt` 依赖 `loader/utils/ResourcesInjector`；`StickerPanelRepository` / `VoicePanelRepository` / `CloneVoiceRepository` / `VoiceProviders` / `TelegramStickerPackRepository` 依赖 `utils/MediaFileTypeDetector` / `utils/TelegramStickerConverter` / `features/items/chat/ChatLocalizedResources`；`VoiceSendUtils.kt` 为 Compose。
- 另记：`contacts/hidecontacts` 7 个文件全部挂在 49KB 的 Compose 伞类 `contacts/HideContacts.kt` 上（还需 `NotificationsEvolved`、`SplitGroupCall`、`ContactsSelector`），本轮未动。
