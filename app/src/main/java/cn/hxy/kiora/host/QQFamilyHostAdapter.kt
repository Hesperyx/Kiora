package cn.hxy.kiora.host

import android.content.Context
import android.os.Bundle
import cn.hxy.kiora.utils.dexkit.DexKitTask
import cn.hxy.kiora.utils.json.MessageTool
import cn.hxy.kiora.utils.qq.MsgTool
import cn.hxy.kiora.utils.qq.QQCurrentEnv

/**
 * QQ 系宿主（QQ / TIM）的公共实现。
 *
 * 两者在启动锚点、账号锚点、账号获取方式上完全一致，仅包名与展示名不同，
 * 因此共用一份实现，差异通过构造参数注入。
 */
abstract class QQFamilyHostAdapter(
    override val tag: String,
    override val packageName: String,
    override val displayName: String,
    override val scopePackages: Set<String>,
    override val adaptedVersions: String
) : IHostAdapter {

    /** QQ / TIM 长期在跑，视为已验证。 */
    override val verified: Boolean = true

    /**
     * QFix 热更捕获锚点。
     *
     * 与改造前 `Startup.init` 的行为逐字等价：同样是
     * `com.tencent.common.app.QFixApplicationImplProxy.attachBaseContext(Context)`。
     * 之所以保留锚点形式而非直接写死，是为了让微信走 Tinker 的同类通路。
     */
    override val hotfixAnchor: AnchorSpec
        get() = AnchorSpec(
            "com.tencent.common.app.QFixApplicationImplProxy",
            "attachBaseContext",
            listOf(Context::class.java)
        )

    /**
     * 宿主启动锚点。
     *
     * 必须是 QQ 系宿主才存在的类 —— 微信下该类不存在，
     * 因此微信适配器不得复用本实现（见 [WeChatHostAdapter]）。
     */
    override val startupAnchor: AnchorSpec
        get() = AnchorSpec("com.tencent.common.app.BaseApplicationImpl", "onCreate")

    /** 账号切换锚点：挂在 `BaseQQMessageFacade` 的构造方法之后（methodName 为 null）。 */
    override val accountAnchor: AnchorSpec
        get() = AnchorSpec("com.tencent.imcore.message.BaseQQMessageFacade", null)

    /** 主界面锚点：改造前 DexKit 弹窗用的就是这一处，保持逐字不变。 */
    override val mainUiAnchor: AnchorSpec
        get() = AnchorSpec(
            "com.tencent.mobileqq.activity.SplashActivity",
            "doOnCreate",
            listOf(Bundle::class.java)
        )

    /**
     * 账号标识。
     *
     * **必须与改造前 `BaseSwitchHookItem.prefs` 使用的值逐字一致**：
     * 该值参与配置文件名（`Kiora_Config_<account>`），一旦变化，
     * 存量用户的全部开关与配置都会读不到。故此处直接委托原有取值入口。
     */
    override val currentAccount: String
        get() = QQCurrentEnv.currentUin

    /** 改造前的常量原值，行为不变。 */
    override val stubActivityCandidates: List<String>
        get() = listOf("com.tencent.mobileqq.activity.photo.CameraPreviewActivity")

    /** 改造前的候选数组原值，行为不变；顺序即优先级。 */
    override val counterfeitCandidates: List<String>
        get() = listOf(
            "com.tencent.mobileqq.activity.QQSettingSettingActivity",
            "com.tencent.mobileqq.activity.QPublicFragmentActivity"
        )

    /** QQ 系宿主的 DexKit 任务：注册表由 DexKitFinder 另行拼接，这里补两个工具单例。 */
    override fun dexKitTasks(): List<DexKitTask> = listOf<DexKitTask>(MsgTool, MessageTool)
}
