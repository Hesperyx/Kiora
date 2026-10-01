package cn.hxy.kiora.ui.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.common.ModuleMeta
import cn.hxy.kiora.host.HostAdapters
import cn.hxy.kiora.ui.components.atoms.KioraCard
import cn.hxy.kiora.ui.core.theme.AccentOrange
import cn.hxy.kiora.ui.core.theme.KioraTheme

/** 已适配的宿主版本。 */
@Composable
fun AdaptedVersionCard(modifier: Modifier = Modifier) {
    InfoCard(title = "适配版本", accent = KioraTheme.colors.accentGreen, modifier = modifier) {
        // 走 adapters 而不是写死几行，接入新宿主时这里自动跟上。
        HostAdapters.all.forEach { adapter ->
            InfoRow(adapter.displayName, "v${adapter.adaptedVersions} 及以上")
        }
    }
}

/** 构建信息：提交哈希与提交时间由 Gradle 在编译时注入 BuildConfig。 */
@Composable
fun BuildInfoCard(
    commitHash: String,
    commitTime: String,
    modifier: Modifier = Modifier
) {
    InfoCard(title = "构建信息", accent = KioraTheme.colors.accentBlue, modifier = modifier) {
        InfoRow("提交", commitHash)
        InfoRow("提交时间", commitTime)
    }
}

/** 开发者：在线头像，点击跳转到对应 QQ 主页。 */
@Composable
fun DevelopersCard(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    InfoCard(title = "开发者", accent = DevAccent, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModuleMeta.developers.forEach { dev ->
                RingAvatar(url = dev.avatarUrl) {
                    runCatching {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(dev.profileUrl)
                        )
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

/** 开源项目使用声明：列明模块用到的开源项目，并声明其著作权与许可归属。 */
@Composable
fun OssNoticeCard(modifier: Modifier = Modifier) {
    val colors = KioraTheme.colors
    InfoCard(title = "开源项目使用声明", accent = AccentOrange, modifier = modifier) {
        Text(
            "本模块在实现过程中使用了以下开源项目。其著作权及相关许可归各自作者所有，" +
                "具体条款以各项目官方仓库为准。",
            fontSize = 13.sp,
            color = colors.textSecondary,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        ModuleMeta.ossCredits.forEach { name ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.textSecondary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    name,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 卡片共用的外壳：标题（带强调条） + 若干行信息。主页的激活卡也复用它。 */
@Composable
internal fun InfoCard(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = KioraTheme.colors
    KioraCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/** 左标签右取值；取值可能较长（如提交哈希、致谢说明），允许换行。 */
@Composable
private fun InfoRow(label: String, value: String) {
    val colors = KioraTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.width(84.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = colors.textPrimary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
