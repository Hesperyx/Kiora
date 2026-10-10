package dev.ujhhgtg.wekit.features.items.system

import com.tencent.mm.ui.base.preference.Preference
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.dexkit.abc.IResolveDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.features.core.FeatureCategoryIds
import dev.ujhhgtg.wekit.features.core.SwitchFeature

object UseLegacyWalletViewInMePage : SwitchFeature(), IResolveDex {

    override val technicalId = "恢复旧版「我」界面卡包"
    override val nameRes = R.string.feature_use_legacy_wallet_view_in_me_page_name
    override val categoryIds = listOf(FeatureCategoryIds.SYSTEM_PRIVACY)
    override val descriptionRes = R.string.feature_use_legacy_wallet_view_in_me_page_description

    override fun onEnable() {
        installHook("UseLegacyWalletViewInMePage#1") {
            methodGetOrderAndCardEntranceInfo.hookAfter {
                // 被 hook 的方法在部分版本/账号形态下会返回 null；此时没有任何对象可改，
                // 直接放行。旧写法 `result!!` 会在钩子内抛 NPE（被 executeHookAction 吞掉后
                // 本功能静默失效），这里显式判空让「返回值确实存在」时才改。
                val value = result ?: return@hookAfter
                value.reflekt()
                    .firstField {
                        type = Int::class.java
                    }.set(1)
            }
        }

        installHook("UseLegacyWalletViewInMePage#2") {
            methodMoreTabUIHandlePrefOnClick.hookBefore {
                val field = Preference::class.reflekt()
                    .firstField { type = String::class }

                val pref = args[1] as Preference
                if (field.get(pref) as? String? == "settings_mm_cardpackage_new") {
                    field.set(pref, "settings_mm_cardpackage")
                }
            }
        }
    }

    private val methodGetOrderAndCardEntranceInfo by dexMethod {
        matcher {
            usingEqStrings("MicroMsg.EcsOrderService", "getOrderAndCardEntranceInfo use finder logic")
        }
    }

    private val methodMoreTabUIHandlePrefOnClick by dexMethod {
        matcher {
            usingEqStrings("MicroMsg.MoreTabUI", "account has not already!", "onPreferenceTreeClick")
        }
    }
}
