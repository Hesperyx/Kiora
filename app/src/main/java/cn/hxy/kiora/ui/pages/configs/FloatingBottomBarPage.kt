package cn.hxy.kiora.ui.pages.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import cn.hxy.kiora.conf.FloatingBarConfig
import cn.hxy.kiora.ui.components.listitems.PreferenceSection
import cn.hxy.kiora.ui.components.listitems.SelectionGroup
import cn.hxy.kiora.ui.components.listitems.SelectionItem
import cn.hxy.kiora.ui.components.listitems.SwitchItem
import cn.hxy.kiora.ui.components.scaffold.ConfigPageScaffold

@Composable
fun FloatingBottomBarPage(
    currentConfig: FloatingBarConfig,
    onSave: (FloatingBarConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var tempConfig by remember(currentConfig) { mutableStateOf(currentConfig) }

    ConfigPageScaffold(
        title = "悬浮底栏配置",
        configData = tempConfig,
        onSave = onSave,
        onDismiss = onDismiss
    ) { _ ->
        PreferenceSection(title = "底栏样式") {
            SelectionGroup {
                SelectionItem(
                    title = "经典",
                    subtitle = "布局手术 + 液态玻璃折射，需要 Android 13",
                    isSelected = tempConfig.implementation == FloatingBarConfig.IMPL_CLASSIC,
                    onClick = {
                        tempConfig = tempConfig.copy(implementation = FloatingBarConfig.IMPL_CLASSIC)
                    }
                )
                SelectionItem(
                    title = "新视图",
                    subtitle = "View 自绘，兼容更低系统版本",
                    isSelected = tempConfig.implementation == FloatingBarConfig.IMPL_NEW_VIEW,
                    onClick = {
                        tempConfig = tempConfig.copy(implementation = FloatingBarConfig.IMPL_NEW_VIEW)
                    }
                )
            }
        }

        PreferenceSection(title = "渲染模式") {
            SelectionGroup {
                SelectionItem(
                    title = "普通",
                    subtitle = "纯色药丸，性能开销最低",
                    isSelected = tempConfig.mode == FloatingBarConfig.MODE_NORMAL,
                    onClick = {
                        tempConfig = tempConfig.copy(mode = FloatingBarConfig.MODE_NORMAL)
                    }
                )
                SelectionItem(
                    title = "液态玻璃",
                    subtitle = "RuntimeShader 折射，需要 Android 13",
                    isSelected = tempConfig.mode == FloatingBarConfig.MODE_LIQUID_GLASS,
                    onClick = {
                        tempConfig = tempConfig.copy(mode = FloatingBarConfig.MODE_LIQUID_GLASS)
                    }
                )
            }
        }

        PreferenceSection(title = "悬浮底栏缩放") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionGroup {
                    listOf(80, 90, 100).forEach { percent ->
                        SelectionItem(
                            title = "$percent%",
                            subtitle = if (percent == 100) "默认大小" else "缩小显示",
                            isSelected = tempConfig.scalePercent == percent,
                            onClick = { tempConfig = tempConfig.copy(scalePercent = percent) }
                        )
                    }
                }
                SelectionGroup {
                    listOf(110, 120).forEach { percent ->
                        SelectionItem(
                            title = "$percent%",
                            subtitle = "放大显示",
                            isSelected = tempConfig.scalePercent == percent,
                            onClick = { tempConfig = tempConfig.copy(scalePercent = percent) }
                        )
                    }
                }
            }
        }

        PreferenceSection(title = "背景模糊") {
            SelectionGroup {
                listOf(0, 50, 100).forEach { percent ->
                    SelectionItem(
                        title = "$percent%",
                        subtitle = when (percent) {
                            0 -> "不模糊，纯色底"
                            50 -> "半模糊"
                            else -> "完全模糊"
                        },
                        isSelected = tempConfig.blurPercent == percent,
                        onClick = { tempConfig = tempConfig.copy(blurPercent = percent) }
                    )
                }
            }
        }

        PreferenceSection(title = "底栏位置") {
            SelectionGroup {
                SelectionItem(
                    title = "适中",
                    subtitle = "距屏幕底部留出更多间隙",
                    isSelected = tempConfig.position == FloatingBarConfig.POSITION_MODERATE,
                    onClick = {
                        tempConfig = tempConfig.copy(position = FloatingBarConfig.POSITION_MODERATE)
                    }
                )
                SelectionItem(
                    title = "靠底",
                    subtitle = "贴近屏幕底部",
                    isSelected = tempConfig.position == FloatingBarConfig.POSITION_BOTTOM,
                    onClick = {
                        tempConfig = tempConfig.copy(position = FloatingBarConfig.POSITION_BOTTOM)
                    }
                )
            }
        }

        PreferenceSection(title = "其他") {
            SwitchItem(
                title = "平滑切页",
                description = "切换标签页时走 ViewPager2 平滑动画路径",
                checked = tempConfig.smoothPageSwitch,
                onCheckedChange = { tempConfig = tempConfig.copy(smoothPageSwitch = it) }
            )
        }
    }
}
