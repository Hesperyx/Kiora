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
    }
}
