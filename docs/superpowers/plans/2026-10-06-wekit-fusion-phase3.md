# WeKit → Kiora 融合收口计划（Phase 3/4）

> **Goal:** 把 WeKit 血统剩余的 106 个 items 功能与其依赖层（工具 / 组件 / 数据 / 主题）按依赖闭包分批、可编译地迁入 Kiora。
> 上游 `D:\code\fenxi3\WeKit-master`，目标 `D:\code\fenxi3\Kiora`。
> 前置计划：`docs/superpowers/plans/2026-10-05-wekit-full-migration.md`（Phase 1/2，已完成）。

## 基线（2026-10-06 实测）

| 维度 | Kiora | WeKit 上游 | 覆盖率 |
|---|---|---|---|
| Kotlin 文件（`dev/ujhhgtg/wekit`） | 305 | 767 | 40% |
| items 功能对象 | 123 | 229 | 54% |
| `WxFeatureRegistry.all` 注册 | 122 | — | — |
| api/ui 服务层 | 17 | 20 | 85% |
| ui 层文件 | 15 | 99 | 15% |
| data 层文件 | 1（`KvStore.kt`） | 19 | 5% |
| utils 层文件 | 46 | 75 | 61% |
| i18n 层文件 | 9 | 9 | 100%（简化实现） |
| 缺失 items | 106 | — | — |

- 编译基线：`.\gradlew.bat :app:assembleRelease --offline --no-daemon` → `BUILD SUCCESSFUL in 19s`。
- 缺口分布：Compose 相关 93、i18n 相关 18、纯逻辑 6（这 6 项亦各有依赖墙）。
- 未提交：82 未跟踪 + 9 已改（902+/6-），`main` 停在 `244623e`。

## Global Constraints

- 不新增第三方依赖、不提交、不推送；不引入 Compose 分支到既有非 Compose 链路。
- 微信 DexKit 行为不变：冷启动不自动本地扫描、不自动强制重启，解析由 `WxFeatureLoader` 统一处理。
- 新增字符串同时补 `app/src/main/res/values/strings.xml` 与 `app/src/main/java/dev/ujhhgtg/wekit/R.kt`。
- 新增 items 必须在 `WxFeatureRegistry.kt` 注册；服务 API 进 `WeApiRegistry.dexBacked`（有 DexKit 委托）或 `startupBacked`（有 Hook 副作用）。
- 新增分类映射补到 `cn/hxy/kiora/hook/wekit/WeKitHookRegistry.kt`。
- 不做无关重构；不提交 `local.properties`、签名、`dex-reports/*`、`*.png`。
- 每批验证：`.\gradlew.bat :app:assembleRelease --offline --no-daemon` + `git diff --check`。

## 依赖拓扑（五层缺口）

```
L0 已有底座（122 项在用）
   showComposeDialog / AlertDialogContent / TextButton / Button / DefaultColumn
   SegmentedColumn / SwitchWidget / BaseWidget / BaseItemContainer / BaseSupportingWidget
   ListItem / NumberPickerWidget / KvStore.prefOption / showToast
   + 17 个 api/ui 服务层

L1 单点工具层（零设计风险）                     → 解锁 ~25 项
   ui/utils: findViewWhich / findViewsWhich / rootView / dpToPx / ComposableIcons 图标子集
             (EditIcon, DownloadIcon, ReplyIcon, DeleteIcon, ForwardIcon, ChatInfoIcon,
              StarIcon, CameraIcon, FolderAddIcon, UndoIcon, ExposurePlus1Icon)
   utils: isDarkMode / runOnUiThread / openInSystem / nul / unreachable / now
          strings.isGroupChatWxId / stripWxId / replaceEmojis
          reflection.{int, bool, BString, void, any}
          fs.{asPath, createDirsSafe, asAndroidUri}
          serialization.asStringOrNull
   data: KvStore.getBoolOrFalse        dexkit: dsl.data

L2 组件层（做一次解锁多批）                      → 解锁 ~40 项
   ContactsSelector + SingleContactSelector + BaseContactSelector   ← fan-in 18
   m3: RadioButtonWidget(9) / ColorPickerWidget(3) / DropDownMenuWidget+DropdownOption(6)
       TextFieldDialogWidget(3) / PlaceholderChips(5) / LazySegmentedItems(3)
       SettingsComponents / ExpressiveBackButton
   api/ui: WeChatMessageContextMenuApi  ← fan-in 13

L3 数据层                                       → 解锁 moments/contacts 详情类
   data/WeKitDatabase + JsonDataMigration + dao/entity/structured 子集
   api/ui: WeChatSettingsManager / WeViewTreeLifecycleProvider

L4 主题层                                       → 解锁 beautify 11 项
   ui/utils/theme 全 8 文件（Color / InjectedUiTheme / ModuleAppTheme / ModuleTheme /
     SeedResolver / Theme / ThemeSettings / Type）
   ui/content/{nuke 12, liquid 4, animation 2}
   （`kyant0.backdrop` / `kyant0.shapes` 已在依赖清单里，可直接承接）

L5 需决策（不引入依赖即无法完成）
   okhttp3        → StickerPanel / VoicePanel / FunBoxServiceClient / TelegramStickerApiClient
   lsparanoid     → 18 项 i18n 完整形态（上游：dev.ujhhgtg.lsparanoid:runtime:0.13.3）
   Coil + MaterialSymbols → batch 的 ContactsSelector 上游实现
```

## 批次划分

### P0 固化（无代码，最高优先）
- [ ] `git bundle create` 备份 + 把 91 个变更落一个 WIP commit（显式路径排除 `.idea`、`local.properties`、签名、`dex-reports/*`、`*.png`）。
- [ ] 重导 DexKit 报告至 122 项并上传 Release `Kiora-wechat`（`uploads.github.com` 直传 + `gitproxy.mrhjx.cn` 镜像校验）。
- [ ] 修 P0 风控：`common/Startup.kt:134` 的 `LogUtils.logEnvironment()` 不再写
      `/sdcard/Android/data/com.tencent.mm/Kiora/global/log/environment_info.txt`；核对 `WeChatHostAdapter.kt:75 accountAnchor`。

### P1 零新增依赖 5 项（其缺失依赖全部落在 L0）
- [ ] `chat/ReadReceipts.kt`（864 行；依赖 `WeChatInputBarMenuApi` + `WeCurrentConversationApi` + `ClickableFeature` + `showToast`，均已存在）
- [ ] `chat_input_bar_menu/SendCardMessage.kt`（60 行）
- [ ] `contacts/RoundAvatars.kt`（133 行；`AlertDialogContent` + `IntNumberPickerWidget` + `SegmentedColumn` + `showComposeDialog`）
- [ ] `official_accounts/UseLegacyOfficialAccountsView.kt`（需先验证简化版 `i18n/HostLocalizedStrings`）
- [ ] `system/PreventXposedDetection.kt`（同上，需 `i18n/HostLocalizedStrings`）

### P2 L1 工具层（一批）
- [ ] 补 ui/utils 查找与图标、android/reflection/strings/fs 工具函数、`dexkit.dsl.data`、`KvStore.getBoolOrFalse`。
- [ ] 迁「只缺 1 个依赖」项中的工具型：`debug/RedirectHostLogs`、`debug/ResetDexCache`、
      `payment/AutoAcceptTransfers`、`system/LinkExternalAppJump`、`moments/AutoRefresh`、
      `chat/MessageEntranceAnimation`。

### P3 组件层（三批）
- [ ] P3a `api/ui/WeChatMessageContextMenuApi`（fan-in 13，原生实现）→ 迁 chat 菜单类 12 项：
      `RemoveChatMessageContextMenuItems`、`BatchRevoke`、`DownloadFilesToLocalStorage`、
      `DownloadImagesToLocalStorage`、`ModifyTextMessageDisplay`、`RepeatMessages`、
      `SaveStickersToLocalStorage`、`SaveVoicesToLocalStorage`、`ForwardMessages`、
      `DisplayMessageDetails`、`QuickRevokeAndEdit`、`ForwardMessagesToMoments`。
- [ ] P3b `ContactsSelector` 系（fan-in 18）→ 迁 `batch/*` 7 项 + `chat/AutoCacheFiles` +
      `chat/AutoCacheImages` + `chat/BlockAtAllNotifications` + `contacts/SplitGroupChats` +
      `moments/FakeMomentsLikes` + `moments/ForwardMessagesToMoments`。
- [ ] P3c m3 控件补齐（RadioButton / ColorPicker / DropDownMenu / TextFieldDialog /
      PlaceholderChips / LazySegmentedItems）→ 补齐剩余 ~20 项。

### P4 L3 + L4 数据与主题层
- [ ] `data/WeKitDatabase` + `JsonDataMigration` + `dao`/`entity`/`structured` 子集 →
      `moments/CustomDetails`、`moments/DisplayDetails`、`contacts/CustomLocalFriendAvatars`。
- [ ] `ui/utils/theme` 8 文件 + `ui/content/nuke|liquid` → beautify 11 项
      （含 `Themes`、`MonetEngine`、`ApplyGlobalBackground`、`CustomMessageBubbles`、
      `BeautifyConversationList`、`CenterProfileCard`、`ReplaceNavigationBar` 等）。

### P5 决策 + 收口（Phase 4）
- [ ] 依决策结果处理 okhttp3 / lsparanoid 相关项；未引入的列入「暂不迁」清单。
- [ ] 复跑盘点至缺失收敛；清理 `WeApiRegistry` / `WxFeatureRegistry` 重复项与顺序问题。
- [ ] 重导 122+ 项云端报告；真机微信回归：清 DexKit 缓存冷启动，确认无自动扫描/自动重启、弹窗正确关闭、无崩溃。

## 明确不在本计划范围

- `agent/` 子系统（71 文件）与 `extensions/` 扩展包（13 文件）：依赖 Python 运行时、扩展包索引与
  `ResourcesInjector` 栈，属独立工程，不计入 items 迁移分母。
- `loader/` 的 zygisk / frida 入口与 `libwekit_native.so` 编译：Kiora 保持纯 Xposed，不引入 Zygisk。
