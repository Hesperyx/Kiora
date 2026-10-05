package cn.hxy.kiora.bootstrap

import android.app.Dialog
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.common.ModuleScope
import cn.hxy.kiora.hook.MainHook
import cn.hxy.kiora.host.HostInfo
import cn.hxy.kiora.ui.components.dialogs.CenterDialogContainerNoButton
import cn.hxy.kiora.ui.core.compatibility.KioraCenterDialog
import cn.hxy.kiora.ui.core.theme.KioraTheme
import cn.hxy.kiora.utils.dexkit.DexKitFinder
import cn.hxy.kiora.utils.hook.hookAfter
import cn.hxy.kiora.utils.log.LogUtils
import cn.hxy.kiora.utils.reflect.TAG
import dev.ujhhgtg.wekit.dexkit.cache.CloudDexResolver

/**
 * DexKit 首次查找的编排层。
 *
 * 从 `utils/dexkit` 里拆出来，因为那部分不是「查找」而是「一个用例」：
 * 加载 native 库 → 判断要不要查 → 挂主界面锚点弹窗 → 查完重启宿主。
 * [DexKitFinder] 只负责「查 + 写缓存」，进度经回调传出，两边因此互不依赖 UI。
 *
 * 缓存缺失时不再无条件本地扫描：挂锚点的同时**并行**尝试云端恢复
 * （[CloudDexResolver.tryRestoreMainDex]，网络在 IO 线程）。云端命中则跳过
 * 「查找方法中」弹窗 + 本地扫描 + 强制重启，直接 [MainHook.loadHook]。
 */
object DexKitBootstrap {

    private const val READY_TEXT = "准备开始查找..."
    private const val DONE_TEXT = "查找完成，保存并重启应用"

    /** 弹窗进度文案。Compose 状态：UI 读它触发重组，查找回调写它。 */
    private var progressText by mutableStateOf(READY_TEXT)

    /** 云端恢复是否已命中：命中则跳过本地扫描 + 强制重启。 */
    @Volatile
    private var cloudRestored = false

    /** 「查找方法中」弹窗引用，云端恢复在扫描前命中时用于关闭。 */
    @Volatile
    private var findDialog: Dialog? = null

    fun doFind() {
        // 没有缺失就别打扰用户：弹窗会顺手把应用重启一次
        if (DexKitFinder.missingKeys().isEmpty()) return

        // 微信宿主：DexKit 解析统一交给 WxFeatureLoader（云端/本地选择框），
        // 它负责补齐主框架键并回调 MainHook.loadHook()。这里不再弹「查找方法中」+
        // 本地扫描 + 强制重启，避免与选择框叠加、云端拉取后残留弹窗。
        if (HostInfo.isWeChat) {
            LogUtils.i("$TAG 微信宿主：主框架 DexKit 解析交由 WxFeatureLoader 处理")
            return
        }

        runCatching {
            System.loadLibrary("dexkit")
            showFindDialog()
            // 并行尝试云端恢复（网络，不能阻塞锚点回调）。命中后下一次锚点触发
            // 直接 loadHook，免本地扫描 + 重启。
            ModuleScope.launchIO("MainDexCloudRestore") {
                cloudRestored = CloudDexResolver.tryRestoreMainDex()
            }
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
            if (cloudRestored) {
                // 云端已恢复缓存，直接加载 hook，不再弹窗扫描 + 重启。
                LogUtils.i("$TAG 主框架 DexKit 已从云端恢复，跳过本地扫描")
                MainHook.loadHook()
                return@hookAfter
            }

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
            }.also { findDialog = it }

            startFind(context)
        }
    }

    private fun startFind(context: Context) {
        // 重置文案：否则弹窗重开时会先显示上一轮的「查找完成」
        progressText = READY_TEXT

        ModuleScope.launchIO(TAG) {
            // 扫描前再查一次：云端恢复可能刚命中（网络比主界面加载慢一拍），
            // 命中则关掉弹窗直接 loadHook，避免无谓的扫描 + 重启。
            if (cloudRestored) {
                LogUtils.i("$TAG 主框架 DexKit 已从云端恢复，跳过本地扫描")
                ModuleScope.launchMain {
                    findDialog?.dismiss()
                    MainHook.loadHook()
                }
                return@launchIO
            }

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
