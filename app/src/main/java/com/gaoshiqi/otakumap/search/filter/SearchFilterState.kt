package com.gaoshiqi.otakumap.search.filter

import android.os.Parcelable
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.data.bean.SearchFilter
import kotlinx.parcelize.Parcelize

/**
 * 搜索排序方式
 * @param apiValue API 请求中使用的排序字段值
 * @param labelResId 界面显示的字符串资源 ID
 */
enum class SortOption(val apiValue: String, val labelResId: Int) {
    MATCH("match", R.string.filter_sort_match),
    HEAT("heat", R.string.filter_sort_heat),
    RANK("rank", R.string.filter_sort_rank),
    SCORE("score", R.string.filter_sort_score);
}

/**
 * 条目类型
 * @param typeId Bangumi API 中的类型 ID
 * @param labelResId 界面显示的字符串资源 ID
 */
enum class SubjectType(val typeId: Int, val labelResId: Int) {
    BOOK(1, R.string.subject_book),
    ANIME(2, R.string.subject_anime),
    MUSIC(3, R.string.subject_music),
    GAME(4, R.string.subject_game),
    REAL(6, R.string.subject_real);

    companion object {
        fun fromTypeId(typeId: Int): SubjectType? = entries.find { it.typeId == typeId }
    }
}

/**
 * NSFW 过滤选项
 */
enum class NsfwOption(val labelResId: Int) {
    NO_FILTER(R.string.filter_nsfw_no_filter),
    ONLY_NSFW(R.string.filter_nsfw_only),
    EXCLUDE(R.string.filter_nsfw_exclude);
}

/**
 * 范围条件（用于评分、排名、评分人数等范围查询）
 * 转换为 API 格式：">=min", "<=max"
 */
@Parcelize
data class RangeCondition(
    val min: String = "",
    val max: String = ""
) : Parcelable {

    fun isEmpty(): Boolean = min.isBlank() && max.isBlank()

    /**
     * 转换为 API 请求格式的字符串列表
     * 例如: min="6", max="8" → [">=6", "<=8"]
     */
    fun toApiConditions(): List<String>? {
        val conditions = mutableListOf<String>()
        if (min.isNotBlank()) conditions.add(">=$min")
        if (max.isNotBlank()) conditions.add("<=$max")
        return conditions.ifEmpty { null }
    }

    fun toDisplayText(label: String): String {
        return when {
            min.isNotBlank() && max.isNotBlank() -> "$label: $min ~ $max"
            min.isNotBlank() -> "$label: >= $min"
            max.isNotBlank() -> "$label: <= $max"
            else -> ""
        }
    }
}

/**
 * 搜索筛选完整状态
 * 使用 @Parcelize 支持通过 Bundle 在 Fragment 间传递
 */
@Parcelize
data class SearchFilterState(
    val sort: SortOption = SortOption.MATCH,
    val types: Set<SubjectType> = setOf(SubjectType.ANIME),
    val tags: List<String> = emptyList(),
    val airDateStart: String = "",
    val airDateEnd: String = "",
    val rating: RangeCondition = RangeCondition(),
    val rank: RangeCondition = RangeCondition(),
    val ratingCount: RangeCondition = RangeCondition(),
    val nsfw: NsfwOption = NsfwOption.NO_FILTER
) : Parcelable {

    /**
     * 是否有激活的非默认筛选条件
     */
    fun hasActiveFilters(): Boolean {
        return sort != SortOption.MATCH
                || types != setOf(SubjectType.ANIME)
                || tags.isNotEmpty()
                || airDateStart.isNotBlank()
                || airDateEnd.isNotBlank()
                || !rating.isEmpty()
                || !rank.isEmpty()
                || !ratingCount.isEmpty()
                || nsfw != NsfwOption.NO_FILTER
    }

    /**
     * 转换为 API 请求所需的 SearchFilter
     */
    fun toSearchFilter(): SearchFilter {
        val typeIds = types.map { it.typeId }.ifEmpty { null }

        val airDateConditions = mutableListOf<String>()
        if (airDateStart.isNotBlank()) airDateConditions.add(">=$airDateStart")
        if (airDateEnd.isNotBlank()) airDateConditions.add("<=$airDateEnd")

        val nsfwValue = when (nsfw) {
            NsfwOption.NO_FILTER -> null
            NsfwOption.ONLY_NSFW -> true
            NsfwOption.EXCLUDE -> false
        }

        return SearchFilter(
            type = typeIds,
            tag = tags.ifEmpty { null },
            airDate = airDateConditions.ifEmpty { null },
            rating = rating.toApiConditions(),
            rank = rank.toApiConditions(),
            ratingCount = ratingCount.toApiConditions(),
            nsfw = nsfwValue
        )
    }

    /**
     * 获取当前激活的筛选条件标签列表，用于搜索栏下方的标签展示
     */
    fun getActiveFilterChips(): List<ActiveFilterChip> {
        val chips = mutableListOf<ActiveFilterChip>()

        if (sort != SortOption.MATCH) {
            chips.add(ActiveFilterChip(ChipType.SORT, sort.labelResId))
        }

        if (types != setOf(SubjectType.ANIME)) {
            types.forEach { type ->
                chips.add(ActiveFilterChip(ChipType.TYPE, type.labelResId, typeId = type.typeId))
            }
        }

        tags.forEach { tag ->
            chips.add(ActiveFilterChip(ChipType.TAG, displayText = tag, tagValue = tag))
        }

        if (airDateStart.isNotBlank() || airDateEnd.isNotBlank()) {
            val text = when {
                airDateStart.isNotBlank() && airDateEnd.isNotBlank() -> "$airDateStart ~ $airDateEnd"
                airDateStart.isNotBlank() -> ">= $airDateStart"
                else -> "<= $airDateEnd"
            }
            chips.add(ActiveFilterChip(ChipType.AIR_DATE, displayText = text))
        }

        if (!rating.isEmpty()) {
            chips.add(ActiveFilterChip(ChipType.RATING, displayText = rating.toDisplayText("")))
        }

        if (!ratingCount.isEmpty()) {
            chips.add(ActiveFilterChip(ChipType.RATING_COUNT, displayText = ratingCount.toDisplayText("")))
        }

        if (!rank.isEmpty()) {
            chips.add(ActiveFilterChip(ChipType.RANK, displayText = rank.toDisplayText("")))
        }

        if (nsfw != NsfwOption.NO_FILTER) {
            chips.add(ActiveFilterChip(ChipType.NSFW, nsfw.labelResId))
        }

        return chips
    }

    companion object {
        val DEFAULT = SearchFilterState()
    }
}

/**
 * 激活筛选条件标签的类型
 */
enum class ChipType {
    SORT, TYPE, TAG, AIR_DATE, RATING, RATING_COUNT, RANK, NSFW
}

/**
 * 搜索栏下方展示的激活筛选条件标签
 */
@Parcelize
data class ActiveFilterChip(
    val type: ChipType,
    val labelResId: Int = 0,
    val displayText: String = "",
    val tagValue: String = "",
    val typeId: Int = 0
) : Parcelable
