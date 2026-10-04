# AGENTS.md

本文件约束在本仓库中工作的 AI/Coding Agent。人类贡献者请以 `CONTRIBUTING.md` 为准。

## 项目概况

Kiora 是基于 Xposed 的 QQ / TIM / 微信增强模块，Kotlin + Compose，多宿主共用一套 `host` 抽象。
当前主线同时包含 QQ 原生功能与微信侧 WeKit 血统功能迁移，改动前先确认目标宿主。

## 硬性规则

- 只修改与任务直接相关的文件，不做无关重构、格式化或“顺手修复”。
- 未收到明确指令不得创建分支、提交或推送。
- 不提交 `local.properties`、签名文件、`dex-reports/pulled-*`、`dex-reports/*.png` 等本地产物。
- 不新增第三方依赖或存储引擎，除非任务明确要求。
- 配置兼容：`Kiora_Config_*`（SharedPreferences）和 `Kiora/data`（ObjectStore JSON）是存量数据，不得随意改键名或目录。
- 宿主隔离：QQ 专用逻辑放在 `cn.hxy.kiora.qq`，微信专用逻辑放在 `cn.hxy.kiora.wx`；跨宿主改动先走 `HostInfo` / `IHostAdapter`。
- 微信 DexKit 行为约束：冷启动不能自动本地扫描、不能自动强制重启；解析流程由 `WxFeatureLoader` 与 `NoticeDialog` 统一处理。
- 微信账号锚点尚未接通，`currentAccount` 冷启动可能回退 `global`；涉及配置目录/文件名时要考虑这个现状。

## 关键入口

- 宿主抽象：`app/src/main/java/cn/hxy/kiora/host/`
- 通用 Hook 基类：`app/src/main/java/cn/hxy/kiora/hook/base/`
- QQ 宿主：`app/src/main/java/cn/hxy/kiora/qq/`
- 微信宿主：`app/src/main/java/cn/hxy/kiora/wx/`
- 微信 WeKit 功能：`app/src/main/java/dev/ujhhgtg/wekit/features/`
- DexKit 缓存：`app/src/main/java/cn/hxy/kiora/utils/dexkit/DexKitCache.kt`

## 构建与验证

```bash
# Windows
gradlew.bat :app:assembleDebug --offline --no-daemon
gradlew.bat :app:assembleRelease --offline --no-daemon

# Linux / macOS
./gradlew :app:assembleDebug --offline --no-daemon
```

- release 需要 `local.properties` 提供签名配置；无签名时先验证 debug 或由维护者构建。
- 完成后至少运行 `git diff --check`；改动 Kotlin/资源时应实际构建，不能只凭静态阅读声称完成。
- 真机微信回归：先 `am force-stop com.tencent.mm`，再删 DexKit 缓存并启动 `com.tencent.mm/.ui.LauncherUI`，确认无自动扫描/自动重启且弹窗正确关闭。

## 提交信息

遵循 `CONTRIBUTING.md` 的 Conventional Commits 格式，示例：

```text
fix(wx): 云端拉取后关闭选择弹窗
docs: 增加协作与 agent 规则
```

## 完成前检查

- 改动是否保持现有代码风格（Kotlin official style）。
- 是否影响配置存储、DexKit 缓存或宿主切换。
- 是否生成了应忽略的本地文件。
- 验证命令是否真正执行，并在最终说明中引用真实结果。
