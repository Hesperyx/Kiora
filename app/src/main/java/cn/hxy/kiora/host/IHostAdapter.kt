package cn.hxy.kiora.host

import cn.hxy.kiora.utils.dexkit.DexKitTask
import cn.hxy.kiora.utils.reflect.toClass
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * 启动 / 账号切换 / 热更捕获的挂载锚点。
 *
 * [methodName] 为 null 表示锚点是**构造方法** —— QQ 的账号切换挂在
 * `BaseQQMessageFacade` 的构造之后，而不是某个普通方法。
 *
 * [paramTypes] 用于精确匹配重载；空列表即无参方法。
 *
 * 重要：解析走 `getDeclaredMethod`，**不回溯父类**。`className` 必须是
 * **真正声明**该方法的类，否则解析必然失败。微信侧就踩过这个坑：
 * `com.tencent.mm.app.Application` 并未覆写 `onCreate`，该方法由
 * `TinkerApplication` 声明。
 */
data class AnchorSpec(
    val className: String,
    val methodName: String? = null,
    val paramTypes: List<Class<*>> = emptyList()
) {

    /** 解析为普通方法；构造锚点或解析失败返回 null。 */
    fun resolveMethod(): Method? {
        val name = methodName ?: return null
        return runCatching {
            className.toClass.getDeclaredMethod(name, *paramTypes.toTypedArray())
        }.getOrNull()
    }

    /** 解析为构造方法；非构造锚点或解析失败返回 null。 */
    fun resolveConstructor(): Constructor<*>? {
        if (methodName != null) return null
        return runCatching { className.toClass.declaredConstructors.firstOrNull() }.getOrNull()
    }
}

/**
 * 宿主适配器：QQ / TIM / 微信 的差异全部收敛在这里。
 *
 * 约定：
 * - 只暴露「宿主差异」，不承载任何业务逻辑；
 * - [startupAnchor] 返回 null 表示该宿主尚未适配，模块应**静默不生效**，
 *   而不是抛异常刷日志；
 * - 实现类应当是单例（`object`），[HostAdapters.all] 在初始化时即持有它们。
 */
interface IHostAdapter {

    /** 宿主标识，取值见 [HostTag]。 */
    val tag: String

    /** 宿主主包名，如 com.tencent.mobileqq。 */
    val packageName: String

    /** 设置页展示名，如 "QQ" / "TIM" / "微信"。 */
    val displayName: String

    /** 本适配器覆盖的全部包名（含分身包名）。 */
    val scopePackages: Set<String>

    /** 版本适配区间描述，用于设置页展示。 */
    val adaptedVersions: String

    /**
     * 是否已在**真机**验证通过。
     *
     * 未验证的宿主不计入「已支持范围」——[HostAdapters.readyPackages] 依赖本标记，
     * 因此新接入的宿主在验证前不会污染「模块是否已激活」的判定与版本展示。
     *
     * 该标记与 [startupAnchor] 是两个独立的闸门：
     * `startupAnchor != null` 只代表「代码通路已接好、可以尝试加载」，
     * [verified] 才代表「对外宣称已支持」。
     */
    val verified: Boolean

    /**
     * 热更 ClassLoader 捕获锚点；挂在该方法**之前**。
     *
     * 宿主若使用热更框架（QQ 的 QFix / 微信的 Tinker），真实类会由热更
     * ClassLoader 加载，必须先捕获它再挂业务 hook。
     *
     * 返回 null 表示宿主无热更：直接用初始 ClassLoader 启动。
     */
    val hotfixAnchor: AnchorSpec?

    /** 启动锚点；null 表示尚未适配该宿主。 */
    val startupAnchor: AnchorSpec?

    /** 账号就绪锚点；null 表示该宿主无需处理账号切换。 */
    val accountAnchor: AnchorSpec?

    /**
     * 宿主主界面就绪锚点（一个已起的 Activity 的方法）。
     *
     * 用途：DexKit 查找进度弹窗需要一个 Activity 级别的 Context，
     * 宿主 Application 起来时还没有窗口可用，必须等到主界面。
     *
     * null 表示该宿主尚无此锚点 —— DexKit 弹窗将退化为不展示（查找本身仍会执行）。
     */
    val mainUiAnchor: AnchorSpec?

    /**
     * 当前登录账号标识（QQ 为 uin，微信为 wxid）。
     *
     * 该值参与配置文件名（`Kiora_Config_<account>`），实现时**必须与改造前
     * 各调用点使用的取值一致**，否则存量用户的开关与配置会读不到。
     */
    val currentAccount: String

    /**
     * 寄生 Activity 的占位宿主 Activity 候选，按优先级排列。
     *
     * 用途：`startActivity` 的 component 会被临时替换成一个**宿主真实声明**
     * 的 Activity，绕过 AMS 的存在性校验，稍后还原为模块自身的 Activity。
     *
     * 取第一个能通过 PMS 解析的候选。优先级原则：优先「即使还原失败也不会
     * 打扰用户」的空壳 Activity，其次才是入口 Activity。
     */
    val stubActivityCandidates: List<String>

    /**
     * 伪造 ActivityInfo 时用于取模板的宿主 Activity 候选，按优先级排列。
     *
     * 取值必须是宿主包内**真实声明**的 Activity，否则 PMS 查不到；同样取
     * 第一个可解析者。
     */
    val counterfeitCandidates: List<String>

    /** 该宿主需要的 DexKit 特征任务。 */
    fun dexKitTasks(): List<DexKitTask>

    /** 某个包名是否属于本宿主。 */
    fun matches(pkg: String): Boolean = pkg in scopePackages

    /** [HostTag] 集合中是否命中本宿主；集合为空视为不限宿主。 */
    fun matchesTag(tags: Array<String>): Boolean = tags.isEmpty() || tag in tags
}
