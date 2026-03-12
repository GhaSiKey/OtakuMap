package com.gaoshiqi.otakumap.data.bean

import com.google.gson.annotations.SerializedName

/**
 * 新版搜索接口 /v0/search/subjects 的数据模型
 * Created by gaoshiqi
 */

// ==================== 请求相关 ====================

/**
 * 搜索请求体
 * @param keyword 搜索关键词（必填）
 * @param sort 排序方式：match(匹配度), heat(热度), rank(排名), score(评分)
 * @param filter 筛选条件
 */
data class SearchRequest(
    val keyword: String,
    val sort: String? = null,
    val filter: SearchFilter? = null
)

/**
 * 搜索筛选条件
 */
data class SearchFilter(
    val type: List<Int>? = null,
    val tag: List<String>? = null,
    @SerializedName("air_date") val airDate: List<String>? = null,
    val rating: List<String>? = null,
    val rank: List<String>? = null,
    @SerializedName("meta_tags") val metaTags: List<String>? = null,
    @SerializedName("rating_count") val ratingCount: List<String>? = null,
    val nsfw: Boolean? = null
)

// ==================== 响应相关 ====================

/**
 * 搜索响应
 */
data class SearchSubjectsResponse(
    val data: List<SearchSubject>? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null
)

/**
 * 搜索结果条目
 */
data class SearchSubject(
    val id: Int? = null,
    val name: String? = null,
    @SerializedName("name_cn") val nameCn: String? = null,
    val type: Int? = null,
    val date: String? = null,
    val platform: String? = null,
    val image: String? = null,
    val images: SearchImages? = null,
    val summary: String? = null,
    val rating: SearchRating? = null,
    val rank: Int? = null,
    val collection: SearchCollection? = null,
    val tags: List<SearchTag>? = null,
    val eps: Int? = null,
    val volumes: Int? = null,
    val nsfw: Boolean? = null
)

data class SearchImages(
    val large: String? = null,
    val common: String? = null,
    val medium: String? = null,
    val small: String? = null,
    val grid: String? = null
)

data class SearchRating(
    val rank: Int? = null,
    val total: Int? = null,
    val count: Map<String, Int>? = null,
    val score: Double? = null
)

data class SearchCollection(
    val wish: Int? = null,
    val collect: Int? = null,
    val doing: Int? = null,
    @SerializedName("on_hold") val onHold: Int? = null,
    val dropped: Int? = null
)

data class SearchTag(
    val name: String? = null,
    val count: Int? = null
)
