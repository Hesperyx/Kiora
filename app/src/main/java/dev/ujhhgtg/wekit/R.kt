package dev.ujhhgtg.wekit

/**
 * WeKit 血统代码的资源入口桥。
 *
 * 功能文件统一 `import dev.ujhhgtg.wekit.R` 并用 `R.string.xxx`，而资源实际由 Kiora
 * 模块产出（`cn.hxy.kiora.R`）。这里不能省事写成 `typealias R = cn.hxy.kiora.R` ——
 * Kotlin 的 typealias **不支持嵌套类访问**，`R.string` 会报 Unresolved reference。
 * 所以只能手写一层转发。
 *
 * 维护约定：
 * 1. 字符串名与 WeKit 上游保持一致（便于对照、便于同步），值放在
 *    `app/src/main/res/values/strings.xml`。
 * 2. 新增功能时，把用到的字符串加到资源里，并在这里补一行转发。
 *    漏补的表现是编译期 Unresolved reference，不会静默失效。
 */
object R {
    object string {
        val feature_disable_pat_name = cn.hxy.kiora.R.string.feature_disable_pat_name
        val feature_disable_pat_description = cn.hxy.kiora.R.string.feature_disable_pat_description

        val feature_remove_external_app_sharing_signature_verify_name =
            cn.hxy.kiora.R.string.feature_remove_external_app_sharing_signature_verify_name
        val feature_remove_external_app_sharing_signature_verify_description =
            cn.hxy.kiora.R.string.feature_remove_external_app_sharing_signature_verify_description

        val feature_skip_splash_name = cn.hxy.kiora.R.string.feature_skip_splash_name
        val feature_skip_splash_description = cn.hxy.kiora.R.string.feature_skip_splash_description

        val feature_disable_videos_auto_play_name = cn.hxy.kiora.R.string.feature_disable_videos_auto_play_name
        val feature_disable_videos_auto_play_description =
            cn.hxy.kiora.R.string.feature_disable_videos_auto_play_description

        // 第二批迁入
        val feature_anti_read_receipts_name = cn.hxy.kiora.R.string.feature_anti_read_receipts_name
        val feature_anti_read_receipts_description = cn.hxy.kiora.R.string.feature_anti_read_receipts_description
        val feature_bypass_risk_file_blocking_name = cn.hxy.kiora.R.string.feature_bypass_risk_file_blocking_name
        val feature_bypass_risk_file_blocking_description = cn.hxy.kiora.R.string.feature_bypass_risk_file_blocking_description
        val feature_disable_message_collapsing_name = cn.hxy.kiora.R.string.feature_disable_message_collapsing_name
        val feature_disable_message_collapsing_description = cn.hxy.kiora.R.string.feature_disable_message_collapsing_description
        val feature_disable_resume_watching_toast_name = cn.hxy.kiora.R.string.feature_disable_resume_watching_toast_name
        val feature_disable_resume_watching_toast_description = cn.hxy.kiora.R.string.feature_disable_resume_watching_toast_description
        val feature_disable_share_screenshot_toast_name = cn.hxy.kiora.R.string.feature_disable_share_screenshot_toast_name
        val feature_disable_share_screenshot_toast_description = cn.hxy.kiora.R.string.feature_disable_share_screenshot_toast_description
        val feature_disable_typing_status_uploading_name = cn.hxy.kiora.R.string.feature_disable_typing_status_uploading_name
        val feature_disable_typing_status_uploading_description = cn.hxy.kiora.R.string.feature_disable_typing_status_uploading_description
        val feature_hide_other_devices_banner_name = cn.hxy.kiora.R.string.feature_hide_other_devices_banner_name
        val feature_hide_other_devices_banner_description = cn.hxy.kiora.R.string.feature_hide_other_devices_banner_description
        val feature_merge_chat_message_context_menu_items_name = cn.hxy.kiora.R.string.feature_merge_chat_message_context_menu_items_name
        val feature_merge_chat_message_context_menu_items_description = cn.hxy.kiora.R.string.feature_merge_chat_message_context_menu_items_description
        val feature_nerf_background_process_checker_name = cn.hxy.kiora.R.string.feature_nerf_background_process_checker_name
        val feature_nerf_background_process_checker_description = cn.hxy.kiora.R.string.feature_nerf_background_process_checker_description
        val feature_remove_splash_ads_name = cn.hxy.kiora.R.string.feature_remove_splash_ads_name
        val feature_remove_splash_ads_description = cn.hxy.kiora.R.string.feature_remove_splash_ads_description
        val feature_spoof_host_version_name = cn.hxy.kiora.R.string.feature_spoof_host_version_name
        val feature_spoof_host_version_description = cn.hxy.kiora.R.string.feature_spoof_host_version_description
    }
}
