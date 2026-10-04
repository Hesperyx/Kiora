package cn.hxy.kiora.wx.host

import cn.hxy.kiora.host.IHostAdapter
import cn.hxy.kiora.host.AnchorSpec
import cn.hxy.kiora.host.HostTag
import android.content.Context
import android.os.Bundle
import cn.hxy.kiora.common.ModuleMeta
import cn.hxy.kiora.utils.dexkit.DexKitTask
import cn.hxy.kiora.wx.util.WeChatDexKit
import cn.hxy.kiora.wx.util.WxRestartUtils

/**
 * 微信宿主适配器。
 *
 * 锚点取值来自对微信 8.0.78 (arm64) 安装包的静态侦察，不是猜测：
 *
 * - 清单中 `android:name="com.tencent.mm.app.Application"`；
 * - `com.tencent.mm.app.Application` **继承** `com.tencent.tinker.loader.app.TinkerApplication`，
 *   即微信用的是 Tinker 热更 —— 与 QQ 的 QFix 结构同型，都是
 *   「Application 基类挂热更 → 在 attachBaseContext 内换 ClassLoader」。
 *
 * 方法声明的归属（用 dex 的 `declaredMethods` 逐类核对，已确认）：
 * - `TinkerApplication` 声明 `onCreate()V`；
 * - `com.tencent.mm.app.Application` **没有覆写 `onCreate`**，只覆写了
 *   `attachBaseContext(Context)`。
 *
 * 因此 [startupAnchor] 必须指向 `TinkerApplication` 而不是清单里的那个类 ——
 * 反射解析不回溯父类，锚错了会静默拿不到方法。
 *
 * 当前状态：[verified] 为 true（8.0.78 arm64 已真机验证），计入
 * [HostAdapters.readyPackages]，「适配版本」与激活状态都按正常宿主展示。
 */
object WeChatHostAdapter : IHostAdapter {

    override val tag = HostTag.WECHAT
    override val packageName = HostTag.PACKAGE_WECHAT
    override val displayName = "微信"
    override val scopePackages = setOf(HostTag.PACKAGE_WECHAT)
    override val adaptedVersions = ModuleMeta.ADAPTED_WECHAT_VERSION

    /** 8.0.78 arm64 已真机验证。 */
    override val verified = true

    /**
     * Tinker 热更捕获锚点。
     *
     * Tinker 在 `attachBaseContext` 内完成补丁加载并切换 ClassLoader
     * （`TinkerLoader.tryLoad`）。挂在该方法**之前**，才能让
     * [cn.hxy.kiora.common.Startup] 装下的 `BaseDexClassLoader` 构造钩子
     * 捕获到 Tinker 新建的 Loader。
     *
     * 微信的 `Application` 确实覆写了本方法，可以直接定位。
     */
    override val hotfixAnchor: AnchorSpec
        get() = AnchorSpec(
            "com.tencent.mm.app.Application",
            "attachBaseContext",
            listOf(Context::class.java)
        )

    /**
     * 启动锚点。
     *
     * 指向 `TinkerApplication` —— 见类注释，`onCreate` 由它声明。
     * Tinker 完成补丁加载后才会调用 `onCreate`，此时类已由热更 Loader 加载，
     * 与 QQ 侧「QFix 装载完毕后再挂 `BaseApplicationImpl.onCreate`」等价。
     */
    override val startupAnchor: AnchorSpec
        get() = AnchorSpec("com.tencent.tinker.loader.app.TinkerApplication", "onCreate")

    /**
     * 账号切换锚点尚未接通。
     *
     * 微信的账号体系（wxid / `SharedPreferences` 布局 / 多账号切换时机）与
     * QQ 差异很大，需要单独的锚点与账号读取实现，列入阶段 3。
     * 返回 null 时账号切换逻辑整体跳过，不影响其余 hook。
     */
    override val accountAnchor: AnchorSpec? = null

    /**
     * 主界面锚点。
     *
     * `com.tencent.mm.ui.LauncherUI` 是微信的入口 Activity，已从 8.0.78 的 dex 中
     * 核实它**自己声明**了 `onCreate(Landroid/os/Bundle;)V`（父类是
     * `MMSecDataFragmentActivity`），因此可以直接定位，无需回溯父类。
     */
    override val mainUiAnchor: AnchorSpec
        get() = AnchorSpec(
            "com.tencent.mm.ui.LauncherUI",
            "onCreate",
            listOf(Bundle::class.java)
        )

    /**
     * 当前微信账号。
     *
     * 通过 [WeChatDexKit.ACCOUNT_CURRENT]（`app.q3;->i()` 静态方法，读
     * `login_weixin_username` 回退 `login_user_name`）反射拿登录用户名，让配置
     * 文件/目录层级与 QQ 保持一致（QQ 用 QQ 号、微信用微信号，而不是都叫
     * `global`）。DexKit 缓存未就绪或取不到时回退 `"global"` 兜底。
     */
    override val currentAccount: String
        get() = runCatching {
            val method = WeChatDexKit.requireMethod(WeChatDexKit.ACCOUNT_CURRENT)
            (method.invoke(null) as? String)?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: "global"

    /**
     * 寄生占位 Activity。
     *
     * 首选 `com.tencent.mm.ui.EmptyActivity`：清单中声明为 `launchMode=singleTask`、
     * 无 `taskAffinity`，是货真价实的空壳。即便 Intent 还原失败，
     * 也只会留在一个空白页，不会把微信主界面拉起来。
     *
     * 兜底 `LauncherUI`：微信入口 Activity，`exported=true`，跨版本最稳。
     */
    override val stubActivityCandidates: List<String>
        get() = listOf(
            "com.tencent.mm.ui.EmptyActivity",
            "com.tencent.mm.ui.LauncherUI"
        )

    /**
     * 伪造 ActivityInfo 的模板候选。
     *
     * 三个都是清单中真实声明、跨版本长期存在的 Activity，就近取第一个能查到的：
     * 空壳 / 设置主页 / 入口页。
     */
    override val counterfeitCandidates: List<String>
        get() = listOf(
            "com.tencent.mm.ui.EmptyActivity",
            "com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI",
            "com.tencent.mm.ui.LauncherUI"
        )

    /**
     * 微信侧 DexKit 任务。
     *
     * 目前只有 [WeChatDexKit] 一份特征表，登记的是 WA 样例里 16 处基于
     * 字符串常量的锚点，外加 1 条自挖的账号 getter —— 每条都用
     * `recon/strref.py` 对 8.0.78 的 dex 做过全量 `const-string` 反向定位，
     * 并验证过多串 AND 的交集唯一。
     *
     * 这里返回非空是个**开关**：[cn.hxy.kiora.utils.dexkit.DexKitFinder] 见了
     * 非空名单才会去补缓存。返回空表时微信侧任何 DexKit 依赖的 hook 都会
     * 一直停在「当前环境不可用」—— 所以哪怕只有一个可用锚点也要登记。
     *
     * QQ 侧的 `MsgTool` / `MessageTool` 是纯 QQ NT 结构，不能拿到微信来跑。
     */
    override fun dexKitTasks(): List<DexKitTask> = listOf(WeChatDexKit)

    /** 微信重启：走 [WxRestartUtils]，不复用 QQ 的实现（微信没有重启加载页）。 */
    override fun restartHost(context: Context, tipText: String) {
        WxRestartUtils.restartApp(context, tipText)
    }
}
