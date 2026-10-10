package cn.hxy.kiora.ui.pages.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.R
import cn.hxy.kiora.ui.components.dialogs.CenterDialog
import cn.hxy.kiora.ui.components.listitems.SwitchActionCard
import cn.hxy.kiora.ui.components.molecules.AnimatedListItem
import cn.hxy.kiora.ui.components.molecules.SearchTopBar
import cn.hxy.kiora.ui.components.molecules.TopBarMenuItem
import cn.hxy.kiora.ui.core.theme.Dimens
import cn.hxy.kiora.ui.core.theme.KioraTheme
import cn.hxy.kiora.ui.pages.configs.ConfigUiRegistry
import cn.hxy.kiora.ui.pages.settings.components.CategoryCard
import cn.hxy.kiora.utils.ui.HighlightUtils

@Immutable
data class CategoryData(val name: String, val items: List<FunctionData>)

@Immutable
data class FunctionData(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val isAvailable: Boolean,
    val isClickable: Boolean,
    val configKey: String? = null,
    /**
     * 被同互斥分组里已开启的另一项占用，值为占用者的名字。
     *
     * 非空时卡片置灰且不可点 —— 二选一的功能里，只把开关拨回去不够，
     * 用户会以为是自己没点中。
     */
    val lockedBy: String? = null,
    /** 是否显示右侧开关；纯动作项（如批量操作、调试工具）为 false，只保留行点击。 */
    val showSwitch: Boolean = true
)

@Composable
fun SettingsScreen(
    categories: List<CategoryData>,
    searchResults: List<Pair<FunctionData, String>>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    versionInfo: String,
    themeMode: Int,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onImportConfig: () -> Unit,
    onExportConfig: () -> Unit,
    onFunctionToggle: (String, Boolean) -> Unit,
    onFunctionClick: (String) -> Unit,
    activeConfigKey: String?,
    onDismissDialog: () -> Unit,
    isNeedRestart: Boolean,
    onRestartStateChange: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KioraTheme.colors
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    val mainListState = rememberLazyListState()

    val handleBack = {
        when {
            activeConfigKey != null -> onDismissDialog()
            isSearchActive -> onSearchActiveChange(false)
            selectedCategoryName != null -> selectedCategoryName = null
            isNeedRestart -> onRestartStateChange()
            else -> onBackClick()
        }
    }

    BackHandler(onBack = handleBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchTopBar(
                title = selectedCategoryName ?: "Kiora",
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                isSearchActive = isSearchActive,
                onSearchActiveChange = onSearchActiveChange,
                showBackButton = true,
                onBackClick = handleBack,
                themeMode = themeMode,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                searchHint = "搜索功能名称或描述...",
                menuItems = listOf(
                    TopBarMenuItem(
                        "导入配置",
                        R.drawable.ic_file_import,
                        onImportConfig
                    ),
                    TopBarMenuItem(
                        "导出配置",
                        R.drawable.ic_file_export,
                        onExportConfig
                    )
                )
            )

            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ContentTransition"
            ) { searching ->
                if (searching) {
                    SearchModeContent(
                        searchResults = searchResults,
                        searchQuery = searchQuery,
                        onFunctionToggle = onFunctionToggle,
                        onFunctionClick = onFunctionClick
                    )
                } else {
                    if (selectedCategoryName == null) {
                        MainPage(
                            categories = categories,
                            versionInfo = versionInfo,
                            onCategoryClick = { selectedCategoryName = it.name },
                            listState = mainListState
                        )
                    } else {
                        categories.find { it.name == selectedCategoryName }?.let { category ->
                            DetailPage(
                                category = category,
                                onFunctionToggle = onFunctionToggle,
                                onFunctionClick = onFunctionClick
                            )
                        }
                    }
                }
            }
        }

        ConfigDialogOverlay(activeConfigKey, onDismissDialog)
    }
}

@Composable
private fun SearchModeContent(
    searchResults: List<Pair<FunctionData, String>>,
    searchQuery: String,
    onFunctionToggle: (String, Boolean) -> Unit,
    onFunctionClick: (String) -> Unit
) {
    val colors = KioraTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.PaddingMedium, Dimens.PaddingSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到相关功能",
                        color = colors.textSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        }

        items(searchResults, key = { it.first.id }) { (item, categoryName) ->
            val highlightedTitle = HighlightUtils.highlightText(
                text = item.name,
                query = searchQuery,
                highlightColor = colors.accentBlue,
                baseColor = colors.textPrimary
            )
            val highlightedDesc = HighlightUtils.highlightText(
                text = "[$categoryName] · ${item.description}",
                query = searchQuery,
                highlightColor = colors.accentBlue,
                baseColor = colors.textSecondary
            )

            SwitchActionCard(
                title = highlightedTitle,
                subtitle = highlightedDesc,
                isChecked = item.isEnabled,
                onCheckedChange = { onFunctionToggle(item.id, it) },
                isAvailable = item.isAvailable && item.lockedBy == null,
                disabledHint = item.lockedBy?.let { "已由「$it」接管" },
                onClick = if (item.isClickable) { { onFunctionClick(item.id) } } else null,
                showSwitch = item.showSwitch
            )
        }
    }
}

@Composable
private fun MainPage(
    categories: List<CategoryData>,
    versionInfo: String,
    onCategoryClick: (CategoryData) -> Unit,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }
        items(categories, key = { it.name }) { category ->
            AnimatedListItem(categories.indexOf(category)) {
                CategoryCard(
                    name = category.name,
                    totalCount = category.items.size,
                    enabledCount = category.items.count { it.isEnabled },
                    onClick = { onCategoryClick(category) },
                    modifier = Modifier.padding(Dimens.PaddingMedium, 6.dp)
                )
            }
        }
        item {
            AboutSection(versionInfo = versionInfo)
        }
    }
}

@Composable
private fun DetailPage(
    category: CategoryData,
    onFunctionToggle: (String, Boolean) -> Unit,
    onFunctionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.PaddingMedium, Dimens.PaddingSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(category.items, key = { it.id }) { item ->
            AnimatedListItem(category.items.indexOf(item)) {
                SwitchActionCard(
                    title = item.name,
                    subtitle = item.description,
                    isChecked = item.isEnabled,
                    onCheckedChange = { onFunctionToggle(item.id, it) },
                    isAvailable = item.isAvailable && item.lockedBy == null,
                    disabledHint = item.lockedBy?.let { "已由「$it」接管" },
                    onClick = if (item.isClickable) { { onFunctionClick(item.id) } } else null,
                    showSwitch = item.showSwitch
                )
            }
        }
    }
}

@Composable
private fun AboutSection(versionInfo: String) {
    val colors = KioraTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = versionInfo,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun ConfigDialogOverlay(activeConfigKey: String?, onDismiss: () -> Unit) {
    val configUi = activeConfigKey?.let { ConfigUiRegistry.getConfigUi(it) }
    CenterDialog(visible = activeConfigKey != null && configUi != null, onDismiss = onDismiss) {
        configUi?.invoke(onDismiss)
    }
}
