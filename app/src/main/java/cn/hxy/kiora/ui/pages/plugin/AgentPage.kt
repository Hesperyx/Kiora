package cn.hxy.kiora.ui.pages.plugin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.BuildConfig
import cn.hxy.kiora.R
import cn.hxy.kiora.plugin.bean.AgentChatMessage
import cn.hxy.kiora.plugin.bean.AgentMode
import cn.hxy.kiora.plugin.bean.AgentScriptFiles
import cn.hxy.kiora.plugin.bean.AgentScriptInsight
import cn.hxy.kiora.plugin.bean.AgentSession
import cn.hxy.kiora.plugin.bean.AgentStage
import cn.hxy.kiora.plugin.bean.AgentToolStep
import cn.hxy.kiora.plugin.agent.ScriptDiff
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import cn.hxy.kiora.plugin.net.AgentConfig
import cn.hxy.kiora.ui.components.atoms.DialogButton
import cn.hxy.kiora.ui.components.atoms.KioraSwitch
import cn.hxy.kiora.ui.components.atoms.DialogTextField
import cn.hxy.kiora.ui.components.dialogs.BaseDialogSurface
import cn.hxy.kiora.ui.components.dialogs.CenterDialog
import cn.hxy.kiora.ui.core.theme.AccentBlue
import cn.hxy.kiora.ui.core.theme.AccentGreen
import cn.hxy.kiora.ui.core.theme.AccentRed
import cn.hxy.kiora.ui.core.theme.KioraTheme
import cn.hxy.kiora.utils.qq.HostInfo
import cn.hxy.kiora.utils.qq.Toasts
import cn.hxy.kiora.utils.ui.CodeHighlighter

/** 输入区与悬浮 Tab 之间留出的呼吸空间。 */
private val INPUT_TO_TABS_GAP = 14.dp

/** 把毫秒压成一眼能读的时长。 */
private fun formatElapsed(ms: Long): String =
    if (ms < 1000L) "${ms}ms" else "%.1fs".format(ms / 1000.0)

// ==================== 图标映射 ====================
//
// 放在 UI 这一侧：AgentStage / AgentMode 是纯数据，不该把 Android 资源 id 塞进去，
// 否则 bean 层就得依赖 R，序列化和复用都会跟着变脏。

private val AgentStage.iconRes: Int
    get() = when (this) {
        AgentStage.CONNECTING -> R.drawable.ic_agent_link
        AgentStage.THINKING -> R.drawable.ic_agent_think
        AgentStage.WRITING -> R.drawable.ic_agent_code
        AgentStage.PARSING -> R.drawable.ic_agent_parse
        AgentStage.DONE -> R.drawable.ic_agent_done
        AgentStage.FAILED -> R.drawable.ic_agent_error
        AgentStage.TOOL -> R.drawable.ic_agent_tool
    }

private val AgentMode.iconRes: Int
    get() = when (this) {
        AgentMode.QA -> R.drawable.ic_agent_qa
        AgentMode.TASK -> R.drawable.ic_agent_task
    }

/** 产物文件按扩展名给图标。 */
private fun iconResOfScriptFile(path: String): Int = when {
    path.endsWith(".java", ignoreCase = true) -> R.drawable.ic_agent_code
    path.endsWith(".json", ignoreCase = true) -> R.drawable.ic_agent_parse
    else -> R.drawable.ic_agent_file
}

@Composable
fun AgentPage(
    messages: List<AgentChatMessage>,
    files: AgentScriptFiles,
    sessions: List<AgentSession>,
    isGenerating: Boolean,
    config: AgentConfig?,
    mode: AgentMode,
    showSettings: Boolean,
    showSaveDialog: Boolean,
    showHistoryPanel: Boolean,
    modelList: List<String>,
    isFetchingModels: Boolean,
    isTestingConnection: Boolean,
    connectionResult: String?,
    modelFetchError: String?,
    insight: AgentScriptInsight,
    stage: AgentStage,
    elapsedMs: Long,
    overwriteTargetName: String?,
    overwriteDiff: ScriptDiff?,
    onSendMessage: (String) -> Unit,
    onSelectMode: (AgentMode) -> Unit,
    onEditFile: (String, String) -> Unit,
    onSaveScript: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onUpdateConfig: (AgentConfig) -> Unit,
    onFetchModels: (String, String) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onDismissSaveDialog: () -> Unit,
    onConfirmSave: (String, String, String, Boolean) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismissOverwrite: () -> Unit,
    onCancelGenerate: () -> Unit,
    onToggleHistory: () -> Unit,
    onNewSession: () -> Unit,
    onSwitchSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    /** 底部悬浮 Tab 的实测占位高度：输入区据此上移，避免被 Tab 压住。 */
    reservedBottom: Dp,
    quickPrompts: List<String>,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val colors = KioraTheme.colors

    // 代码抽屉：null = 收起；非空 = 展开并显示该文件。
    //
    // 对话是唯一主线，代码按需从底部升起、看完划走。之前用「整页切换」表达
    // 在看代码，用户得在两个心智模型间反复横跳；而且产物其实已经在对话里
    // 通过工具卡片表达过了，代码页那时只剩一个只读查看器的角色。
    var codeSheetFile by remember { mutableStateOf<String?>(null) }

    // 产物被清空时收起抽屉，避免停在一个已经不存在的文件上。
    LaunchedEffect(files.isEmpty) {
        if (files.isEmpty) codeSheetFile = null
    }

    // 生成中不占着抽屉，让用户看流式输出。
    LaunchedEffect(isGenerating) {
        if (isGenerating) codeSheetFile = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        if (config == null || config.apiUrl.isBlank() || config.apiKey.isBlank()) {
            ConfigPromptView(onOpenSettings = onOpenSettings)
        } else {
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    WelcomeView(
                        mode = mode,
                        quickPrompts = quickPrompts,
                        onSendMessage = onSendMessage
                    )
                } else {
                    AgentChatView(
                        messages = messages,
                        canSave = mode == AgentMode.TASK && !files.isEmpty,
                        onSaveScript = onSaveScript,
                        onViewCode = { codeSheetFile = AgentScriptFiles.FILE_MAIN },
                        onContinue = {
                            onSendMessage("上次输出被 max_tokens 截断了，请从中断处继续，把剩余部分补齐。")
                        }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isGenerating,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.cardBackground)
                            .border(1.dp, AccentBlue.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // 只留阶段和耗时。字数读数对用户没有意义，
                        // 阶段推进在工具卡片里已经表达过了。
                        Icon(
                            painter = painterResource(stage.iconRes),
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stage.labelIn(mode)} · ${formatElapsed(elapsedMs)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = codeSheetFile != null && !files.isEmpty,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AgentCodeSheet(
                        files = files,
                        activeFile = codeSheetFile ?: AgentScriptFiles.FILE_MAIN,
                        insight = insight,
                        isDark = colors.isDark,
                        onSelectFile = { codeSheetFile = it },
                        onEditFile = onEditFile,
                        onClose = { codeSheetFile = null }
                    )
                }
            }

            AgentInputBar(
                mode = mode,
                isGenerating = isGenerating,
                onToggleMode = {
                    val next = if (mode == AgentMode.QA) AgentMode.TASK else AgentMode.QA
                    onSelectMode(next)
                    // 切模式是个状态切换，给一句明确反馈，别让人猜点没点中
                    Toasts.qqToast(0, "已切换到「${next.label}」模式")
                },
                onSend = { text ->
                    onSendMessage(text)
                    keyboardController?.hide()
                },
                onCancel = onCancelGenerate
            )

            // 悬浮 Tab 固定贴在 Box 底部，输入区必须始终留在它上方。
            // 这里不再按键盘可见性归零：宿主是寄生 Activity，IME inset 会误报，
            // 一旦误判成「键盘可见」，让位归零，输入框就会滑到 Tab 底下。
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(reservedBottom + INPUT_TO_TABS_GAP)
            )
        }
    }

    AgentSettingsDialog(
        visible = showSettings,
        initial = config,
        modelList = modelList,
        isFetchingModels = isFetchingModels,
        isTestingConnection = isTestingConnection,
        connectionResult = connectionResult,
        modelFetchError = modelFetchError,
        onDismiss = onDismissSettings,
        onConfirm = onUpdateConfig,
        onFetchModels = onFetchModels,
        onTestConnection = onTestConnection
    )

    AgentSaveDialog(
        visible = showSaveDialog,
        files = files,
        insight = insight,
        onDismiss = onDismissSaveDialog,
        onConfirm = onConfirmSave
    )

    AgentOverwriteDialog(
        targetName = overwriteTargetName,
        diff = overwriteDiff,
        files = files,
        insight = insight,
        onConfirm = onConfirmOverwrite,
        onDismiss = onDismissOverwrite
    )

    AgentHistoryDialog(
        visible = showHistoryPanel,
        sessions = sessions,
        onDismiss = onToggleHistory,
        onNewSession = onNewSession,
        onSwitchSession = onSwitchSession,
        onDeleteSession = onDeleteSession
    )
}




// ==================== 代码抽屉 ====================

/**
 * 抽屉占内容区的高度比例。刻意不满屏 —— 上方留出一截，暗示「这是浮层，不是切页」。
 */
private const val CODE_SHEET_HEIGHT = 0.74f

/**
 * 代码抽屉。
 *
 * 从底部升起、看完划走。对话始终是主线，代码是临时查看的对象 —— 之前用整页
 * 切换表达「正在看代码」，用户得在两个心智模型之间反复横跳；而产物其实已经在
 * 对话里通过工具卡片表达过了，那个代码页只剩一个只读查看器的角色。
 */
@Composable
private fun AgentCodeSheet(
    files: AgentScriptFiles,
    activeFile: String,
    insight: AgentScriptInsight,
    isDark: Boolean,
    onSelectFile: (String) -> Unit,
    onEditFile: (String, String) -> Unit,
    onClose: () -> Unit
) {
    val colors = KioraTheme.colors
    val context = LocalContext.current
    val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    val original = files.contentOf(activeFile)
    var editing by remember(activeFile) { mutableStateOf(false) }
    var draft by remember(activeFile) { mutableStateOf(original) }

    // 外部内容变了（重新生成、切会话）就把草稿同步过来，避免编辑框里留着旧文本
    LaunchedEffect(activeFile, original) {
        if (!editing) draft = original
    }

    val editScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(CODE_SHEET_HEIGHT)
            .clip(sheetShape)
            .background(colors.cardBackground)
            .border(1.dp, colors.textSecondary.copy(alpha = 0.12f), sheetShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.textSecondary.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (editing) "编辑 $activeFile" else activeFile,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (editing) AccentRed else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (editing) {
                SheetTextAction("取消") {
                    draft = original
                    editing = false
                }
                SheetTextAction("保存", highlight = true) {
                    onEditFile(activeFile, draft)
                    editing = false
                }
            } else {
                SheetIconAction(R.drawable.ic_agent_edit) { draft = original; editing = true }
                SheetIconAction(R.drawable.ic_agent_copy) { copyToClipboard(context, original) }
                SheetIconAction(R.drawable.ic_close) { onClose() }
            }
        }

        // 文件标签用相对路径：分层脚本会带 core 目录下的同名文件
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            files.fileNames.forEach { path ->
                SheetFileTab(
                    iconRes = iconResOfScriptFile(path),
                    label = path,
                    active = path == activeFile,
                    onClick = {
                        if (editing) {
                            // 带着未保存的改动切文件会丢内容，先落定
                            onEditFile(activeFile, draft)
                            editing = false
                        }
                        onSelectFile(path)
                    }
                )
            }
        }

        // 能力摘要搬到这里而不是留在对话流：它是对产物的注解，看代码时正好用得上，
        // 放在气泡里则是和工具卡片重复一遍。
        if (!insight.isEmpty && !editing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_agent_think),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = insight.summary,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.1f))

        Box(modifier = Modifier.weight(1f)) {
            if (editing) {
                // 编辑时用等宽纯文本、不做语法高亮 —— 高亮是逐行构造 AnnotatedString，
                // 每敲一个字符全量重算会明显卡顿；等保存回查看态再着色。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(editScroll)
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        textStyle = TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        ),
                        cursorBrush = SolidColor(AccentBlue)
                    )
                }
            } else {
                AgentCodeViewer(code = original, isDark = isDark)
            }
        }
    }
}

/** 抽屉头部的图标动作。 */
@Composable
private fun SheetIconAction(
    iconRes: Int,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val colors = KioraTheme.colors

    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = tint ?: colors.textPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .size(18.dp)
    )
}

/** 抽屉头部的文字动作：编辑态下的「保存 / 取消」用。 */
@Composable
private fun SheetTextAction(
    text: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val colors = KioraTheme.colors

    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = if (highlight) AccentBlue else colors.textPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun SheetFileTab(iconRes: Int, label: String, active: Boolean, onClick: () -> Unit) {
    val colors = KioraTheme.colors

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) AccentBlue.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (active) AccentBlue else colors.textSecondary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) AccentBlue else colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun ToolbarChip(
    text: String,
    onClick: () -> Unit,
    iconRes: Int? = null,
    enabled: Boolean = true,
    highlight: Boolean = false,
    danger: Boolean = false
) {
    val colors = KioraTheme.colors

    val tint = when {
        !enabled -> colors.textSecondary.copy(alpha = 0.5f)
        highlight -> AccentGreen
        danger -> AccentRed
        else -> colors.textPrimary
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.background)
            .border(1.dp, colors.textSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}

// ==================== 代码查看器 ====================

@Composable
private fun AgentCodeViewer(code: String, isDark: Boolean) {
    val colors = KioraTheme.colors

    val palette = remember(isDark) { CodeHighlighter.palette(isDark) }
    val lines = remember(code) { code.split('\n') }
    val listState = rememberLazyListState()

    if (code.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("此文件为空", fontSize = 13.sp, color = colors.textSecondary)
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = (index + 1).toString(),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = palette.lineNumber,
                    modifier = Modifier
                        .width(44.dp)
                        .padding(end = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Text(
                    text = CodeHighlighter.highlightLine(line, palette),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
            }
        }
    }
}

// ==================== 对话流 ====================

@Composable
private fun AgentChatView(
    messages: List<AgentChatMessage>,
    canSave: Boolean,
    onSaveScript: () -> Unit,
    onViewCode: () -> Unit,
    onContinue: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length, messages.lastOrNull()?.reasoningContent?.length) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1, scrollOffset = Int.MAX_VALUE)
        }
    }

    // 产物是否存在由会话级状态判断（canSave）—— 消息上的 files 不落盘，
    // 切回历史会话时它是空的，拿它判断会漏掉结果卡。
    val settled = messages.lastOrNull()
        ?.let { it.role == AgentChatMessage.ROLE_ASSISTANT && !it.isStreaming } == true
    val showResult = settled && canSave
    // 只对最后一条提示续写：早先那几轮的截断多半已经被补过了
    val showContinue = settled && messages.lastOrNull()?.truncated == true

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // key 必须与内容无关：以前用 hashCode()，流式每来一个 token key 就变，
        // LazyColumn 每帧重建气泡，思考区反复重排 —— 就是那个上下抖动。
        itemsIndexed(
            items = messages,
            key = { index, msg -> msg.id.ifBlank { "legacy_$index" } }
        ) { _, msg ->
            AgentBubble(message = msg)
        }

        // 结果卡挂在对话末尾：保存入口跟着产物走，不再占一行常驻工具栏。
        if (canSave && showResult) {
            item(key = "result_actions") {
                AgentResultActions(onSaveScript = onSaveScript, onViewCode = onViewCode)
            }
        }

        // 上一轮被截断：给一个明确的续写入口，而不是让用户自己打「继续」
        if (showContinue) {
            item(key = "continue_action") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolbarChip(
                        text = "继续输出",
                        onClick = onContinue,
                        iconRes = R.drawable.ic_agent_continue,
                        highlight = true
                    )
                }
            }
        }
    }
}

/** 产物就绪后的下一步动作。 */
@Composable
private fun AgentResultActions(onSaveScript: () -> Unit, onViewCode: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarChip(
            text = "保存为脚本",
            onClick = onSaveScript,
            iconRes = R.drawable.ic_agent_save,
            highlight = true
        )
        ToolbarChip(
            text = "查看代码",
            onClick = onViewCode,
            iconRes = R.drawable.ic_agent_code
        )
    }
}

@Composable
private fun AgentBubble(message: AgentChatMessage) {
    val colors = KioraTheme.colors


    if (message.role == AgentChatMessage.ROLE_SYSTEM) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = message.content,
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        return
    }

    val isUser = message.role == AgentChatMessage.ROLE_USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                painter = painterResource(R.drawable.ic_agent_bot),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp).size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // 工具卡片放在气泡之外：它描述的是「本地做了什么」，不是模型说的话。
            if (!isUser && message.steps.isNotEmpty()) {
                AgentStepsCard(steps = message.steps, elapsedMs = message.elapsedMs)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) AccentBlue else colors.cardBackground)
                    .padding(12.dp)
            ) {
                if (!isUser && !message.reasoningContent.isNullOrBlank()) {
                    ReasoningSection(
                        reasoning = message.reasoningContent,
                        isStreaming = message.isStreaming
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = if (isUser) Color.White else colors.textPrimary,
                        lineHeight = 20.sp
                    )
                }
                if (message.isStreaming) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("▌", fontSize = 14.sp, color = AccentGreen)
                }
            }

            // 闲聊轮次解析结果为空，这里不冒「已生成」的假消息
            if (!isUser && message.files != null && !message.files.isEmpty && !message.isStreaming) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_agent_file),
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "已生成 ${message.files.pluginName}（${message.files.pluginId}）",
                        fontSize = 12.sp,
                        color = AccentGreen
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_agent_user),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp).size(20.dp)
            )
        }
    }
}

/**
 * 思考过程。
 *
 * 流式期间默认展开并跟随最新内容 —— 模型在思考时用户不该只看见转圈；
 * 收流后自动收起，不长期占用版面。
 */
@Composable
private fun ReasoningSection(reasoning: String, isStreaming: Boolean = false) {
    val colors = KioraTheme.colors

    // 这里不能把 isStreaming 当 remember 的 key：收流那一刻状态会被重置，
    // 思考区突然收起，整条消息跟着弹一下。改成只由流式状态单向驱动展开。
    var expanded by remember { mutableStateOf(false) }
    val reasoningScroll = rememberScrollState()

    LaunchedEffect(isStreaming) {
        if (isStreaming) expanded = true
    }

    LaunchedEffect(reasoning.length, expanded) {
        if (expanded) {
            // 等一帧让布局量到新高度，否则 maxValue 还是上一帧的
            withFrameNanos { }
            reasoningScroll.scrollTo(reasoningScroll.maxValue)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_agent_think),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "思考过程 ${if (expanded) "▾" else "▸"}",
                fontSize = 12.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        if (expanded) {
            Text(
                text = reasoning,
                fontSize = 12.sp,
                color = colors.textSecondary.copy(alpha = 0.85f),
                lineHeight = 17.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(reasoningScroll)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background.copy(alpha = 0.6f))
                    .padding(8.dp)
            )
        }
    }
}

// ==================== 工具卡片 ====================

/**
 * 生成流水线的可视化。
 *
 * 模型只吐文本，「连接 → 思考 → 落码 → 解析」都是本地做的；
 * 陈列出来才知道卡在哪一段，而不是对着一个转圈瞎猜。
 */
@Composable
private fun AgentStepsCard(
    steps: List<AgentToolStep>,
    elapsedMs: Long = 0L
) {
    val colors = KioraTheme.colors

    val doneCount = steps.count { it.done }
    val accent = if (doneCount == steps.size) AccentGreen else AccentBlue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_agent_tool),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "执行过程",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = buildString {
                    append(doneCount).append('/').append(steps.size)
                    if (elapsedMs > 0L) append(" · ").append(formatElapsed(elapsedMs))
                },
                fontSize = 11.sp,
                color = accent
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        steps.forEach { step ->
            val stageEnum = step.stageEnum
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        if (step.done) R.drawable.ic_agent_done else R.drawable.ic_agent_pending
                    ),
                    contentDescription = null,
                    tint = if (step.done) AccentGreen else colors.textSecondary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(stageEnum.iconRes),
                    contentDescription = null,
                    tint = if (step.done) colors.textPrimary else colors.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = step.displayTitle,
                    fontSize = 12.sp,
                    color = if (step.done) colors.textPrimary else colors.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = step.detail,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (step.costMs > 0L) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatElapsed(step.costMs),
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

// ==================== 欢迎页 ====================

@Composable
private fun WelcomeView(
    mode: AgentMode,
    quickPrompts: List<String>,
    onSendMessage: (String) -> Unit
) {
    val colors = KioraTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // 头部压成一行：页面标题已经在顶栏，这里不再重复
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(mode.iconRes),
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (mode) {
                        AgentMode.QA -> "提问 API 用法与参数，回答会带上可直接运行的示例"
                        AgentMode.TASK -> "描述需求，生成 main.java + info.prop + desc.txt"
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 「选择已有脚本」这段删掉了：本地脚本卡片上已经有了「修改」入口，
        // 这里不但重复，还只显示前 6 个 —— 脚本一多就找不到。
        item { SectionDivider(label = if (mode == AgentMode.QA) "常见问题" else "快速开始") }

        items(quickPrompts.chunked(2), key = { it.first() }) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { prompt ->
                    QuickPromptChip(
                        text = prompt,
                        onClick = { onSendMessage(prompt) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 分区标题 + 一条细分隔线。 */
@Composable
private fun SectionDivider(label: String) {
    val colors = KioraTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = colors.textSecondary.copy(alpha = 0.12f),
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

/** 轻量提示标签：细边框胶囊，明确表达「可点击的示例需求」。 */
@Composable
private fun QuickPromptChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = AccentBlue,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== 输入区 ====================

@Composable
private fun AgentInputBar(
    mode: AgentMode,
    isGenerating: Boolean,
    onToggleMode: () -> Unit,
    onSend: (String) -> Unit,
    onCancel: () -> Unit
) {
    val colors = KioraTheme.colors

    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.cardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            // 模式切换从整整一行挪到这里：它是一开始选一次的东西，
            // 不该长期占着页面最显眼的位置。图标和输入提示都跟着模式变，
            // 所以「现在在哪个模式」一眼就能看出来。
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.background)
                    .clickable { onToggleMode() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(mode.iconRes),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.background)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary, lineHeight = 19.sp),
                cursorBrush = SolidColor(AccentBlue),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = when (mode) {
                                    AgentMode.QA -> "询问 API 用法，例如：怎么注册悬浮窗菜单？"
                                    AgentMode.TASK -> "请描述你的插件需求"
                                },
                                fontSize = 14.sp,
                                color = colors.textSecondary
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (text.isNotBlank()) AccentBlue else colors.textSecondary.copy(alpha = 0.3f))
                        .clickable(enabled = text.isNotBlank()) {
                            onSend(text)
                            text = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_up),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// ==================== 未配置引导 ====================

@Composable
private fun ConfigPromptView(onOpenSettings: () -> Unit) {
    val colors = KioraTheme.colors

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_gear),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "需要配置 API",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请先配置 OpenAI 兼容的 API 地址和密钥",
                fontSize = 14.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "点击前往设置",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AccentBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

// ==================== 会话历史 ====================

@Composable
private fun AgentHistoryDialog(
    visible: Boolean,
    sessions: List<AgentSession>,
    onDismiss: () -> Unit,
    onNewSession: () -> Unit,
    onSwitchSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    if (!visible) return

    val colors = KioraTheme.colors

    var query by remember(visible) { mutableStateOf("") }
    val filtered = if (query.isBlank()) {
        sessions
    } else {
        sessions.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.prompt.contains(query, ignoreCase = true)
        }
    }

    CenterDialog(visible = visible, onDismiss = onDismiss) {
        BaseDialogSurface(
            title = "历史记录",
            bottomBar = {
                DialogButton("新建会话", onNewSession, true)
                Spacer(modifier = Modifier.width(12.dp))
                DialogButton("关闭", onDismiss, false)
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 会话攒到几十条之后，翻列表比搜索慢得多
                DialogTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "搜索",
                    hint = "按需求内容或标题查找",
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Text(
                        text = if (sessions.isEmpty()) "还没有历史记录" else "没有匹配的会话",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        items(filtered, key = { it.id }) { session ->
                            AgentHistoryRow(
                                session = session,
                                onSwitch = onSwitchSession,
                                onDelete = onDeleteSession
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 历史会话的一行。
 *
 * 时间、模式、产物名都要露出来 —— 会话记录里这些字段本来就存着，
 * 只显示「N 条 · 脚本名」等于把能帮用户辨认的信息全藏起来了。
 */
@Composable
private fun AgentHistoryRow(
    session: AgentSession,
    onSwitch: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val colors = KioraTheme.colors
    val sessionMode = AgentMode.of(session.mode)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSwitch(session.id) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title.ifBlank { "未命名会话" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(sessionMode.iconRes),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = buildString {
                        append(sessionMode.label)
                        append(" · ").append(formatRelativeTime(session.timestamp))
                        append(" · ").append(session.messages.size).append(" 条")
                        session.files.pluginName.takeIf { it.isNotBlank() }?.let {
                            append(" · ").append(it)
                        }
                    },
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "删除",
            fontSize = 12.sp,
            color = AccentRed,
            modifier = Modifier
                .clickable { onDelete(session.id) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** 会话列表用相对时间：几十条记录里，一串绝对时间戳反而难扫。 */
private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L} 小时前"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L} 天前"
        else -> HISTORY_DATE_FORMAT.format(Date(timestamp))
    }
}

private val HISTORY_DATE_FORMAT = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

// ==================== API 设置 ====================

@Composable
private fun AgentSettingsDialog(
    visible: Boolean,
    initial: AgentConfig?,
    modelList: List<String>,
    isFetchingModels: Boolean,
    isTestingConnection: Boolean,
    connectionResult: String?,
    modelFetchError: String?,
    onDismiss: () -> Unit,
    onConfirm: (AgentConfig) -> Unit,
    onFetchModels: (String, String) -> Unit,
    onTestConnection: (String, String) -> Unit
) {
    if (!visible) return

    val colors = KioraTheme.colors


    var apiUrl by remember(visible) { mutableStateOf(initial?.apiUrl ?: "") }
    var apiKey by remember(visible) { mutableStateOf(initial?.apiKey ?: "") }
    var model by remember(visible) { mutableStateOf(initial?.model ?: "gpt-4o-mini") }
    var temperature by remember(visible) {
        mutableStateOf((initial?.temperature ?: AgentConfig.DEFAULT_TEMPERATURE).toString())
    }
    var maxTokens by remember(visible) {
        mutableStateOf((initial?.maxTokens ?: AgentConfig.DEFAULT_MAX_TOKENS).toString())
    }

    CenterDialog(visible = visible, onDismiss = onDismiss) {
        BaseDialogSurface(
            title = "Agent API 配置",
            bottomBar = {
                DialogButton("取消", onDismiss, false)
                Spacer(modifier = Modifier.width(12.dp))
                DialogButton(
                    "保存",
                    {
                        onConfirm(
                            AgentConfig(
                                apiUrl = apiUrl.trim(),
                                apiKey = apiKey.trim(),
                                model = model.trim().ifBlank { "gpt-4o-mini" },
                                // 越界或写错就回落到默认值，不让一个手滑把请求参数带偏
                                temperature = temperature.toDoubleOrNull()
                                    ?.coerceIn(AgentConfig.MIN_TEMPERATURE, AgentConfig.MAX_TEMPERATURE)
                                    ?: AgentConfig.DEFAULT_TEMPERATURE,
                                maxTokens = maxTokens.toIntOrNull()
                                    ?.coerceIn(AgentConfig.MIN_MAX_TOKENS, AgentConfig.MAX_MAX_TOKENS)
                                    ?: AgentConfig.DEFAULT_MAX_TOKENS,
                            )
                        )
                    },
                    true
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DialogTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = "API 地址",
                    hint = "https://api.openai.com/v1/chat/completions",
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key",
                    hint = "sk-...",
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DialogButton("连通性检测", { onTestConnection(apiUrl, apiKey) }, true)
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AccentBlue
                        )
                    }
                }
                connectionResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        fontSize = 13.sp,
                        color = if (result.startsWith("连接成功")) AccentGreen else AccentRed,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                DialogTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "模型",
                    hint = "gpt-4o-mini",
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DialogButton("获取模型", { onFetchModels(apiUrl, apiKey) }, false)
                    if (isFetchingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AccentBlue
                        )
                    }
                }

                modelFetchError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "获取失败：$err", fontSize = 13.sp, color = AccentRed, lineHeight = 18.sp)
                }

                if (modelList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "点击选择模型 (${modelList.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                    ) {
                        items(modelList, key = { it }) { m ->
                            val selected = m == model
                            Text(
                                text = m,
                                fontSize = 13.sp,
                                color = if (selected) AccentBlue else colors.textPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { model = m }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                // 高级参数：默认值对代码生成够用，一般不用动
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "高级参数",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                DialogTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = "温度（${AgentConfig.MIN_TEMPERATURE} ~ ${AgentConfig.MAX_TEMPERATURE}，越低越稳）",
                    hint = AgentConfig.DEFAULT_TEMPERATURE.toString(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                DialogTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it },
                    label = "最大输出 tokens（${AgentConfig.MIN_MAX_TOKENS} ~ ${AgentConfig.MAX_MAX_TOKENS}）",
                    hint = AgentConfig.DEFAULT_MAX_TOKENS.toString(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "脚本较长时调大「最大输出」，否则容易写到一半被截断。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = colors.textSecondary
                )

                // 运行环境信息收纳在这里，不再占用主界面底部
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Kiora ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) · BeanShell · Java 8",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "宿主 ${HostInfo.hostName} ${HostInfo.versionName}(${HostInfo.versionCode})",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun AgentSaveDialog(
    visible: Boolean,
    files: AgentScriptFiles,
    insight: AgentScriptInsight,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    if (!visible) return

    val colors = KioraTheme.colors

    var name by remember(visible) { mutableStateOf("") }

    // 版本号与作者预填脚本现有的值：编辑既有脚本时不该被悄悄改回 1.0 / Kiora Agent，
    // 否则下一个版本号没地方往上加，"高版本覆盖低版本"也就无从谈起。
    var version by remember(visible) { mutableStateOf(files.version) }
    var author by remember(visible) { mutableStateOf(files.author) }

    // 默认勾上：保存完还要用户自己切页、找到脚本、手动开启，这一步没有存在的理由
    var runAfter by remember(visible) { mutableStateOf(true) }

    val finalName = name.trim().ifBlank { files.pluginName.ifBlank { "Agent脚本" } }

    CenterDialog(visible = visible, onDismiss = onDismiss) {
        BaseDialogSurface(
            title = "确认写入脚本",
            bottomBar = {
                DialogButton("取消", onDismiss, false)
                Spacer(modifier = Modifier.width(12.dp))
                DialogButton("确认写入", { onConfirm(name, version, author, runAfter) }, true)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "脚本名称",
                    hint = files.pluginName.ifBlank { "例如：自动回复" },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                DialogTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = "版本号",
                    hint = AgentScriptFiles.DEFAULT_VERSION,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                DialogTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = "作者",
                    hint = AgentScriptFiles.DEFAULT_AUTHOR,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "同名脚本已存在时，版本号高过它才会覆盖。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "将写入的文件",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FileManifest(files = files, insight = insight)

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "目录  plugin/$finalName/",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "保存后立即运行",
                            fontSize = 13.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "跳过「切到本地脚本页再手动开启」这一步",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                    KioraSwitch(runAfter, { runAfter = it })
                }
            }
        }
    }
}

/**
 * 人机确认：编辑模式下覆盖既有脚本前的最后一道闸。
 *
 * 之前模型一产出就自动 writeText 覆盖，用户没有反悔的机会；
 * 现在产物先停在 ViewModel 里，只有点「覆盖写入」才落盘。
 */
@Composable
private fun AgentOverwriteDialog(
    targetName: String?,
    files: AgentScriptFiles,
    insight: AgentScriptInsight,
    diff: ScriptDiff?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (targetName == null) return

    val colors = KioraTheme.colors

    CenterDialog(visible = true, onDismiss = onDismiss) {
        BaseDialogSurface(
            title = "覆盖既有脚本？",
            bottomBar = {
                DialogButton("取消", onDismiss, false)
                Spacer(modifier = Modifier.width(12.dp))
                DialogButton("覆盖写入", onConfirm, true)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "「$targetName」的 main.java / info.prop / desc.txt 将被本次生成结果替换。",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "原内容不可撤销。想保留原版，请先取消，再用「保存为脚本」另存新脚本。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = AccentRed
                )
                Spacer(modifier = Modifier.height(12.dp))
                AgentDiffSummary(diff)

                Spacer(modifier = Modifier.height(12.dp))
                FileManifest(files = files, insight = insight)
            }
        }
    }
}

/**
 * 改动摘要。
 *
 * 覆盖既有脚本前唯一能让人判断「这次改对了没有」的依据 —— 只报「将被替换」
 * 等于让人闭着眼睛按确认。没有变化时整块不出现。
 */
@Composable
private fun AgentDiffSummary(diff: ScriptDiff?) {
    if (diff == null || !diff.hasChanges) return

    val colors = KioraTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "本次改动",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            diff.files.forEach { change ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = change.path,
                        fontSize = 11.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (change.isNew) {
                        Text("新增", fontSize = 11.sp, color = AccentGreen)
                    } else {
                        Text("+${change.addedLines}", fontSize = 11.sp, color = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("-${change.removedLines}", fontSize = 11.sp, color = AccentRed)
                    }
                }
            }

            if (diff.addedMethods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "新增方法  " + diff.addedMethods.joinToString("、"),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = AccentGreen
                )
            }
            if (diff.removedMethods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "删除方法  " + diff.removedMethods.joinToString("、"),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = AccentRed
                )
            }
        }
    }
}

/** 三件套的落盘清单，保存卡与覆盖确认卡共用。 */
@Composable
private fun FileManifest(files: AgentScriptFiles, insight: AgentScriptInsight) {
    val colors = KioraTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ManifestLine(AgentScriptFiles.FILE_MAIN, files.mainJava, blankHint = "为空，不写入")
        ManifestLine(AgentScriptFiles.FILE_INFO, files.infoProp, blankHint = "为空，按模板生成")
        ManifestLine(AgentScriptFiles.FILE_DESC, files.descTxt, blankHint = "为空，写入默认描述")
        // 分层脚本带来的附加文件也一并列出，让用户在落盘前看清会写出哪些目录
        files.extraFiles.forEach { (path, content) ->
            ManifestLine(path, content, blankHint = "为空，不写入")
        }
    }

    if (!insight.isEmpty) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "能力摘要 · ${insight.summary}",
            fontSize = 11.sp,
            color = AccentGreen,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ManifestLine(name: String, content: String, blankHint: String) {
    val colors = KioraTheme.colors

    val blank = content.isBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (blank) "○" else "●",
            fontSize = 10.sp,
            color = if (blank) colors.textSecondary else AccentGreen
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (blank) "$name（$blankHint）" else name,
            fontSize = 12.sp,
            color = if (blank) colors.textSecondary else colors.textPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        if (!blank) {
            Text(
                text = "${content.lines().size} 行 · ${content.length} 字",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

// ==================== 剪贴板 ====================

private fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) {
        Toasts.qqToast(1, "当前文件为空")
        return
    }
    runCatching {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("KioraScript", text))
        Toasts.qqToast(2, "已复制到剪贴板")
    }.onFailure {
        Toasts.qqToast(1, "复制失败: ${it.message}")
    }
}
