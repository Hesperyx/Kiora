package cn.hxy.kiora.utils.dexkit

import android.content.Context
import cn.hxy.kiora.generated.HookRegistry
import cn.hxy.kiora.hook.base.BaseHookItem
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.TAG
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseMatcher
import java.lang.reflect.Method

/**
 * 缓存键的唯一拼接点，形如 `FakePhoneNumber->fillPhone`。
 *
 * `TAG` 取的是任务的**运行时简单类名**，所以任务类改名/挪包会让旧缓存失效
 * （触发一次全量重查 + 重启，属预期）；[DexKitFinder] 自身搬家则不影响。
 */
internal fun cacheKey(owner: Any, name: String): String = "${owner.TAG}->$name"

/**
 * DexKit 的「查 + 写缓存」。
 *
 * 只负责枚举任务、判断缓存缺失、跑查找、落盘；**不碰 UI、不负责重启** ——
 * 弹窗与重启编排在 [cn.hxy.kiora.bootstrap.DexKitBootstrap]。
 */
object DexKitFinder {

    /**
     * 需要 DexKit 查找的全部任务。
     *
     * 查找与缺失检查必须共用同一份名单。两边各写一份的话，新增任务时很容易变成
     * 「查了却不认」或者「认了却从没查过」—— 后者正是「当前环境不可用」的来源。
     *
     * 名单必须按**当前宿主**收窄：hook 项走 `shouldLoad()`（宿主 + 进程双重闸门），
     * 工具类任务由宿主适配器提供（QQ 是 MsgTool/MessageTool，微信是 WeChatDexKit）。
     * 否则在微信里会拿 QQ 的特征去搜，全是必然失败的查询，只会刷满错误日志。
     */
    private fun allTasks(): List<DexKitTask> =
        HookRegistry.hookItems
            .filterIsInstance<BaseHookItem>()
            .filter { it.shouldLoad() }
            .filterIsInstance<DexKitTask>() +
            HostInfo.adapter?.dexKitTasks().orEmpty()

    /** 某个任务在缓存里占用的键。 */
    private fun keysOf(task: DexKitTask): List<String> =
        runCatching { task.getQueryMap().keys.map { cacheKey(task, it) } }.getOrDefault(emptyList())

    /**
     * 缓存里还缺哪些键。
     *
     * 只看「缓存文件能不能读出来」是不够的：文件名只跟宿主和模块的版本号挂钩，
     * 所以**新增一个 DexKitTask 不会让旧缓存失效**。缺了这道检查，新功能会一直
     * 显示「当前环境不可用」，直到有人碰巧把版本号升上去。
     */
    fun missingKeys(): Set<String> {
        val cached = DexKitCache.cacheMap
        return allTasks().flatMap(::keysOf).filterNot { it in cached }.toSet()
    }

    /**
     * 执行查找：只补缺失的键，写回缓存并落盘。
     *
     * 进度经 [onProgress] 回调传出（形如 `MsgTool->xxx`），可能在 IO 线程触发，
     * 由调用方决定怎么消费。本函数不碰任何 UI，也不负责重启宿主。
     */
    fun runFind(context: Context, onProgress: (String) -> Unit) {
        val missing = missingKeys()
        if (missing.isEmpty()) return

        // 只补缺失的那几个，不必把整份缓存重算一遍
        val tasks = allTasks().filter { task -> keysOf(task).any { it in missing } }
        val sourceDir = context.applicationInfo.sourceDir

        DexKitBridge.create(sourceDir).use { bridge ->
            tasks.forEach { task ->
                runCatching {
                    task.getQueryMap().forEach { (name, query) ->
                        val key = cacheKey(task, name)
                        onProgress(key)

                        val descriptor = when (query) {
                            is FindClass -> bridge.findClass(query).singleOrNull()?.descriptor
                            is FindMethod -> bridge.findMethod(query).singleOrNull()?.descriptor
                            else -> null
                        }

                        if (descriptor == null) {
                            LogUtils.w("$key 没有匹配结果，宿主结构可能变了")
                        }
                        DexKitCache.cacheMap[key] = descriptor.orEmpty()
                    }
                }.onFailure { LogUtils.e(task.TAG, it) }
            }
        }
        DexKitCache.saveCache()
    }
}

interface DexKitTask {

    fun getQueryMap(): Map<String, BaseMatcher>

    fun requireClass(name: String): Class<*> {
        return DexKitCache.getClass(cacheKey(this, name))
    }

    fun requireMethod(name: String): Method {
        return DexKitCache.getMethod(cacheKey(this, name))
    }
}
