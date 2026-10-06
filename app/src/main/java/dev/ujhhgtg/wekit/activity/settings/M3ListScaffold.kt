package dev.ujhhgtg.wekit.activity.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.ujhhgtg.wekit.ui.content.m3.ExpressiveCollapsingTopAppBar
import dev.ujhhgtg.wekit.ui.content.m3AppBarBlur
import dev.ujhhgtg.wekit.ui.content.m3AppBarColor
import dev.ujhhgtg.wekit.ui.content.rememberMaterial3BlurBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 上游 `activity\settings\SettingsActivity.kt:341` 的共享列表脚手架。
 *
 * 上游把 M3 设置外壳（依赖 KSP 生成的 `FeaturesProvider`）留在同一个文件里，
 * 而 Kiora 用 `WxFeatureRegistry` 手写注册表、设置页由
 * `cn.hxy.kiora.activity.SettingActivity` 提供，因此那份外壳整体未移植。
 * 但**功能自带的配置页**（如 `features\items\chat\ReadReceiptsSettingsActivity.kt`）
 * 会 `import dev.ujhhgtg.wekit.activity.settings.M3ListScaffold`，故这里单独抽出该函数。
 *
 * 与上游逐字一致（上游 :340-374），只补 import。
 */
@Composable
fun M3ListScaffold(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val barBackdrop = rememberMaterial3BlurBackdrop()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ExpressiveCollapsingTopAppBar(
                modifier = Modifier.m3AppBarBlur(barBackdrop),
                title = title,
                navigationIcon = { navigationIcon?.invoke() },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barBackdrop.m3AppBarColor(),
                    scrolledContainerColor = barBackdrop.m3AppBarColor(),
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(barBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = innerPadding,
            content = content,
        )
    }
}
