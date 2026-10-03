package dev.ujhhgtg.wekit.features

import dev.ujhhgtg.wekit.features.core.BaseFeature
import dev.ujhhgtg.wekit.features.core.validateFeatures
import dev.ujhhgtg.wekit.features.items.chat.DisablePat
import dev.ujhhgtg.wekit.features.items.miniapps.SkipSplash
import dev.ujhhgtg.wekit.features.items.moments.DisableVideosAutoPlay
import dev.ujhhgtg.wekit.features.items.system.RemoveExternalAppSharingSignatureVerify

/**
 * WeKit 血统功能的注册表。
 *
 * WeKit 原版靠 KSP 扫描 `BaseFeature` 子类型，编译期生成 `FeaturesProvider`
 * （`libs/common:annotation-scanner`）。**切片改手写注册表** —— 为 4 个功能引入
 * 一套 KSP 代码生成器不划算。
 *
 * 功能数量上来之后应该补回扫描器，否则每加一个功能都要改这里，而且容易漏。
 */
object WxFeatureRegistry {

    val all: List<BaseFeature> by lazy {
        validateFeatures(
            listOf(
                DisablePat,
                RemoveExternalAppSharingSignatureVerify,
                SkipSplash,
                DisableVideosAutoPlay,
            )
        )
    }
}
