package cn.hxy.kiora.utils.dexkit

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.tencent.mobileqq.activity.SplashActivity
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.generated.HookRegistry
import cn.hxy.kiora.ui.components.dialogs.CenterDialogContainerNoButton
import cn.hxy.kiora.ui.core.compatibility.KioraCenterDialog
import cn.hxy.kiora.ui.core.theme.KioraTheme
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.json.MessageTool
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.qq.AppRestartUtils
import cn.hxy.kiora.utils.qq.MsgTool
import cn.hxy.kiora.utils.reflect.TAG
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseMatcher
import java.lang.reflect.Method

object DexKitFinder {

    private var progressText by mutableStateOf("准备开始查找...")

    /**
     * 需要 DexKit 查找的全部任务。
     *
     * 查找与缺失检查必须共用同一份名单。两边各写一份的话，新增任务时很容易变成
     * 「查了却不认」或者「认了却从没查过」—— 后者正是「当前环境不可用」的来源。
     */
    private fun allTasks(): List<DexKitTask> =
        HookRegistry.hookItems.filterIsInstance<DexKitTask>() + MsgTool + MessageTool

    /** 某个任务在缓存里占用的键，形如 `FakePhoneNumber->fillPhone`。 */
    private fun keysOf(task: DexKitTask): List<String> =
        runCatching { task.getQueryMap().keys.map { "${task.TAG}->$it" } }.getOrDefault(emptyList())

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

    fun doFind() {
        // 没有缺失就别打扰用户：弹窗会顺手把应用重启一次
        if (missingKeys().isEmpty()) return

        runCatching {
            System.loadLibrary("dexkit")
            showFindDialog()
        }.onFailure { LogUtils.e("$TAG 加载 dexkit 失败", it) }
    }

    @Suppress("DEPRECATION")
    private fun showFindDialog() {
        SplashActivity::class.java
            .getDeclaredMethod("doOnCreate", Bundle::class.java)
            .hookAfter {
                val context = it.thisObject as Context

                KioraCenterDialog(context) {
                    CenterDialogContainerNoButton(title = "查找方法中") {
                        val colors = KioraTheme.colors
                        Text(
                            text = progressText,
                            fontSize = 15.sp,
                            color = colors.textSecondary,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }.apply {
                    setCanceledOnTouchOutside(false)
                    setCancelable(false)
                    show()
                }

                startFind(context)
            }
    }

    private fun startFind(context: Context) {
        ModuleScope.launchIO(TAG) {
            val missing = missingKeys()
            // 只补缺失的那几个，不必把整份缓存重算一遍
            val tasks = allTasks().filter { task -> keysOf(task).any { it in missing } }

            val sourceDir = context.applicationInfo.sourceDir
            DexKitBridge.create(sourceDir).use { bridge ->
                tasks.forEach { task ->
                    runCatching {
                        task.getQueryMap().forEach { (name, query) ->
                            val tip = "${task.TAG}->$name"
                            progressText = tip

                            val descriptor = when (query) {
                                is FindClass -> bridge.findClass(query).singleOrNull()?.descriptor
                                is FindMethod -> bridge.findMethod(query).singleOrNull()?.descriptor
                                else -> null
                            }

                            if (descriptor == null) {
                                LogUtils.w("$tip 没有匹配结果，宿主结构可能变了")
                            }
                            DexKitCache.cacheMap[tip] = descriptor.orEmpty()
                        }
                    }.onFailure { LogUtils.e(task.TAG, it) }
                }
            }
            progressText = "查找完成，保存并重启应用"
            DexKitCache.saveCache()
            ModuleScope.launchMain {
                AppRestartUtils.restartApp(context)
            }
        }
    }
}

interface DexKitTask {

    fun getQueryMap(): Map<String, BaseMatcher>

    fun requireClass(name: String): Class<*> {
        return DexKitCache.getClass("${TAG}->$name")
    }

    fun requireMethod(name: String): Method {
        return DexKitCache.getMethod("${TAG}->$name")
    }
}
