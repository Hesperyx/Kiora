package dev.ujhhgtg.wekit.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import dev.ujhhgtg.wekit.BuildConfig
import dev.ujhhgtg.wekit.utils.android.isDarkMode

/**
 * 透明的中转 Activity。
 *
 * 基类跟随上游用 [FragmentActivity] 而非 `ComponentActivity`：`BiometricPrompt` 的
 * Activity 构造器只接受 `FragmentActivity`，指纹支付（FingerprintPay）要在这个 Activity
 * 的 `launch` 回调里弹生物识别。`FragmentActivity` 本身就是 `ComponentActivity` 的子类，
 * 现有调用方不受影响。
 */
class TransparentActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            requestFeature(Window.FEATURE_NO_TITLE)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            WindowCompat.setDecorFitsSystemWindows(this, false)
            WindowInsetsControllerCompat(this, this.decorView).isAppearanceLightStatusBars = !isDarkMode
        }
        setTheme(android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)

        val action = pendingAction ?: run { finish(); return }
        pendingAction = null
        action(this)
    }

    companion object {
        @Volatile
        private var pendingAction: (FragmentActivity.() -> Unit)? = null

        fun launch(context: Context, action: FragmentActivity.() -> Unit) {
            pendingAction = action
            context.startActivity(
                Intent(context, TransparentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(BuildConfig.TAG, true)
                }
            )
        }
    }
}