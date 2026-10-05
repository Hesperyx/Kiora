package dev.ujhhgtg.wekit.features.api.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import cn.hxy.kiora.hook.wekit.WeKitFeatureHookItem
import dev.ujhhgtg.wekit.features.WxFeatureRegistry
import dev.ujhhgtg.wekit.features.core.ClickableFeature
import dev.ujhhgtg.wekit.features.core.SwitchFeature
import dev.ujhhgtg.wekit.i18n.LocaleResourceMode
import dev.ujhhgtg.wekit.i18n.LocalizedContextFactory
import dev.ujhhgtg.wekit.i18n.WeKitLocaleController

/**
 * 精简原生版 WeKit 设置入口。
 *
 * 上游 [WeSettingsInjector] 是完整的 Compose/DexKit 设置注入器；本切片只为
 * [ModuleSettings] 提供宿主内可用的开关列表。通过 [WeKitFeatureHookItem] 复用
 * Kiora 账号级偏好写入与 [SwitchFeature.isEnabled] 的即时启停逻辑。
 */
object WeSettingsInjector {

    fun openSettingsDialog(activity: Activity) {
        val localized = LocalizedContextFactory.create(
            activity,
            WeKitLocaleController.resolvedLocale,
            LocaleResourceMode.InjectedHost,
        )

        val items = WxFeatureRegistry.all
            .filterIsInstance<SwitchFeature>()
            .filter { (it as? ClickableFeature)?.noSwitchWidget != true }
            .map { feature ->
                val label = runCatching { feature.localizedName(localized) }
                    .getOrElse { feature.technicalId }
                FeatureEntry(WeKitFeatureHookItem(feature), label)
            }

        val adapter = FeatureListAdapter(localized, items)
        AlertDialog.Builder(activity)
            .setTitle(activity.localizedModuleSettingsTitle())
            .setAdapter(adapter) { dialog, position ->
                val entry = items[position]
                entry.item.isEnable = !entry.item.isEnable
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private data class FeatureEntry(
        val item: WeKitFeatureHookItem,
        val label: String,
    )

    private class FeatureListAdapter(
        private val context: Context,
        private val items: List<FeatureEntry>,
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): FeatureEntry = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val entry = items[position]
            val row = convertView as? LinearLayout ?: LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 8, 32, 8)
            }
            row.removeAllViews()

            val label = TextView(context).apply {
                text = entry.label
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                )
            }
            val checkBox = CheckBox(context).apply {
                isChecked = entry.item.isEnable
                isFocusable = false
                isClickable = false
            }
            row.addView(label)
            row.addView(checkBox)
            return row
        }
    }
}

private fun Context.localizedModuleSettingsTitle(): String =
    runCatching {
        LocalizedContextFactory.create(
            this,
            WeKitLocaleController.resolvedLocale,
            LocaleResourceMode.InjectedHost,
        ).getString(dev.ujhhgtg.wekit.R.string.feature_module_settings_name)
    }.getOrElse { "Module settings" }