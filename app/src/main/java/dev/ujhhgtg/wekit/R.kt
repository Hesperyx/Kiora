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
        // features.api
        val feature_we_app_msg_api_description = cn.hxy.kiora.R.string.feature_we_app_msg_api_description
        val feature_we_app_msg_api_name = cn.hxy.kiora.R.string.feature_we_app_msg_api_name
        val feature_we_auth_api_description = cn.hxy.kiora.R.string.feature_we_auth_api_description
        val feature_we_auth_api_name = cn.hxy.kiora.R.string.feature_we_auth_api_name
        val feature_we_contact_api_description = cn.hxy.kiora.R.string.feature_we_contact_api_description
        val feature_we_contact_api_name = cn.hxy.kiora.R.string.feature_we_contact_api_name
        val feature_we_contact_label_api_description = cn.hxy.kiora.R.string.feature_we_contact_label_api_description
        val feature_we_contact_label_api_name = cn.hxy.kiora.R.string.feature_we_contact_label_api_name
        val feature_we_conversation_api_description = cn.hxy.kiora.R.string.feature_we_conversation_api_description
        val feature_we_conversation_api_name = cn.hxy.kiora.R.string.feature_we_conversation_api_name
        val feature_we_database_api_description = cn.hxy.kiora.R.string.feature_we_database_api_description
        val feature_we_database_api_name = cn.hxy.kiora.R.string.feature_we_database_api_name
        val feature_we_database_listener_api_description = cn.hxy.kiora.R.string.feature_we_database_listener_api_description
        val feature_we_database_listener_api_name = cn.hxy.kiora.R.string.feature_we_database_listener_api_name
        val feature_we_group_api_description = cn.hxy.kiora.R.string.feature_we_group_api_description
        val feature_we_group_api_name = cn.hxy.kiora.R.string.feature_we_group_api_name
        val feature_we_message_api_description = cn.hxy.kiora.R.string.feature_we_message_api_description
        val feature_we_message_api_name = cn.hxy.kiora.R.string.feature_we_message_api_name
        val feature_we_net_scene_api_description = cn.hxy.kiora.R.string.feature_we_net_scene_api_description
        val feature_we_net_scene_api_name = cn.hxy.kiora.R.string.feature_we_net_scene_api_name
        val feature_we_packet_dispatcher_description = cn.hxy.kiora.R.string.feature_we_packet_dispatcher_description
        val feature_we_packet_dispatcher_name = cn.hxy.kiora.R.string.feature_we_packet_dispatcher_name
        val feature_we_packet_helper_name = cn.hxy.kiora.R.string.feature_we_packet_helper_name
        val feature_we_payment_api_description = cn.hxy.kiora.R.string.feature_we_payment_api_description
        val feature_we_payment_api_name = cn.hxy.kiora.R.string.feature_we_payment_api_name
        val feature_we_service_api_description = cn.hxy.kiora.R.string.feature_we_service_api_description
        val feature_we_service_api_name = cn.hxy.kiora.R.string.feature_we_service_api_name
        val feature_we_text_status_api_description = cn.hxy.kiora.R.string.feature_we_text_status_api_description
        val feature_we_text_status_api_name = cn.hxy.kiora.R.string.feature_we_text_status_api_name
        val feature_we_transfer_api_description = cn.hxy.kiora.R.string.feature_we_transfer_api_description
        val feature_we_transfer_api_name = cn.hxy.kiora.R.string.feature_we_transfer_api_name
        val feature_we_unsafe_api_description = cn.hxy.kiora.R.string.feature_we_unsafe_api_description
        val feature_we_unsafe_api_name = cn.hxy.kiora.R.string.feature_we_unsafe_api_name
        val feature_we_xml_parser_api_description = cn.hxy.kiora.R.string.feature_we_xml_parser_api_description
        val feature_we_xml_parser_api_name = cn.hxy.kiora.R.string.feature_we_xml_parser_api_name
        val language_english = cn.hxy.kiora.R.string.language_english
        val language_follow_system = cn.hxy.kiora.R.string.language_follow_system
        val language_meow_chinese = cn.hxy.kiora.R.string.language_meow_chinese
        val language_simplified_chinese = cn.hxy.kiora.R.string.language_simplified_chinese
        val language_traditional_chinese = cn.hxy.kiora.R.string.language_traditional_chinese
        val message_type_account_video = cn.hxy.kiora.R.string.message_type_account_video
        val message_type_app = cn.hxy.kiora.R.string.message_type_app
        val message_type_card = cn.hxy.kiora.R.string.message_type_card
        val message_type_contact_recommend = cn.hxy.kiora.R.string.message_type_contact_recommend
        val message_type_file = cn.hxy.kiora.R.string.message_type_file
        val message_type_friend_verify = cn.hxy.kiora.R.string.message_type_friend_verify
        val message_type_group_note = cn.hxy.kiora.R.string.message_type_group_note
        val message_type_image = cn.hxy.kiora.R.string.message_type_image
        val message_type_link = cn.hxy.kiora.R.string.message_type_link
        val message_type_location = cn.hxy.kiora.R.string.message_type_location
        val message_type_micro_video = cn.hxy.kiora.R.string.message_type_micro_video
        val message_type_moments = cn.hxy.kiora.R.string.message_type_moments
        val message_type_music = cn.hxy.kiora.R.string.message_type_music
        val message_type_pat = cn.hxy.kiora.R.string.message_type_pat
        val message_type_product = cn.hxy.kiora.R.string.message_type_product
        val message_type_quote = cn.hxy.kiora.R.string.message_type_quote
        val message_type_recall = cn.hxy.kiora.R.string.message_type_recall
        val message_type_red_packet = cn.hxy.kiora.R.string.message_type_red_packet
        val message_type_red_packet_cover = cn.hxy.kiora.R.string.message_type_red_packet_cover
        val message_type_service = cn.hxy.kiora.R.string.message_type_service
        val message_type_sogou_emoji = cn.hxy.kiora.R.string.message_type_sogou_emoji
        val message_type_special_red_packet = cn.hxy.kiora.R.string.message_type_special_red_packet
        val message_type_status = cn.hxy.kiora.R.string.message_type_status
        val message_type_sticker = cn.hxy.kiora.R.string.message_type_sticker
        val message_type_system = cn.hxy.kiora.R.string.message_type_system
        val message_type_system_location = cn.hxy.kiora.R.string.message_type_system_location
        val message_type_system_notice = cn.hxy.kiora.R.string.message_type_system_notice
        val message_type_text = cn.hxy.kiora.R.string.message_type_text
        val message_type_transfer = cn.hxy.kiora.R.string.message_type_transfer
        val message_type_unknown = cn.hxy.kiora.R.string.message_type_unknown
        val message_type_video = cn.hxy.kiora.R.string.message_type_video
        val message_type_video_account = cn.hxy.kiora.R.string.message_type_video_account
        val message_type_video_account_card = cn.hxy.kiora.R.string.message_type_video_account_card
        val message_type_video_account_live = cn.hxy.kiora.R.string.message_type_video_account_live
        val message_type_voice = cn.hxy.kiora.R.string.message_type_voice
        val message_type_voip = cn.hxy.kiora.R.string.message_type_voip
        val message_type_voip_invite = cn.hxy.kiora.R.string.message_type_voip_invite
        val message_type_voip_notify = cn.hxy.kiora.R.string.message_type_voip_notify
        val res_inject_success = cn.hxy.kiora.R.string.res_inject_success
        // features/items
        val chat_abnormal_sticker_blocked = cn.hxy.kiora.R.string.chat_abnormal_sticker_blocked
        val feature_anti_moments_delete_description = cn.hxy.kiora.R.string.feature_anti_moments_delete_description
        val feature_anti_moments_delete_name = cn.hxy.kiora.R.string.feature_anti_moments_delete_name
        val feature_anti_sec_msg_description = cn.hxy.kiora.R.string.feature_anti_sec_msg_description
        val feature_anti_sec_msg_name = cn.hxy.kiora.R.string.feature_anti_sec_msg_name
        val feature_auto_dnd_after_join_group_description = cn.hxy.kiora.R.string.feature_auto_dnd_after_join_group_description
        val feature_auto_dnd_after_join_group_name = cn.hxy.kiora.R.string.feature_auto_dnd_after_join_group_name
        val feature_block_abnormal_size_stickers_description = cn.hxy.kiora.R.string.feature_block_abnormal_size_stickers_description
        val feature_block_abnormal_size_stickers_name = cn.hxy.kiora.R.string.feature_block_abnormal_size_stickers_name
        val feature_disable_pinned_chats_collapsing_description = cn.hxy.kiora.R.string.feature_disable_pinned_chats_collapsing_description
        val feature_disable_pinned_chats_collapsing_name = cn.hxy.kiora.R.string.feature_disable_pinned_chats_collapsing_name
        val feature_disable_web_view_safety_warnings_description = cn.hxy.kiora.R.string.feature_disable_web_view_safety_warnings_description
        val feature_disable_web_view_safety_warnings_name = cn.hxy.kiora.R.string.feature_disable_web_view_safety_warnings_name
        val feature_enhance_query_description = cn.hxy.kiora.R.string.feature_enhance_query_description
        val feature_enhance_query_name = cn.hxy.kiora.R.string.feature_enhance_query_name
        val feature_moments_editor_back_optimization_description = cn.hxy.kiora.R.string.feature_moments_editor_back_optimization_description
        val feature_moments_editor_back_optimization_name = cn.hxy.kiora.R.string.feature_moments_editor_back_optimization_name
        val feature_remove_article_ads_description = cn.hxy.kiora.R.string.feature_remove_article_ads_description
        val feature_remove_article_ads_name = cn.hxy.kiora.R.string.feature_remove_article_ads_name
        val feature_remove_limits_during_calls_description = cn.hxy.kiora.R.string.feature_remove_limits_during_calls_description
        val feature_remove_limits_during_calls_name = cn.hxy.kiora.R.string.feature_remove_limits_during_calls_name
        val feature_remove_menu_limits_description = cn.hxy.kiora.R.string.feature_remove_menu_limits_description
        val feature_remove_menu_limits_name = cn.hxy.kiora.R.string.feature_remove_menu_limits_name
        val feature_remove_official_account_ads_description = cn.hxy.kiora.R.string.feature_remove_official_account_ads_description
        val feature_remove_official_account_ads_name = cn.hxy.kiora.R.string.feature_remove_official_account_ads_name
        val m7g = cn.hxy.kiora.R.string.m7g
        val m7h = cn.hxy.kiora.R.string.m7h
        val moments_content_type_article_video = cn.hxy.kiora.R.string.moments_content_type_article_video
        val moments_content_type_channels_long_video = cn.hxy.kiora.R.string.moments_content_type_channels_long_video
        val moments_content_type_channels_tv = cn.hxy.kiora.R.string.moments_content_type_channels_tv
        val moments_content_type_channels_video = cn.hxy.kiora.R.string.moments_content_type_channels_video
        val moments_content_type_coupon = cn.hxy.kiora.R.string.moments_content_type_coupon
        val moments_content_type_image = cn.hxy.kiora.R.string.moments_content_type_image
        val moments_content_type_link = cn.hxy.kiora.R.string.moments_content_type_link
        val moments_content_type_listen_audio = cn.hxy.kiora.R.string.moments_content_type_listen_audio
        val moments_content_type_lite_app = cn.hxy.kiora.R.string.moments_content_type_lite_app
        val moments_content_type_live = cn.hxy.kiora.R.string.moments_content_type_live
        val moments_content_type_live_photo = cn.hxy.kiora.R.string.moments_content_type_live_photo
        val moments_content_type_miniapp_page = cn.hxy.kiora.R.string.moments_content_type_miniapp_page
        val moments_content_type_music = cn.hxy.kiora.R.string.moments_content_type_music
        val moments_content_type_note = cn.hxy.kiora.R.string.moments_content_type_note
        val moments_content_type_product = cn.hxy.kiora.R.string.moments_content_type_product
        val moments_content_type_product_old = cn.hxy.kiora.R.string.moments_content_type_product_old
        val moments_content_type_rich_music = cn.hxy.kiora.R.string.moments_content_type_rich_music
        val moments_content_type_short_video = cn.hxy.kiora.R.string.moments_content_type_short_video
        val moments_content_type_sticker = cn.hxy.kiora.R.string.moments_content_type_sticker
        val moments_content_type_stream = cn.hxy.kiora.R.string.moments_content_type_stream
        val moments_content_type_text = cn.hxy.kiora.R.string.moments_content_type_text
        val moments_content_type_video = cn.hxy.kiora.R.string.moments_content_type_video
        val moments_editor_back_hint = cn.hxy.kiora.R.string.moments_editor_back_hint
        // clickable
        val feature_auto_clean_cache_description = cn.hxy.kiora.R.string.feature_auto_clean_cache_description
        val feature_auto_clean_cache_name = cn.hxy.kiora.R.string.feature_auto_clean_cache_name
        val feature_external_sharing_evolved_description = cn.hxy.kiora.R.string.feature_external_sharing_evolved_description
        val feature_external_sharing_evolved_name = cn.hxy.kiora.R.string.feature_external_sharing_evolved_name
        val feature_profile_memory_description = cn.hxy.kiora.R.string.feature_profile_memory_description
        val feature_profile_memory_name = cn.hxy.kiora.R.string.feature_profile_memory_name
        val system_auto_clean_complete = cn.hxy.kiora.R.string.system_auto_clean_complete
        val system_auto_clean_next = cn.hxy.kiora.R.string.system_auto_clean_next
        val feature_beautify_view_press_effect_name = cn.hxy.kiora.R.string.feature_beautify_view_press_effect_name
        val feature_beautify_view_press_effect_description = cn.hxy.kiora.R.string.feature_beautify_view_press_effect_description
        val feature_disable_chat_background_dimming_name = cn.hxy.kiora.R.string.feature_disable_chat_background_dimming_name
        val feature_disable_chat_background_dimming_description = cn.hxy.kiora.R.string.feature_disable_chat_background_dimming_description
        val feature_auto_enable_send_as_media_group_name = cn.hxy.kiora.R.string.feature_auto_enable_send_as_media_group_name
        val feature_auto_enable_send_as_media_group_description = cn.hxy.kiora.R.string.feature_auto_enable_send_as_media_group_description
        val feature_auto_enable_send_original_media_name = cn.hxy.kiora.R.string.feature_auto_enable_send_original_media_name
        val feature_auto_enable_send_original_media_description = cn.hxy.kiora.R.string.feature_auto_enable_send_original_media_description
        val feature_auto_view_original_media_name = cn.hxy.kiora.R.string.feature_auto_view_original_media_name
        val feature_auto_view_original_media_description = cn.hxy.kiora.R.string.feature_auto_view_original_media_description
        val feature_disable_speech_to_text_button_name = cn.hxy.kiora.R.string.feature_disable_speech_to_text_button_name
        val feature_disable_speech_to_text_button_description = cn.hxy.kiora.R.string.feature_disable_speech_to_text_button_description
        val feature_force_enable_all_tools_name = cn.hxy.kiora.R.string.feature_force_enable_all_tools_name
        val feature_force_enable_all_tools_description = cn.hxy.kiora.R.string.feature_force_enable_all_tools_description
        val feature_monitor_group_member_operations_name = cn.hxy.kiora.R.string.feature_monitor_group_member_operations_name
        val feature_monitor_group_member_operations_description = cn.hxy.kiora.R.string.feature_monitor_group_member_operations_description
        val feature_quick_remove_quote_name = cn.hxy.kiora.R.string.feature_quick_remove_quote_name
        val feature_quick_remove_quote_description = cn.hxy.kiora.R.string.feature_quick_remove_quote_description
        val feature_remove_custom_stickers_limit_name = cn.hxy.kiora.R.string.feature_remove_custom_stickers_limit_name
        val feature_remove_custom_stickers_limit_description = cn.hxy.kiora.R.string.feature_remove_custom_stickers_limit_description
        val feature_remove_message_selection_limit_name = cn.hxy.kiora.R.string.feature_remove_message_selection_limit_name
        val feature_remove_message_selection_limit_description = cn.hxy.kiora.R.string.feature_remove_message_selection_limit_description
        val feature_remove_send_media_count_limit_name = cn.hxy.kiora.R.string.feature_remove_send_media_count_limit_name
        val feature_remove_send_media_count_limit_description = cn.hxy.kiora.R.string.feature_remove_send_media_count_limit_description
        val feature_auto_add_nearby_friends_name = cn.hxy.kiora.R.string.feature_auto_add_nearby_friends_name
        val feature_auto_add_nearby_friends_description = cn.hxy.kiora.R.string.feature_auto_add_nearby_friends_description
        val feature_display_hidden_contact_settings_name = cn.hxy.kiora.R.string.feature_display_hidden_contact_settings_name
        val feature_display_hidden_contact_settings_description = cn.hxy.kiora.R.string.feature_display_hidden_contact_settings_description
        val feature_remove_message_batch_forward_limit_name = cn.hxy.kiora.R.string.feature_remove_message_batch_forward_limit_name
        val feature_remove_message_batch_forward_limit_description = cn.hxy.kiora.R.string.feature_remove_message_batch_forward_limit_description
        val feature_copy_we_chat_debug_info_name = cn.hxy.kiora.R.string.feature_copy_we_chat_debug_info_name
        val feature_copy_we_chat_debug_info_description = cn.hxy.kiora.R.string.feature_copy_we_chat_debug_info_description
        val feature_crash_interceptor_name = cn.hxy.kiora.R.string.feature_crash_interceptor_name
        val feature_crash_interceptor_description = cn.hxy.kiora.R.string.feature_crash_interceptor_description
        val feature_native_crash_interceptor_name = cn.hxy.kiora.R.string.feature_native_crash_interceptor_name
        val feature_native_crash_interceptor_description = cn.hxy.kiora.R.string.feature_native_crash_interceptor_description
        val feature_rainbow_text_name = cn.hxy.kiora.R.string.feature_rainbow_text_name
        val feature_rainbow_text_description = cn.hxy.kiora.R.string.feature_rainbow_text_description
        val feature_bypass_underage_gaming_limit_name = cn.hxy.kiora.R.string.feature_bypass_underage_gaming_limit_name
        val feature_bypass_underage_gaming_limit_description = cn.hxy.kiora.R.string.feature_bypass_underage_gaming_limit_description
        val feature_remove_embedded_ads_name = cn.hxy.kiora.R.string.feature_remove_embedded_ads_name
        val feature_remove_embedded_ads_description = cn.hxy.kiora.R.string.feature_remove_embedded_ads_description
        val feature_remove_video_ads_name = cn.hxy.kiora.R.string.feature_remove_video_ads_name
        val feature_remove_video_ads_description = cn.hxy.kiora.R.string.feature_remove_video_ads_description
        val feature_skip_rewarded_ads_name = cn.hxy.kiora.R.string.feature_skip_rewarded_ads_name
        val feature_skip_rewarded_ads_description = cn.hxy.kiora.R.string.feature_skip_rewarded_ads_description
        val feature_always_show_interaction_entry_name = cn.hxy.kiora.R.string.feature_always_show_interaction_entry_name
        val feature_always_show_interaction_entry_description = cn.hxy.kiora.R.string.feature_always_show_interaction_entry_description
        val feature_anti_moment_comments_delete_name = cn.hxy.kiora.R.string.feature_anti_moment_comments_delete_name
        val feature_anti_moment_comments_delete_description = cn.hxy.kiora.R.string.feature_anti_moment_comments_delete_description
        val feature_no_close_video_player_on_click_name = cn.hxy.kiora.R.string.feature_no_close_video_player_on_click_name
        val feature_no_close_video_player_on_click_description = cn.hxy.kiora.R.string.feature_no_close_video_player_on_click_description
        val feature_open_details_on_item_click_name = cn.hxy.kiora.R.string.feature_open_details_on_item_click_name
        val feature_open_details_on_item_click_description = cn.hxy.kiora.R.string.feature_open_details_on_item_click_description
        val feature_remove_moments_ads_name = cn.hxy.kiora.R.string.feature_remove_moments_ads_name
        val feature_remove_moments_ads_description = cn.hxy.kiora.R.string.feature_remove_moments_ads_description
        val feature_allow_private_chat_receive_outgoing_red_packets_name = cn.hxy.kiora.R.string.feature_allow_private_chat_receive_outgoing_red_packets_name
        val feature_allow_private_chat_receive_outgoing_red_packets_description = cn.hxy.kiora.R.string.feature_allow_private_chat_receive_outgoing_red_packets_description
        val feature_remove_signature_limits_name = cn.hxy.kiora.R.string.feature_remove_signature_limits_name
        val feature_remove_signature_limits_description = cn.hxy.kiora.R.string.feature_remove_signature_limits_description
        val feature_remove_text_status_length_limit_name = cn.hxy.kiora.R.string.feature_remove_text_status_length_limit_name
        val feature_remove_text_status_length_limit_description = cn.hxy.kiora.R.string.feature_remove_text_status_length_limit_description
        val feature_upload_transparent_avatars_name = cn.hxy.kiora.R.string.feature_upload_transparent_avatars_name
        val feature_upload_transparent_avatars_description = cn.hxy.kiora.R.string.feature_upload_transparent_avatars_description
        val feature_disable_comment_size_limit_name = cn.hxy.kiora.R.string.feature_disable_comment_size_limit_name
        val feature_disable_comment_size_limit_description = cn.hxy.kiora.R.string.feature_disable_comment_size_limit_description
        val feature_remove_comment_ads_name = cn.hxy.kiora.R.string.feature_remove_comment_ads_name
        val feature_remove_comment_ads_description = cn.hxy.kiora.R.string.feature_remove_comment_ads_description
        val feature_disable_high_brightness_name = cn.hxy.kiora.R.string.feature_disable_high_brightness_name
        val feature_disable_high_brightness_description = cn.hxy.kiora.R.string.feature_disable_high_brightness_description
        val feature_disable_host_hot_updates_name = cn.hxy.kiora.R.string.feature_disable_host_hot_updates_name
        val feature_disable_host_hot_updates_description = cn.hxy.kiora.R.string.feature_disable_host_hot_updates_description
        val feature_disable_low_available_storage_detection_name = cn.hxy.kiora.R.string.feature_disable_low_available_storage_detection_name
        val feature_disable_low_available_storage_detection_description = cn.hxy.kiora.R.string.feature_disable_low_available_storage_detection_description
        val feature_enable_web_view_features_name = cn.hxy.kiora.R.string.feature_enable_web_view_features_name
        val feature_enable_web_view_features_description = cn.hxy.kiora.R.string.feature_enable_web_view_features_description
        val feature_hide_module_from_app_list_name = cn.hxy.kiora.R.string.feature_hide_module_from_app_list_name
        val feature_hide_module_from_app_list_description = cn.hxy.kiora.R.string.feature_hide_module_from_app_list_description
        val feature_power_saver_name = cn.hxy.kiora.R.string.feature_power_saver_name
        val feature_power_saver_description = cn.hxy.kiora.R.string.feature_power_saver_description
        val feature_prevent_module_data_deletion_name = cn.hxy.kiora.R.string.feature_prevent_module_data_deletion_name
        val feature_prevent_module_data_deletion_description = cn.hxy.kiora.R.string.feature_prevent_module_data_deletion_description
        val feature_remove_qr_code_scan_limit_name = cn.hxy.kiora.R.string.feature_remove_qr_code_scan_limit_name
        val feature_remove_qr_code_scan_limit_description = cn.hxy.kiora.R.string.feature_remove_qr_code_scan_limit_description
        val feature_spoof_environment_name = cn.hxy.kiora.R.string.feature_spoof_environment_name
        val feature_spoof_environment_description = cn.hxy.kiora.R.string.feature_spoof_environment_description
        val feature_use_legacy_wallet_view_in_me_page_name = cn.hxy.kiora.R.string.feature_use_legacy_wallet_view_in_me_page_name
        val feature_use_legacy_wallet_view_in_me_page_description = cn.hxy.kiora.R.string.feature_use_legacy_wallet_view_in_me_page_description
        val chat_group_member_left = cn.hxy.kiora.R.string.chat_group_member_left
        val chat_group_member_nickname_changed = cn.hxy.kiora.R.string.chat_group_member_nickname_changed
        val chat_group_member_no_nickname = cn.hxy.kiora.R.string.chat_group_member_no_nickname
        val contacts_auto_add_nearby_menu = cn.hxy.kiora.R.string.contacts_auto_add_nearby_menu
        val debug_copied = cn.hxy.kiora.R.string.debug_copied
        val debug_crash_exception_information = cn.hxy.kiora.R.string.debug_crash_exception_information
        val debug_crash_stack_preview = cn.hxy.kiora.R.string.debug_crash_stack_preview
        val debug_crash_summary_parse_failed = cn.hxy.kiora.R.string.debug_crash_summary_parse_failed
        val debug_crash_view_full_log_hint = cn.hxy.kiora.R.string.debug_crash_view_full_log_hint
        val debug_java_crash_details = cn.hxy.kiora.R.string.debug_java_crash_details
        val debug_java_crash_detected = cn.hxy.kiora.R.string.debug_java_crash_detected
        val debug_java_crash_preparing_report = cn.hxy.kiora.R.string.debug_java_crash_preparing_report
        val debug_native_crash_details = cn.hxy.kiora.R.string.debug_native_crash_details
        val debug_native_crash_detected = cn.hxy.kiora.R.string.debug_native_crash_detected
        val debug_native_crash_preparing_report = cn.hxy.kiora.R.string.debug_native_crash_preparing_report
        val moments_interaction_idle = cn.hxy.kiora.R.string.moments_interaction_idle

        // home_screen_menu
        val feature_we_home_screen_popup_menu_api_name = cn.hxy.kiora.R.string.feature_we_home_screen_popup_menu_api_name
        val feature_we_home_screen_popup_menu_api_description = cn.hxy.kiora.R.string.feature_we_home_screen_popup_menu_api_description
        val feature_kill_host_process_name = cn.hxy.kiora.R.string.feature_kill_host_process_name
        val feature_kill_host_process_description = cn.hxy.kiora.R.string.feature_kill_host_process_description
        val home_menu_force_stop = cn.hxy.kiora.R.string.home_menu_force_stop
        val feature_mark_all_as_read_name = cn.hxy.kiora.R.string.feature_mark_all_as_read_name
        val feature_mark_all_as_read_description = cn.hxy.kiora.R.string.feature_mark_all_as_read_description
        val home_menu_mark_all_read = cn.hxy.kiora.R.string.home_menu_mark_all_read
        val home_menu_all_marked_read = cn.hxy.kiora.R.string.home_menu_all_marked_read
        val feature_toggle_all_conversations_visibility_name = cn.hxy.kiora.R.string.feature_toggle_all_conversations_visibility_name
        val feature_toggle_all_conversations_visibility_description = cn.hxy.kiora.R.string.feature_toggle_all_conversations_visibility_description
        val home_menu_show_conversations = cn.hxy.kiora.R.string.home_menu_show_conversations
        val home_menu_hide_conversations = cn.hxy.kiora.R.string.home_menu_hide_conversations

        // WeStartActivityApi / official_accounts
        val feature_we_start_activity_api_name = cn.hxy.kiora.R.string.feature_we_start_activity_api_name
        val feature_we_start_activity_api_description = cn.hxy.kiora.R.string.feature_we_start_activity_api_description
        val feature_use_multi_web_view_for_official_accounts_name = cn.hxy.kiora.R.string.feature_use_multi_web_view_for_official_accounts_name
        val feature_use_multi_web_view_for_official_accounts_description = cn.hxy.kiora.R.string.feature_use_multi_web_view_for_official_accounts_description

        // WeChatMessageViewApi / group member nickname
        val feature_we_chat_message_view_api_name = cn.hxy.kiora.R.string.feature_we_chat_message_view_api_name
        val feature_we_chat_message_view_api_description = cn.hxy.kiora.R.string.feature_we_chat_message_view_api_description
        val feature_remove_group_member_nickname_control_characters_name = cn.hxy.kiora.R.string.feature_remove_group_member_nickname_control_characters_name
        val feature_remove_group_member_nickname_control_characters_description = cn.hxy.kiora.R.string.feature_remove_group_member_nickname_control_characters_description
        val feature_remove_group_member_nickname_length_limit_name = cn.hxy.kiora.R.string.feature_remove_group_member_nickname_length_limit_name
        val feature_remove_group_member_nickname_length_limit_description = cn.hxy.kiora.R.string.feature_remove_group_member_nickname_length_limit_description
        val feature_auto_speech_to_text_name = cn.hxy.kiora.R.string.feature_auto_speech_to_text_name
        val feature_auto_speech_to_text_description = cn.hxy.kiora.R.string.feature_auto_speech_to_text_description
        val feature_merge_messages_into_groups_name = cn.hxy.kiora.R.string.feature_merge_messages_into_groups_name
        val feature_merge_messages_into_groups_description = cn.hxy.kiora.R.string.feature_merge_messages_into_groups_description

        // Phase 1 UI APIs
        val feature_we_alert_dialog_api_name = cn.hxy.kiora.R.string.feature_we_alert_dialog_api_name
        val feature_we_alert_dialog_api_description = cn.hxy.kiora.R.string.feature_we_alert_dialog_api_description
        val feature_we_current_conversation_api_name = cn.hxy.kiora.R.string.feature_we_current_conversation_api_name
        val feature_we_current_conversation_api_description = cn.hxy.kiora.R.string.feature_we_current_conversation_api_description
        // Phase 1 UI APIs batch 2
        val feature_we_chat_input_bar_api_name = cn.hxy.kiora.R.string.feature_we_chat_input_bar_api_name
        val feature_we_chat_input_bar_api_description = cn.hxy.kiora.R.string.feature_we_chat_input_bar_api_description
        val feature_we_main_activity_beautify_api_name = cn.hxy.kiora.R.string.feature_we_main_activity_beautify_api_name
        val feature_we_main_activity_beautify_api_description = cn.hxy.kiora.R.string.feature_we_main_activity_beautify_api_description
        val feature_we_contact_header_api_name = cn.hxy.kiora.R.string.feature_we_contact_header_api_name
        // Phase 1 UI APIs batch 3
        val feature_we_contact_prefs_screen_api_name = cn.hxy.kiora.R.string.feature_we_contact_prefs_screen_api_name
        val feature_we_conversation_context_menu_api_name = cn.hxy.kiora.R.string.feature_we_conversation_context_menu_api_name
        val feature_we_conversation_context_menu_api_description = cn.hxy.kiora.R.string.feature_we_conversation_context_menu_api_description
        val feature_we_conversation_list_view_api_name = cn.hxy.kiora.R.string.feature_we_conversation_list_view_api_name
        val feature_we_conversation_list_view_api_description = cn.hxy.kiora.R.string.feature_we_conversation_list_view_api_description
        val feature_we_short_videos_share_menu_api_name = cn.hxy.kiora.R.string.feature_we_short_videos_share_menu_api_name
        val feature_we_short_videos_share_menu_api_description = cn.hxy.kiora.R.string.feature_we_short_videos_share_menu_api_description
        val feature_we_web_view_api_name = cn.hxy.kiora.R.string.feature_we_web_view_api_name
        val feature_we_web_view_api_description = cn.hxy.kiora.R.string.feature_we_web_view_api_description
        // Phase 2 non-Compose items batch 1
        val feature_troll_ban_name = cn.hxy.kiora.R.string.feature_troll_ban_name
        val feature_troll_ban_description = cn.hxy.kiora.R.string.feature_troll_ban_description
        val feature_quick_back_to_bottom_name = cn.hxy.kiora.R.string.feature_quick_back_to_bottom_name
        val feature_quick_back_to_bottom_description = cn.hxy.kiora.R.string.feature_quick_back_to_bottom_description
        val feature_stickers_manager_enhancements_name = cn.hxy.kiora.R.string.feature_stickers_manager_enhancements_name
        val feature_stickers_manager_enhancements_description = cn.hxy.kiora.R.string.feature_stickers_manager_enhancements_description
        val feature_view_sticker_as_image_name = cn.hxy.kiora.R.string.feature_view_sticker_as_image_name
        val feature_view_sticker_as_image_description = cn.hxy.kiora.R.string.feature_view_sticker_as_image_description
        val feature_display_group_member_inviter_name = cn.hxy.kiora.R.string.feature_display_group_member_inviter_name
        val feature_display_group_member_inviter_description = cn.hxy.kiora.R.string.feature_display_group_member_inviter_description
        val feature_display_group_member_messages_name = cn.hxy.kiora.R.string.feature_display_group_member_messages_name
        val feature_display_group_member_messages_description = cn.hxy.kiora.R.string.feature_display_group_member_messages_description
        val feature_show_friend_add_time_name = cn.hxy.kiora.R.string.feature_show_friend_add_time_name
        val feature_show_friend_add_time_description = cn.hxy.kiora.R.string.feature_show_friend_add_time_description
        val feature_show_wx_id_in_contact_details_name = cn.hxy.kiora.R.string.feature_show_wx_id_in_contact_details_name
        val feature_show_wx_id_in_contact_details_description = cn.hxy.kiora.R.string.feature_show_wx_id_in_contact_details_description
        val chat_contact_tap_to_view = cn.hxy.kiora.R.string.chat_contact_tap_to_view
        val chat_member_inviter_title = cn.hxy.kiora.R.string.chat_member_inviter_title
        val chat_member_inviter_querying = cn.hxy.kiora.R.string.chat_member_inviter_querying
        val chat_member_inviter_no_record = cn.hxy.kiora.R.string.chat_member_inviter_no_record
        val chat_member_inviter_self_joined = cn.hxy.kiora.R.string.chat_member_inviter_self_joined
        val chat_member_inviter_result = cn.hxy.kiora.R.string.chat_member_inviter_result
        val chat_sticker_manager_select_all = cn.hxy.kiora.R.string.chat_sticker_manager_select_all
        val chat_sticker_manager_select_none = cn.hxy.kiora.R.string.chat_sticker_manager_select_none
        val chat_sticker_manager_invert = cn.hxy.kiora.R.string.chat_sticker_manager_invert
        val chat_sticker_manager_export = cn.hxy.kiora.R.string.chat_sticker_manager_export
        val chat_sticker_manager_exporting = cn.hxy.kiora.R.string.chat_sticker_manager_exporting
        val contacts_wechat_id_value = cn.hxy.kiora.R.string.contacts_wechat_id_value
        val contacts_get_failed = cn.hxy.kiora.R.string.contacts_get_failed
        val contacts_copied = cn.hxy.kiora.R.string.contacts_copied
        val contacts_group_message_history = cn.hxy.kiora.R.string.contacts_group_message_history
        val contacts_add_time_value = cn.hxy.kiora.R.string.contacts_add_time_value
        // Phase 2 batch: shortvideos/DownloadMedia
        val feature_download_media_name = cn.hxy.kiora.R.string.feature_download_media_name
        val feature_download_media_description = cn.hxy.kiora.R.string.feature_download_media_description
        val action_download = cn.hxy.kiora.R.string.action_download
        val copied_to_clipboard = cn.hxy.kiora.R.string.copied_to_clipboard
        val shortvideos_copy_link = cn.hxy.kiora.R.string.shortvideos_copy_link
        val shortvideos_duration = cn.hxy.kiora.R.string.shortvideos_duration
        val shortvideos_size = cn.hxy.kiora.R.string.shortvideos_size
        val shortvideos_protected_link = cn.hxy.kiora.R.string.shortvideos_protected_link
        val shortvideos_key = cn.hxy.kiora.R.string.shortvideos_key
        val shortvideos_link = cn.hxy.kiora.R.string.shortvideos_link
        val shortvideos_unknown_media_type = cn.hxy.kiora.R.string.shortvideos_unknown_media_type
        val shortvideos_image_download_started = cn.hxy.kiora.R.string.shortvideos_image_download_started
        val shortvideos_image_download_reported_success = cn.hxy.kiora.R.string.shortvideos_image_download_reported_success
        val shortvideos_image_downloaded_to = cn.hxy.kiora.R.string.shortvideos_image_downloaded_to
        val shortvideos_decrypting_video = cn.hxy.kiora.R.string.shortvideos_decrypting_video
        val shortvideos_downloading_video = cn.hxy.kiora.R.string.shortvideos_downloading_video
        val shortvideos_video_download_failed = cn.hxy.kiora.R.string.shortvideos_video_download_failed
        val shortvideos_video_downloaded_to = cn.hxy.kiora.R.string.shortvideos_video_downloaded_to
        val shortvideos_video_download_started = cn.hxy.kiora.R.string.shortvideos_video_download_started
        // scripting_java/JavaHookApi
        val feature_java_hook_api_name = cn.hxy.kiora.R.string.feature_java_hook_api_name
        val feature_java_hook_api_description = cn.hxy.kiora.R.string.feature_java_hook_api_description
        // miniapps/ErudaConsole
        val feature_eruda_console_name = cn.hxy.kiora.R.string.feature_eruda_console_name
        val feature_eruda_console_description = cn.hxy.kiora.R.string.feature_eruda_console_description
        // contacts/QuickOpenMoments
        val feature_quick_open_moments_name = cn.hxy.kiora.R.string.feature_quick_open_moments_name
        val feature_quick_open_moments_description = cn.hxy.kiora.R.string.feature_quick_open_moments_description
        val contacts_open_moments = cn.hxy.kiora.R.string.contacts_open_moments
        // beautify/HideHomeScreenSwipeDownPage
        val feature_hide_home_screen_swipe_down_page_name = cn.hxy.kiora.R.string.feature_hide_home_screen_swipe_down_page_name
        val feature_hide_home_screen_swipe_down_page_description = cn.hxy.kiora.R.string.feature_hide_home_screen_swipe_down_page_description
        // contacts/OpenConversationDialog + home_screen_menu/OpenConversationMenu
        val feature_open_conversation_name = cn.hxy.kiora.R.string.feature_open_conversation_name
        val feature_open_conversation_menu_name = cn.hxy.kiora.R.string.feature_open_conversation_menu_name
        val feature_open_conversation_menu_description = cn.hxy.kiora.R.string.feature_open_conversation_menu_description
        val home_menu_open_conversation = cn.hxy.kiora.R.string.home_menu_open_conversation
        val contacts_wechat_id = cn.hxy.kiora.R.string.contacts_wechat_id
        val contacts_wechat_id_empty = cn.hxy.kiora.R.string.contacts_wechat_id_empty
        val contacts_open_homepage = cn.hxy.kiora.R.string.contacts_open_homepage
        val contacts_open_settings = cn.hxy.kiora.R.string.contacts_open_settings
        val contacts_open_chat = cn.hxy.kiora.R.string.contacts_open_chat
        val feature_open_conversation_description = cn.hxy.kiora.R.string.feature_open_conversation_description
        val debug_crash_copy_full_log = cn.hxy.kiora.R.string.debug_crash_copy_full_log
        val debug_crash_ignore = cn.hxy.kiora.R.string.debug_crash_ignore
        val debug_crash_log_truncated_notice = cn.hxy.kiora.R.string.debug_crash_log_truncated_notice
        val debug_crash_view_details = cn.hxy.kiora.R.string.debug_crash_view_details
        val feature_predictive_back_gestures_name = cn.hxy.kiora.R.string.feature_predictive_back_gestures_name
        val feature_predictive_back_gestures_description = cn.hxy.kiora.R.string.feature_predictive_back_gestures_description
        val feature_qr_code_record_name = cn.hxy.kiora.R.string.feature_qr_code_record_name
        val feature_qr_code_record_description = cn.hxy.kiora.R.string.feature_qr_code_record_description
        val qr_code_record_home_menu_title = cn.hxy.kiora.R.string.qr_code_record_home_menu_title
        val system_qr_code_record_empty = cn.hxy.kiora.R.string.system_qr_code_record_empty
        val system_qr_code_record_open = cn.hxy.kiora.R.string.system_qr_code_record_open
        val system_qr_code_record_copy = cn.hxy.kiora.R.string.system_qr_code_record_copy
        val system_qr_code_record_clear = cn.hxy.kiora.R.string.system_qr_code_record_clear
        val system_qr_code_record_clear_description = cn.hxy.kiora.R.string.system_qr_code_record_clear_description
        val system_qr_code_record_cleared = cn.hxy.kiora.R.string.system_qr_code_record_cleared
        val qr_code_record_open_failed = cn.hxy.kiora.R.string.qr_code_record_open_failed
        val feature_module_settings_name = cn.hxy.kiora.R.string.feature_module_settings_name
        val feature_module_settings_description = cn.hxy.kiora.R.string.feature_module_settings_description
        val feature_we_chat_input_bar_menu_api_name = cn.hxy.kiora.R.string.feature_we_chat_input_bar_menu_api_name
        val feature_we_chat_input_bar_menu_api_description = cn.hxy.kiora.R.string.feature_we_chat_input_bar_menu_api_description
        val feature_chat_footer_hooks_name = cn.hxy.kiora.R.string.feature_chat_footer_hooks_name
        val feature_chat_footer_hooks_description = cn.hxy.kiora.R.string.feature_chat_footer_hooks_description
        val noncompose_chat_input_actions_title = cn.hxy.kiora.R.string.noncompose_chat_input_actions_title
        val noncompose_chat_input_no_actions = cn.hxy.kiora.R.string.noncompose_chat_input_no_actions
        val feature_decompile_bean_shell_snapshot_name = cn.hxy.kiora.R.string.feature_decompile_bean_shell_snapshot_name
        val feature_decompile_bean_shell_snapshot_description = cn.hxy.kiora.R.string.feature_decompile_bean_shell_snapshot_description
        val noncompose_bsh_saved = cn.hxy.kiora.R.string.noncompose_bsh_saved
        val noncompose_bsh_empty_result = cn.hxy.kiora.R.string.noncompose_bsh_empty_result
        val noncompose_bsh_error = cn.hxy.kiora.R.string.noncompose_bsh_error
        val noncompose_bsh_selection_cancelled = cn.hxy.kiora.R.string.noncompose_bsh_selection_cancelled
        // moments / RepostMoments
        val feature_repost_moments_name = cn.hxy.kiora.R.string.feature_repost_moments_name
        val feature_repost_moments_description = cn.hxy.kiora.R.string.feature_repost_moments_description
        val feature_we_moments_api_name = cn.hxy.kiora.R.string.feature_we_moments_api_name
        val feature_we_moments_api_description = cn.hxy.kiora.R.string.feature_we_moments_api_description
        val feature_we_moments_context_menu_api_name = cn.hxy.kiora.R.string.feature_we_moments_context_menu_api_name
        val feature_we_moments_context_menu_api_description = cn.hxy.kiora.R.string.feature_we_moments_context_menu_api_description
        val moments_repost_menu = cn.hxy.kiora.R.string.moments_repost_menu
        val moments_quick_repost_menu = cn.hxy.kiora.R.string.moments_quick_repost_menu
        val moments_repost_parse_failed = cn.hxy.kiora.R.string.moments_repost_parse_failed
        val moments_repost_preparing_images = cn.hxy.kiora.R.string.moments_repost_preparing_images
        val moments_repost_image_download_failed = cn.hxy.kiora.R.string.moments_repost_image_download_failed
        val moments_repost_preparing_video = cn.hxy.kiora.R.string.moments_repost_preparing_video
        val moments_repost_video_download_failed = cn.hxy.kiora.R.string.moments_repost_video_download_failed
        val moments_repost_video_save_failed = cn.hxy.kiora.R.string.moments_repost_video_save_failed
        val moments_repost_video_select_failed = cn.hxy.kiora.R.string.moments_repost_video_select_failed
        val moments_repost_card_unsupported = cn.hxy.kiora.R.string.moments_repost_card_unsupported
        val moments_repost_preparing_live_photo = cn.hxy.kiora.R.string.moments_repost_preparing_live_photo
        val moments_quick_repost_preparing = cn.hxy.kiora.R.string.moments_quick_repost_preparing
        val moments_repost_no_live_photo = cn.hxy.kiora.R.string.moments_repost_no_live_photo
        val moments_repost_live_photo_video_download_failed = cn.hxy.kiora.R.string.moments_repost_live_photo_video_download_failed
        val moments_repost_image_cache_missing = cn.hxy.kiora.R.string.moments_repost_image_cache_missing
        val moments_repost_video_cache_missing = cn.hxy.kiora.R.string.moments_repost_video_cache_missing
        val moments_repost_live_photo_cache_missing = cn.hxy.kiora.R.string.moments_repost_live_photo_cache_missing
        val moments_repost_live_photo_save_failed = cn.hxy.kiora.R.string.moments_repost_live_photo_save_failed
        val moments_repost_editor_opened = cn.hxy.kiora.R.string.moments_repost_editor_opened
        val moments_repost_live_photo_select_failed = cn.hxy.kiora.R.string.moments_repost_live_photo_select_failed
        val moments_repost_live_photo_editor_failed = cn.hxy.kiora.R.string.moments_repost_live_photo_editor_failed
        val moments_repost_card_parse_failed = cn.hxy.kiora.R.string.moments_repost_card_parse_failed
        val moments_repost_card_clone_failed = cn.hxy.kiora.R.string.moments_repost_card_clone_failed
        val moments_repost_container_unavailable = cn.hxy.kiora.R.string.moments_repost_container_unavailable
        val moments_repost_queued = cn.hxy.kiora.R.string.moments_repost_queued
        val moments_repost_queued_static_live_photos = cn.hxy.kiora.R.string.moments_repost_queued_static_live_photos
        val moments_repost_failed = cn.hxy.kiora.R.string.moments_repost_failed
        val noncompose_moments_thumbnail_warning = cn.hxy.kiora.R.string.noncompose_moments_thumbnail_warning

        // Compose items batch 1: FakeVoiceDuration
        val dialog_cancel = cn.hxy.kiora.R.string.dialog_cancel
        val dialog_confirm = cn.hxy.kiora.R.string.dialog_confirm
        val feature_fake_voice_duration_name = cn.hxy.kiora.R.string.feature_fake_voice_duration_name
        val feature_fake_voice_duration_description = cn.hxy.kiora.R.string.feature_fake_voice_duration_description
        val chat_fake_voice_duration_millis = cn.hxy.kiora.R.string.chat_fake_voice_duration_millis
        val chat_fake_voice_duration_invalid = cn.hxy.kiora.R.string.chat_fake_voice_duration_invalid

        // Compose items batch 2: nicknames/DPI/tablet
        val feature_limit_group_member_nickname_length_name = cn.hxy.kiora.R.string.feature_limit_group_member_nickname_length_name
        val feature_limit_group_member_nickname_length_description = cn.hxy.kiora.R.string.feature_limit_group_member_nickname_length_description
        val contacts_nickname_max_characters = cn.hxy.kiora.R.string.contacts_nickname_max_characters
        val contacts_invalid_number = cn.hxy.kiora.R.string.contacts_invalid_number
        val feature_custom_dpi_name = cn.hxy.kiora.R.string.feature_custom_dpi_name
        val feature_custom_dpi_description = cn.hxy.kiora.R.string.feature_custom_dpi_description
        val system_custom_dpi_width = cn.hxy.kiora.R.string.system_custom_dpi_width
        val system_invalid_number = cn.hxy.kiora.R.string.system_invalid_number
        val feature_force_tablet_mode_name = cn.hxy.kiora.R.string.feature_force_tablet_mode_name
        val feature_force_tablet_mode_description = cn.hxy.kiora.R.string.feature_force_tablet_mode_description
        val system_risky_feature_warning = cn.hxy.kiora.R.string.system_risky_feature_warning
        val warning = cn.hxy.kiora.R.string.warning

        // Compose items batch 3: debug SendPacket / TriggerCrash
        val action_close = cn.hxy.kiora.R.string.action_close
        val action_back = cn.hxy.kiora.R.string.action_back
        val feature_send_packet_name = cn.hxy.kiora.R.string.feature_send_packet_name
        val feature_send_packet_description = cn.hxy.kiora.R.string.feature_send_packet_description
        val debug_send_packet_title = cn.hxy.kiora.R.string.debug_send_packet_title
        val debug_send_packet_cgi_path = cn.hxy.kiora.R.string.debug_send_packet_cgi_path
        val debug_send_packet_cmd_id = cn.hxy.kiora.R.string.debug_send_packet_cmd_id
        val debug_send_packet_func_id = cn.hxy.kiora.R.string.debug_send_packet_func_id
        val debug_send_packet_route_id = cn.hxy.kiora.R.string.debug_send_packet_route_id
        val debug_send_packet_json_payload = cn.hxy.kiora.R.string.debug_send_packet_json_payload
        val debug_send_packet_uri_required = cn.hxy.kiora.R.string.debug_send_packet_uri_required
        val debug_send_packet_integer_ids_required = cn.hxy.kiora.R.string.debug_send_packet_integer_ids_required
        val debug_send_packet_success_title = cn.hxy.kiora.R.string.debug_send_packet_success_title
        val debug_send_packet_success_result = cn.hxy.kiora.R.string.debug_send_packet_success_result
        val debug_send_packet_failure_title = cn.hxy.kiora.R.string.debug_send_packet_failure_title
        val debug_send_packet_failure_result = cn.hxy.kiora.R.string.debug_send_packet_failure_result
        val feature_trigger_crash_name = cn.hxy.kiora.R.string.feature_trigger_crash_name
        val feature_trigger_crash_description = cn.hxy.kiora.R.string.feature_trigger_crash_description
        val debug_trigger_crash_category_java = cn.hxy.kiora.R.string.debug_trigger_crash_category_java
        val debug_trigger_crash_category_native = cn.hxy.kiora.R.string.debug_trigger_crash_category_native
        val debug_trigger_crash_select_category = cn.hxy.kiora.R.string.debug_trigger_crash_select_category
        val debug_trigger_crash_java_null_pointer = cn.hxy.kiora.R.string.debug_trigger_crash_java_null_pointer
        val debug_trigger_crash_java_array_bounds = cn.hxy.kiora.R.string.debug_trigger_crash_java_array_bounds
        val debug_trigger_crash_java_class_cast = cn.hxy.kiora.R.string.debug_trigger_crash_java_class_cast
        val debug_trigger_crash_java_arithmetic = cn.hxy.kiora.R.string.debug_trigger_crash_java_arithmetic
        val debug_trigger_crash_java_stack_overflow = cn.hxy.kiora.R.string.debug_trigger_crash_java_stack_overflow
        val debug_trigger_crash_select_java_type = cn.hxy.kiora.R.string.debug_trigger_crash_select_java_type
        val debug_trigger_crash_native_sigsegv = cn.hxy.kiora.R.string.debug_trigger_crash_native_sigsegv
        val debug_trigger_crash_native_sigabrt = cn.hxy.kiora.R.string.debug_trigger_crash_native_sigabrt
        val debug_trigger_crash_native_sigfpe = cn.hxy.kiora.R.string.debug_trigger_crash_native_sigfpe
        val debug_trigger_crash_native_sigill = cn.hxy.kiora.R.string.debug_trigger_crash_native_sigill
        val debug_trigger_crash_native_sigbus = cn.hxy.kiora.R.string.debug_trigger_crash_native_sigbus
        val debug_trigger_crash_select_native_type = cn.hxy.kiora.R.string.debug_trigger_crash_select_native_type
        val debug_trigger_crash_confirmation_title = cn.hxy.kiora.R.string.debug_trigger_crash_confirmation_title
        val debug_trigger_crash_confirmation_message = cn.hxy.kiora.R.string.debug_trigger_crash_confirmation_message

        // Compose items batch 4: CustomSourceApp / ModifyTransferWalletBalanceDisplay
        val action_save = cn.hxy.kiora.R.string.action_save
        val feature_custom_source_app_name = cn.hxy.kiora.R.string.feature_custom_source_app_name
        val feature_custom_source_app_description = cn.hxy.kiora.R.string.feature_custom_source_app_description
        val moments_custom_source_title = cn.hxy.kiora.R.string.moments_custom_source_title
        val moments_custom_source_app_id = cn.hxy.kiora.R.string.moments_custom_source_app_id
        val moments_custom_source_app_name = cn.hxy.kiora.R.string.moments_custom_source_app_name
        val moments_custom_source_choose_preset = cn.hxy.kiora.R.string.moments_custom_source_choose_preset
        val moments_custom_source_preset_title = cn.hxy.kiora.R.string.moments_custom_source_preset_title
        val moments_custom_source_search = cn.hxy.kiora.R.string.moments_custom_source_search
        val feature_modify_transfer_wallet_balance_display_name = cn.hxy.kiora.R.string.feature_modify_transfer_wallet_balance_display_name
        val feature_modify_transfer_wallet_balance_display_description = cn.hxy.kiora.R.string.feature_modify_transfer_wallet_balance_display_description
        val payment_wallet_balance_optional = cn.hxy.kiora.R.string.payment_wallet_balance_optional
        val payment_wealth_balance_optional = cn.hxy.kiora.R.string.payment_wealth_balance_optional
        val payment_transfer_wallet_balance = cn.hxy.kiora.R.string.payment_transfer_wallet_balance
        val payment_transfer_wealth_balance = cn.hxy.kiora.R.string.payment_transfer_wealth_balance

        // Compose items batch 5: SetProfileNickname / ClearProfileDetails / LaunchInternalUrls
        val feature_set_profile_nickname_name = cn.hxy.kiora.R.string.feature_set_profile_nickname_name
        val feature_set_profile_nickname_description = cn.hxy.kiora.R.string.feature_set_profile_nickname_description
        val profile_new_nickname = cn.hxy.kiora.R.string.profile_new_nickname
        val profile_nickname_success = cn.hxy.kiora.R.string.profile_nickname_success
        val profile_nickname_server_code = cn.hxy.kiora.R.string.profile_nickname_server_code
        val profile_nickname_failure = cn.hxy.kiora.R.string.profile_nickname_failure
        val profile_nickname_failure_details = cn.hxy.kiora.R.string.profile_nickname_failure_details
        val feature_clear_profile_details_name = cn.hxy.kiora.R.string.feature_clear_profile_details_name
        val feature_clear_profile_details_description = cn.hxy.kiora.R.string.feature_clear_profile_details_description
        val clear_profile_details_confirmation = cn.hxy.kiora.R.string.clear_profile_details_confirmation
        val clear_profile_details_send_success = cn.hxy.kiora.R.string.clear_profile_details_send_success
        val clear_profile_details_server_response_code = cn.hxy.kiora.R.string.clear_profile_details_server_response_code
        val clear_profile_details_send_failure = cn.hxy.kiora.R.string.clear_profile_details_send_failure
        val clear_profile_details_send_failure_details = cn.hxy.kiora.R.string.clear_profile_details_send_failure_details
        val feature_launch_internal_urls_name = cn.hxy.kiora.R.string.feature_launch_internal_urls_name
        val feature_launch_internal_urls_description = cn.hxy.kiora.R.string.feature_launch_internal_urls_description
        val debug_launch_internal_url_arguments = cn.hxy.kiora.R.string.debug_launch_internal_url_arguments
        val debug_launch_internal_url_title = cn.hxy.kiora.R.string.debug_launch_internal_url_title
        val debug_launch_internal_url_url = cn.hxy.kiora.R.string.debug_launch_internal_url_url

        val dialog_close = cn.hxy.kiora.R.string.dialog_close
        val unknown = cn.hxy.kiora.R.string.unknown

        // Compose items batch 6: m3 base + AutoApproveDeviceLogin / BlockVoipRingtone / HideMeTabPageItems
        val feature_auto_approve_device_login_name = cn.hxy.kiora.R.string.feature_auto_approve_device_login_name
        val feature_auto_approve_device_login_description = cn.hxy.kiora.R.string.feature_auto_approve_device_login_description
        val system_auto_approve_auto_login = cn.hxy.kiora.R.string.system_auto_approve_auto_login
        val system_auto_approve_auto_login_summary = cn.hxy.kiora.R.string.system_auto_approve_auto_login_summary
        val system_auto_approve_sync = cn.hxy.kiora.R.string.system_auto_approve_sync
        val system_auto_approve_sync_summary = cn.hxy.kiora.R.string.system_auto_approve_sync_summary
        val feature_block_voip_ringtone_name = cn.hxy.kiora.R.string.feature_block_voip_ringtone_name
        val feature_block_voip_ringtone_description = cn.hxy.kiora.R.string.feature_block_voip_ringtone_description
        val voip_block_outgoing = cn.hxy.kiora.R.string.voip_block_outgoing
        val voip_block_outgoing_summary = cn.hxy.kiora.R.string.voip_block_outgoing_summary
        val voip_block_incoming = cn.hxy.kiora.R.string.voip_block_incoming
        val voip_block_incoming_summary = cn.hxy.kiora.R.string.voip_block_incoming_summary
        val feature_hide_me_tab_page_items_name = cn.hxy.kiora.R.string.feature_hide_me_tab_page_items_name
        val feature_hide_me_tab_page_items_description = cn.hxy.kiora.R.string.feature_hide_me_tab_page_items_description
        val beautify_me_page_title = cn.hxy.kiora.R.string.beautify_me_page_title
        val beautify_me_page_hide_moments = cn.hxy.kiora.R.string.beautify_me_page_hide_moments
        val beautify_me_page_hide_works = cn.hxy.kiora.R.string.beautify_me_page_hide_works
        val beautify_me_page_hide_works_summary = cn.hxy.kiora.R.string.beautify_me_page_hide_works_summary
        val beautify_me_page_hide_cards = cn.hxy.kiora.R.string.beautify_me_page_hide_cards
        val beautify_me_page_hide_cards_summary = cn.hxy.kiora.R.string.beautify_me_page_hide_cards_summary
        val beautify_me_page_hide_stickers = cn.hxy.kiora.R.string.beautify_me_page_hide_stickers

        // Compose items batch 7: ImageRotation + BaseItemContainer/IntNumberPickerWidget
        val feature_image_rotation_name = cn.hxy.kiora.R.string.feature_image_rotation_name
        val feature_image_rotation_description = cn.hxy.kiora.R.string.feature_image_rotation_description
        val image_rotation_only_avatars = cn.hxy.kiora.R.string.image_rotation_only_avatars
        val image_rotation_period_milliseconds = cn.hxy.kiora.R.string.image_rotation_period_milliseconds

        // Compose items batch 8: ApplyDialogBackgroundBlur
        val feature_apply_dialog_background_blur_name = cn.hxy.kiora.R.string.feature_apply_dialog_background_blur_name
        val feature_apply_dialog_background_blur_description = cn.hxy.kiora.R.string.feature_apply_dialog_background_blur_description
        val beautify_dialog_blur_title = cn.hxy.kiora.R.string.beautify_dialog_blur_title
        val beautify_dialog_blur_unsupported_hint = cn.hxy.kiora.R.string.beautify_dialog_blur_unsupported_hint
        val beautify_dialog_blur_radius = cn.hxy.kiora.R.string.beautify_dialog_blur_radius

        // Compose items batch 9: ModifySportsStepCount + DecimalExpression/BaseSupportingWidget
        val feature_modify_sports_step_count_name = cn.hxy.kiora.R.string.feature_modify_sports_step_count_name
        val feature_modify_sports_step_count_description = cn.hxy.kiora.R.string.feature_modify_sports_step_count_description
        val system_sports_active_value = cn.hxy.kiora.R.string.system_sports_active_value
        val system_sports_fixed = cn.hxy.kiora.R.string.system_sports_fixed
        val system_sports_multiplier = cn.hxy.kiora.R.string.system_sports_multiplier
        val system_sports_passive_mode = cn.hxy.kiora.R.string.system_sports_passive_mode
        val system_sports_passive_value = cn.hxy.kiora.R.string.system_sports_passive_value
        val system_sports_passive_expression_hint = cn.hxy.kiora.R.string.system_sports_passive_expression_hint
        val system_sports_upload = cn.hxy.kiora.R.string.system_sports_upload
        val system_sports_upload_result = cn.hxy.kiora.R.string.system_sports_upload_result
        val system_success = cn.hxy.kiora.R.string.system_success
        val system_failure = cn.hxy.kiora.R.string.system_failure

        val system_invalid_format = cn.hxy.kiora.R.string.system_invalid_format

        // Compose items batch 10: ModifyWalletBalanceDisplay
        val feature_modify_wallet_balance_display_name = cn.hxy.kiora.R.string.feature_modify_wallet_balance_display_name
        val feature_modify_wallet_balance_display_description = cn.hxy.kiora.R.string.feature_modify_wallet_balance_display_description
        val payment_wallet_balance_expression_hint = cn.hxy.kiora.R.string.payment_wallet_balance_expression_hint
        val payment_wallet_balance_title = cn.hxy.kiora.R.string.payment_wallet_balance_title
        val payment_wallet_balance_expression = cn.hxy.kiora.R.string.payment_wallet_balance_expression
        val payment_wealth_balance_title = cn.hxy.kiora.R.string.payment_wealth_balance_title
        val payment_business_balance_title = cn.hxy.kiora.R.string.payment_business_balance_title

        // Compose items batch 11: HideMessagesAvatars / ModifyFriendsCount
        val feature_hide_messages_avatars_name = cn.hxy.kiora.R.string.feature_hide_messages_avatars_name
        val feature_hide_messages_avatars_description = cn.hxy.kiora.R.string.feature_hide_messages_avatars_description
        val chat_hide_avatar_incoming = cn.hxy.kiora.R.string.chat_hide_avatar_incoming
        val chat_hide_avatar_incoming_description = cn.hxy.kiora.R.string.chat_hide_avatar_incoming_description
        val chat_hide_avatar_outgoing = cn.hxy.kiora.R.string.chat_hide_avatar_outgoing
        val chat_hide_avatar_outgoing_description = cn.hxy.kiora.R.string.chat_hide_avatar_outgoing_description
        val feature_modify_friends_count_name = cn.hxy.kiora.R.string.feature_modify_friends_count_name
        val feature_modify_friends_count_description = cn.hxy.kiora.R.string.feature_modify_friends_count_description
        val contacts_modify_count_hide = cn.hxy.kiora.R.string.contacts_modify_count_hide
        val contacts_modify_count_display = cn.hxy.kiora.R.string.contacts_modify_count_display

        // Compose items batch 12: UseLegacyOfficialAccountsView / PreventXposedDetection
        val error = cn.hxy.kiora.R.string.error
        val feature_use_legacy_official_accounts_view_name =
            cn.hxy.kiora.R.string.feature_use_legacy_official_accounts_view_name
        val feature_use_legacy_official_accounts_view_description =
            cn.hxy.kiora.R.string.feature_use_legacy_official_accounts_view_description
        val official_accounts_legacy_ui_missing =
            cn.hxy.kiora.R.string.official_accounts_legacy_ui_missing
        val feature_prevent_xposed_detection_name =
            cn.hxy.kiora.R.string.feature_prevent_xposed_detection_name
        val feature_prevent_xposed_detection_description =
            cn.hxy.kiora.R.string.feature_prevent_xposed_detection_description
        val system_prevent_xposed_google_play_warning =
            cn.hxy.kiora.R.string.system_prevent_xposed_google_play_warning

        // Compose items batch 13: WeChatMessageContextMenuApi
        val feature_we_chat_message_context_menu_api_name =
            cn.hxy.kiora.R.string.feature_we_chat_message_context_menu_api_name
        val feature_we_chat_message_context_menu_api_description =
            cn.hxy.kiora.R.string.feature_we_chat_message_context_menu_api_description
        val noncompose_message_menu_adapted_section =
            cn.hxy.kiora.R.string.noncompose_message_menu_adapted_section
        val noncompose_message_menu_automatic_section =
            cn.hxy.kiora.R.string.noncompose_message_menu_automatic_section
        val noncompose_message_menu_no_actions =
            cn.hxy.kiora.R.string.noncompose_message_menu_no_actions
    }

    object plurals {
        val debug_send_packet_byte_count = cn.hxy.kiora.R.plurals.debug_send_packet_byte_count
        val noncompose_message_menu_selected_title =
            cn.hxy.kiora.R.plurals.noncompose_message_menu_selected_title
        val chat_sticker_manager_exported = cn.hxy.kiora.R.plurals.chat_sticker_manager_exported
    }

    object raw {
        val eruda = cn.hxy.kiora.R.raw.eruda

    }

    object id {
        val wekit_multi_select_button = cn.hxy.kiora.R.id.wekit_multi_select_button
    }
}
