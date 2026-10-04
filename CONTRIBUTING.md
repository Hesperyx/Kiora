# Contributing to Kiora

欢迎参与 Kiora。本文件是多人协作的统一入口：分支、提交、PR 与验收规则都按这里执行。

## 1. 环境

- JDK 21
- Android Studio（建议最新稳定版）
- Android SDK：`compileSdk 37`，`minSdk 26`，`targetSdk 37`
- 真机/模拟器：模块依赖 LSPosed / LSPatch 等 Xposed 环境，建议至少准备一台已 root 的 Android 设备。
- 首次导入后若 IDE 报 SDK 路径问题，先确认本机存在 `local.properties` 或由 Android Studio 重新生成。

## 2. 模块结构

| 模块 | 作用 |
| :--- | :--- |
| `app` | 模块主体：宿主适配、Hook、UI、插件与 WeKit 微信功能迁移 |
| `annotation` | Hook 注解声明，供 KSP 扫描 |
| `processor` | KSP 处理器，生成 Hook 注册表 |
| `qqinterface` | QQ/TIM 宿主编译时接口桩 |
| `wxinterface` | 微信宿主编译时接口桩 |

宿主适配入口在 `cn.hxy.kiora.host`；QQ 专有代码放 `cn.hxy.kiora.qq`，微信专有代码放 `cn.hxy.kiora.wx`，跨宿主通用代码放 `cn.hxy.kiora.hook` / `cn.hxy.kiora.utils`。

## 3. 分支与发布

- `main`：稳定主线，始终保持可构建。不要直接向 `main` 推送未经验证的改动。
- 功能分支：`feat/<topic>`，如 `feat/wx-chat-items`
- 修复分支：`fix/<topic>`，如 `fix/wx-dexkit-dialog`
- 文档分支：`docs/<topic>`
- 所有改动经 Pull Request 合入；合并前至少保证对应平台构建通过。

## 4. 提交信息

使用 Conventional Commits 风格：

```text
<type>(<scope>): <summary>
```

常用 type：`feat`、`fix`、`refactor`、`perf`、`test`、`docs`、`chore`、`build`。  
常用 scope：`wx`、`qq`、`tim`、`plugin`、`ui`、`dexkit`、`host`、`ci`。

示例：

```text
feat(wx): 迁入群聊快捷菜单功能
fix(wx): 修复云端拉取后弹窗未关闭
refactor(host): 统一当前账号配置读取入口
docs: 补充多人协作规范
```

## 5. 构建与验证

构建前确认 `local.properties` 存在。release 需要签名配置，本地没有签名时先用 debug 验证：

```bash
# Windows
gradlew.bat :app:assembleDebug --offline --no-daemon
# Linux / macOS
./gradlew :app:assembleDebug --offline --no-daemon
```

有 release 签名时：

```bash
gradlew.bat :app:assembleRelease --offline --no-daemon
```

产出：`app/build/outputs/apk/release/app-release.apk`（或对应 debug 目录）。

真机微信回归参考：

```text
am force-stop com.tencent.mm
删除 Kiora 的 DexKit 缓存后启动 com.tencent.mm/.ui.LauncherUI
验证：冷启动不自动扫描、不自动重启；云端拉取/本地扫描均有结果且弹窗关闭
```

## 6. PR 要求

- 一个 PR 只做一件事；不要混入无关重构、格式化或调试文件。
- 新功能需说明目标宿主、触发条件与预期结果。
- 微信侧改动必须说明 DexKit 缓存/弹窗/重启行为是否受影响。
- 改动配置存储时保持兼容 `Kiora_Config_*` 与 `Kiora/data` 旧数据。
- 不提交 `local.properties`、签名文件、`dex-reports/pulled-*` 等本地产物。
- 合并前通过 `git diff --check`，尽量附上真机或构建验证结果。

## 7. 行为准则

- 代码以现有风格为准：Kotlin official style，中文注释允许，但命名与日志关键信息保持清晰。
- 尊重宿主隔离，不把 QQ 专用逻辑无守卫地搬进微信路径。
- 问题讨论聚焦实现与证据；复现信息不足时先补充日志和宿主版本，不随意下结论。

更多面向 AI/Coding Agent 的约束见 `AGENTS.md`。
