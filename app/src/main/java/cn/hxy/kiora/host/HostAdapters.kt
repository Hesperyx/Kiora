package cn.hxy.kiora.host

import cn.hxy.kiora.qq.host.QQHostAdapter
import cn.hxy.kiora.qq.host.TIMHostAdapter
import cn.hxy.kiora.wx.host.WeChatHostAdapter
/**
 * 宿主适配器注册与查找。
 *
 * 新增宿主只改这一处：加一个 [IHostAdapter] 实现类，再挂进 [all]。
 * 其余代码一律通过 [cn.hxy.kiora.host.HostInfo.adapter] 访问当前宿主。
 */
object HostAdapters {

    /** 全部已注册的适配器，顺序即设置页的展示顺序。 */
    val all: List<IHostAdapter> = listOf(
        QQHostAdapter,
        TIMHostAdapter,
        WeChatHostAdapter
    )

    /**
     * 已接通宿主的全部包名。
     *
     * 判定标准是 [IHostAdapter.verified]。注意这里有**两个独立的闸门**：
     * - `startupAnchor != null`：代码通路已接好，模块在该宿主进程内会尝试加载；
     * - `verified`：已在真机验证通过，才对外宣称「已支持」。
     *
     * QQ / TIM / 微信目前均已真机验证，全部计入本集合。
     *
     * 注意：应用内的**激活状态判定已改为逐宿主**直接读 LSPosed 作用域
     * （见 `MainActivity.buildActivations()`），不再经由本集合。
     * 本集合现在只表示「对外宣称已支持的范围」。
     */
    val readyPackages: Set<String>
        get() = all.filter { it.verified }
            .flatMap { it.scopePackages }
            .toSet()

    /**
     * 按包名查找适配器。
     *
     * 不受支持的包名返回 null —— 调用方（ModuleLoader）应据此决定是否继续初始化。
     */
    fun forPackage(pkg: String): IHostAdapter? = all.firstOrNull { it.matches(pkg) }

    /**
     * 该包名是否属于「通路已接好、可尝试加载」的宿主 —— 入口白名单唯一来源。
     *
     * 判定依据是 [IHostAdapter.startupAnchor] 非空，**不是** [IHostAdapter.verified]：
     * 后者只决定设置页是否对外宣称「已支持」，绝不能用来拦加载。早前入口处硬编码
     * `QQ || TIM`，导致微信即使作用域正常、通路已接，也会在入口被静默 return，
     * 表现为「微信里一条日志都没有」。
     */
    fun isLoadable(pkg: String): Boolean = forPackage(pkg)?.startupAnchor != null
}
