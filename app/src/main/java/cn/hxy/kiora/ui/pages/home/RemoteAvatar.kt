package cn.hxy.kiora.ui.pages.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.hxy.kiora.ui.core.theme.KioraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin

/** 开发者展示用的强调色（紫），浅色/深色下都清晰。 */
internal val DevAccent = Color(0xFF7C4DFF)

/** 头像内存缓存：同一 URL 只下载一次，重进页面不重复请求。 */
private val avatarCache = ConcurrentHashMap<String, Bitmap>()

private suspend fun fetchAvatar(url: String): Bitmap? = withContext(Dispatchers.IO) {
    avatarCache[url]?.let { return@withContext it }
    runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
        }
        conn.inputStream.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()?.also { avatarCache[url] = it }
}

/** 在线头像：下载完成前先显示占位圆，不阻塞布局。 */
@Composable
fun RemoteAvatar(url: String, size: Dp, modifier: Modifier = Modifier) {
    val colors = KioraTheme.colors
    var bitmap by remember(url) { mutableStateOf(avatarCache[url]) }

    LaunchedEffect(url) {
        if (bitmap == null) bitmap = fetchAvatar(url)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.textSecondary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 头像直径。 */
private val AvatarSize = 60.dp

/** 描边圆直径，比头像大 4dp，四周露出 2dp 渐变边。 */
private val AvatarRingSize = 64.dp

/**
 * 波纹容器直径。波纹由 [AvatarRingSize] 扩散到容器边缘即止，
 * 因此容器 = 64dp × [WaveEndScale] = 88dp，不会画出布局外。
 */
private val AvatarWaveSize = 88.dp

/** 单圈波纹扩散到底时的缩放倍数（88 / 64）。 */
private const val WaveEndScale = 1.375f

/** 波纹线条不透明度峰值。 */
private const val WavePeakAlpha = 0.5f

/** 单圈波纹从描边扩散到最外的时长。 */
private const val WaveDurationMillis = 2200

/**
 * 带渐变描边的圆形头像，外圈是持续向外扩散的波纹。
 *
 * 两圈波纹共用同一个 0→1 相位，第二圈偏移半周期，任一时刻总有一圈在外扩；
 * 亮度走 `sin(p·π)`，两端都归零，所以循环处看不出接缝。
 * 相位在 [graphicsLayer] 里才读取，逐帧只刷新绘制层，不触发重组。
 */
@Composable
fun RingAvatar(url: String) {
    val colors = KioraTheme.colors
    val brush = remember(colors.accentBlue) {
        Brush.linearGradient(listOf(colors.accentBlue, DevAccent))
    }

    val wave = rememberInfiniteTransition(label = "avatarWave")
    val phase = wave.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WaveDurationMillis, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    Box(
        modifier = Modifier.size(AvatarWaveSize),
        contentAlignment = Alignment.Center
    ) {
        // 向外扩散的两圈波纹（相位相差半个周期）
        listOf(0f, 0.5f).forEach { offset ->
            Box(
                modifier = Modifier
                    .size(AvatarRingSize)
                    .graphicsLayer {
                        val progress = (phase.value + offset) % 1f
                        val scale = 1f + (WaveEndScale - 1f) * progress
                        scaleX = scale
                        scaleY = scale
                        alpha = sin(progress * PI).toFloat() * WavePeakAlpha
                    }
                    .border(1.5.dp, brush, CircleShape)
            )
        }

        // 静态渐变描边 + 头像
        Box(
            modifier = Modifier
                .size(AvatarRingSize)
                .background(brush, CircleShape)
        )
        RemoteAvatar(url = url, size = AvatarSize)
    }
}
