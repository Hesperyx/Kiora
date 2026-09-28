package cn.hxy.kiora.ui.pages.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.hxy.kiora.common.ModuleMeta
import cn.hxy.kiora.ui.components.atoms.KioraCard
import cn.hxy.kiora.ui.core.theme.KioraTheme

/** 已适配的宿主版本。 */
@Composable
fun AdaptedVersionCard(modifier: Modifier = Modifier) {
    InfoCard(title = "适配版本", modifier = modifier) {
        InfoRow("QQ", "v${ModuleMeta.ADAPTED_QQ_VERSION} 及以上")
        InfoRow("TIM", "v${ModuleMeta.ADAPTED_TIM_VERSION} 及以上")
        InfoRow("开发基线", "QQ ${ModuleMeta.DEV_BASELINE_VERSION}")
    }
}

/** 构建信息：提交哈希与提交时间由 Gradle 在编译时注入 BuildConfig。 */
@Composable
fun BuildInfoCard(
    commitHash: String,
    commitTime: String,
    modifier: Modifier = Modifier
) {
    InfoCard(title = "构建信息", modifier = modifier) {
        InfoRow("提交", commitHash)
        InfoRow("提交时间", commitTime)
    }
}

/** 致谢：模块用到的开源项目，只列项目名。 */
@Composable
fun CreditsCard(modifier: Modifier = Modifier) {
    val colors = KioraTheme.colors
    InfoCard(title = "致谢", modifier = modifier) {
        ModuleMeta.ossCredits.forEach { name ->
            Text(
                name,
                fontSize = 13.sp,
                color = colors.textPrimary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/** 三张卡片共用的外壳：标题 + 若干行信息。 */
@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = KioraTheme.colors
    KioraCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
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
