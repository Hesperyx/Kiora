package cn.hxy.kiora.bootstrap

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.ui.components.dialogs.CenterDialogContainerNoButton
import cn.hxy.kiora.ui.core.compatibility.KioraCenterDialog
import cn.hxy.kiora.ui.core.theme.KioraTheme
import cn.hxy.kiora.utils.dexkit.DexKitFinder
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.TAG

/**
 * DexKit 首次查找的编排层。
 *
 * 从 `utils/dexkit` 里拆出来，因为那部分不是「查找」而是「一个用例」：
 * 加载 native 库 → 判断要不要查 → 挂主界面锚点弹窗 → 查完重启宿主。
 * [DexKitFinder] 只负责「查 + 写缓存」，进度经回调传出，两边因此互不依赖 UI。
 */
object DexKitBootstrap {

    private const val READY_TEXT = "准备开始查找..."
    private const val DONE_TEXT = "查找完成，保存并重启应用"

    /** 弹窗进度文案。Compose 状态：UI 读它触发重组，查找回调写它。 */
    private var progressText by mutableStateOf(READY_TEXT)

    fun doFind() {
        // 没有缺失就别打扰用户：弹窗会顺手把应用重启一次
        if (DexKitFinder.missingKeys().isEmpty()) return

        runCatching {
            System.loadLibrary("dexkit")
            showFindDialog()
        }.onFailure { LogUtils.e("$TAG 加载 dexkit 失败", it) }
    }

    @Suppress("DEPRECATION")
    private fun showFindDialog() {
        // 主界面锚点由宿主适配器给出，不再写死 QQ 的 SplashActivity ——
        // 微信包内没有该类，写死会让弹窗这一步必然失败。
        val method = HostInfo.adapter?.mainUiAnchor?.resolveMethod()
        if (method == null) {
            LogUtils.w("$TAG 当前宿主未提供主界面锚点，跳过 DexKit 查找弹窗")
            return
        }

        method.hookAfter {
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
        // 重置文案：否则弹窗重开时会先显示上一轮的「查找完成」
        progressText = READY_TEXT

        ModuleScope.launchIO(TAG) {
            DexKitFinder.runFind(context) { tip -> progressText = tip }
            progressText = DONE_TEXT

            ModuleScope.launchMain {
                // 重启通路各宿主不通用，交给 adapter 分发
                val adapter = HostInfo.adapter
                if (adapter == null) {
                    LogUtils.w("$TAG 宿主适配器缺失，无法重启应用")
                } else {
                    adapter.restartHost(context)
                }
            }
        }
    }
}
