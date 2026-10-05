package dev.ujhhgtg.wekit.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Kiora 切片版：保留 `ACTION_VIEW` 打开与文件扩展名读取。
 *
 * 上游 `useCustomTabs = true` 依赖 androidx.browser、`ForwardIcon` 与 Compose/图标
 * 工具链；当前宿主无 androidx.browser 依赖，且相关 Compose UI 尚未迁移，因此
 * 这里将参数保留但统一走系统浏览器。迁移 Compose 设置页时再按需补回。
 */
fun Uri.openInSystem(
    context: Context,
    useCustomTabs: Boolean = false
) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = this
    context.startActivity(intent)
}

inline val Uri.fileExtension: String
    get() = pathSegments.last().substringAfterLast('.', "")
