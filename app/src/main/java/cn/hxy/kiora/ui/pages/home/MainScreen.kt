package cn.hxy.kiora.ui.pages.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.R
import cn.hxy.kiora.ui.components.atoms.KioraCard
import cn.hxy.kiora.ui.core.theme.AccentGreen
import cn.hxy.kiora.ui.core.theme.AccentRed
import cn.hxy.kiora.ui.core.theme.KioraTheme

@Composable
fun MainScreen(
    versionName: String,
    versionCode: Int,
    activations: List<HostActivation>,
    frameworkInfo: String,
    isIconVisible: Boolean,
    onToggleIcon: () -> Unit,
    commitHash: String,
    commitTime: String,
    modifier: Modifier = Modifier
) {
    val colors = KioraTheme.colors

    val iconButtonAlpha by animateFloatAsState(
        targetValue = if (isIconVisible) 1f else 0.6f,
        animationSpec = tween(durationMillis = 200),
        label = "iconAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 品牌区：logo + 名称 + 版本胶囊 ──────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Kiora",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "QQ / TIM / WeChat 适配模块",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            KioraCard(animateContentSize = false) {
                Text(
                    "v$versionName · $versionCode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(12.dp, 6.dp)
                )
            }
        }

        // ── 激活状态：逐宿主一列 ──────────────────────────────
        InfoCard(title = "激活状态", accent = colors.accentGreen) {
            activations.forEach { item -> HostStatusRow(item) }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.textSecondary.copy(alpha = 0.15f))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                frameworkInfo,
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )
        }

        AdaptedVersionCard()

        BuildInfoCard(commitHash, commitTime)

        DevelopersCard()

        OssNoticeCard()

        // ── 隐藏桌面图标：低频危险操作，下沉为低强调入口 ──────────
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .alpha(iconButtonAlpha)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleIcon)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_logo_unchecked),
                null,
                Modifier.size(16.dp),
                colors.textSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isIconVisible) "隐藏桌面图标" else "显示桌面图标",
                fontSize = 13.sp,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/** 激活卡里的一行：状态灯 + 宿主名 + 右对齐的状态字。 */
@Composable
private fun HostStatusRow(item: HostActivation) {
    val colors = KioraTheme.colors
    val color = when (item.state) {
        ActivationState.ACTIVE -> AccentGreen
        ActivationState.INACTIVE -> AccentRed
        ActivationState.UNKNOWN -> colors.textSecondary
    }
    val label = when (item.state) {
        ActivationState.ACTIVE -> "已激活"
        ActivationState.INACTIVE -> "未激活"
        ActivationState.UNKNOWN -> "未知"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            item.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
