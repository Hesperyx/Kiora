package dev.ujhhgtg.wekit.features.core

/**
 * 功能注册表自检。与 WeKit 原版逐字一致。
 *
 * 在注册表初始化时调用，任一失败即抛异常 —— 让「重复 ID / 空分类 / 未知分类」
 * 在启动瞬间暴露，而不是等到某个功能静默失效。
 */
fun validateFeatures(features: List<BaseFeature>): List<BaseFeature> {
    features.forEach { feature ->
        require(feature.technicalId.isNotEmpty()) {
            "Feature ${feature.javaClass.name} has an empty technical ID"
        }
        require(feature.categoryIds.isNotEmpty()) {
            "Feature ${feature.javaClass.name} has no categories"
        }
        val unknownCategories = feature.categoryIds.filterNot(FeatureCategoryIds.ALL::contains)
        require(unknownCategories.isEmpty()) {
            "Feature ${feature.javaClass.name} has unknown categories: $unknownCategories"
        }
    }
    val duplicates = features.groupBy(BaseFeature::technicalId)
        .filterValues { it.size > 1 }
    require(duplicates.isEmpty()) {
        duplicates.entries.joinToString(", ") { (technicalId, matchingFeatures) ->
            "Duplicate Feature technical ID '$technicalId': " +
                matchingFeatures.joinToString { it.javaClass.name }
        }
    }
    return features
}
