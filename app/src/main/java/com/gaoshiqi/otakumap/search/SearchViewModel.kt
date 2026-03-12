package com.gaoshiqi.otakumap.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.data.api.BangumiClient
import com.gaoshiqi.otakumap.data.bean.SearchRequest
import com.gaoshiqi.otakumap.data.bean.SearchSubject
import com.gaoshiqi.otakumap.search.filter.ActiveFilterChip
import com.gaoshiqi.otakumap.search.filter.ChipType
import com.gaoshiqi.otakumap.search.filter.NsfwOption
import com.gaoshiqi.otakumap.search.filter.RangeCondition
import com.gaoshiqi.otakumap.search.filter.SearchFilterState
import com.gaoshiqi.otakumap.search.filter.SortOption
import com.gaoshiqi.otakumap.search.filter.SubjectType
import com.gaoshiqi.otakumap.BangumiApplication
import com.gaoshiqi.room.RecentViewEntity
import com.gaoshiqi.room.SearchHistoryEntity
import com.gaoshiqi.room.SearchHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Created by gaoshiqi
 * on 2025/7/31 17:40
 * email: gaoshiqi@bilibili.com
 */
class SearchViewModel(application: Application): AndroidViewModel(application) {
    private val _state = MutableLiveData<SearchState>(SearchState.Idle)
    val state = _state

    private val searchHistoryRepository = SearchHistoryRepository(application)
    val searchHistory: LiveData<List<SearchHistoryEntity>> =
        searchHistoryRepository.allHistory.asLiveData()

    private val recentViewRepository = (application as BangumiApplication).recentViewRepository
    val recentViews: LiveData<List<RecentViewEntity>> =
        recentViewRepository.allRecentViews.asLiveData()

    private val _filterState = MutableStateFlow(SearchFilterState.DEFAULT)
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    val activeFilterChips: LiveData<List<ActiveFilterChip>> =
        _filterState.map { it.getActiveFilterChips() }.asLiveData()

    private var query: String? = null
    private var offset = 0
    private var limit = 10
    private var total = 0

    private var mList: List<SearchSubject> = emptyList()

    init {
        _state.value = SearchState.Idle
        query = null
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.Search -> search(intent.query)
            is SearchIntent.LoadMore -> loadMore()
            is SearchIntent.ClearAllHistory -> clearAllHistory()
            is SearchIntent.DeleteHistory -> deleteHistory(intent.keyword)
            is SearchIntent.UpdateFilter -> updateFilter(intent.state)
            is SearchIntent.ResetFilter -> resetFilter()
            is SearchIntent.RemoveFilterChip -> removeFilterChip(intent.chip)
        }
    }

    private fun clearAllHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearAllHistory()
        }
    }

    private fun deleteHistory(keyword: String) {
        viewModelScope.launch {
            searchHistoryRepository.deleteHistory(keyword)
        }
    }

    private fun saveHistory(keyword: String) {
        viewModelScope.launch {
            searchHistoryRepository.addHistory(keyword)
        }
    }

    /**
     * 是否可以执行搜索：有关键词，或者有非默认筛选条件
     */
    private fun canSearch(): Boolean {
        return !query.isNullOrBlank() || _filterState.value.hasActiveFilters()
    }

    private fun updateFilter(newState: SearchFilterState) {
        _filterState.value = newState
        if (canSearch()) {
            search(query)
        }
    }

    private fun resetFilter() {
        _filterState.value = SearchFilterState.DEFAULT
        if (canSearch()) {
            search(query)
        }
    }

    private fun removeFilterChip(chip: ActiveFilterChip) {
        val current = _filterState.value
        val newState = when (chip.type) {
            ChipType.SORT -> current.copy(sort = SortOption.MATCH)
            ChipType.TYPE -> {
                val newTypes = current.types - SubjectType.fromTypeId(chip.typeId)!!
                current.copy(types = newTypes.ifEmpty { setOf(SubjectType.ANIME) })
            }
            ChipType.TAG -> current.copy(tags = current.tags - chip.tagValue)
            ChipType.AIR_DATE -> current.copy(airDateStart = "", airDateEnd = "")
            ChipType.RATING -> current.copy(rating = RangeCondition())
            ChipType.RATING_COUNT -> current.copy(ratingCount = RangeCondition())
            ChipType.RANK -> current.copy(rank = RangeCondition())
            ChipType.NSFW -> current.copy(nsfw = NsfwOption.NO_FILTER)
        }
        _filterState.value = newState
        if (canSearch()) {
            search(query)
        }
    }

    private fun search(q: String?) {
        offset = 0
        query = q
        mList = emptyList()
        // 无关键词且无筛选条件时不执行搜索
        if (!canSearch()) return
        val keyword = query?.trim() ?: ""
        viewModelScope.launch {
            _state.value = SearchState.Loading
            try {
                val filterState = _filterState.value
                val result = BangumiClient.instance.searchSubjectsV2(
                    limit = limit,
                    offset = offset,
                    request = SearchRequest(
                        keyword = keyword,
                        sort = filterState.sort.apiValue,
                        filter = filterState.toSearchFilter()
                    )
                )
                total = result.total ?: 0
                mList = result.data ?: emptyList()
                _state.value = SearchState.Success(mList)
                if (keyword.isNotBlank()) {
                    saveHistory(keyword)
                }
            } catch (e: Exception) {
                _state.value = SearchState.Error(e.message ?: getApplication<Application>().getString(R.string.search_failed))
            }
        }
    }

    private fun loadMore() {
        if (offset + limit >= total || !canSearch()) {
            return
        }
        val keyword = query?.trim() ?: ""
        val nextOffset = offset + limit
        viewModelScope.launch {
            _state.value = SearchState.LoadMore
            try {
                val filterState = _filterState.value
                val result = BangumiClient.instance.searchSubjectsV2(
                    limit = limit,
                    offset = nextOffset,
                    request = SearchRequest(
                        keyword = keyword,
                        sort = filterState.sort.apiValue,
                        filter = filterState.toSearchFilter()
                    )
                )
                offset = nextOffset
                total = result.total ?: total
                result.data?.let { mList = mList + it }

                _state.value = SearchState.Success(mList)
            } catch (e: Exception) {
                _state.value = SearchState.LoadMoreError(e.message ?: getApplication<Application>().getString(R.string.load_failed))
            }
        }
    }
}

sealed class SearchIntent {
    data class Search(val query: String?): SearchIntent()
    data object LoadMore: SearchIntent()
    data object ClearAllHistory: SearchIntent()
    data class DeleteHistory(val keyword: String): SearchIntent()
    data class UpdateFilter(val state: SearchFilterState): SearchIntent()
    data object ResetFilter: SearchIntent()
    data class RemoveFilterChip(val chip: ActiveFilterChip): SearchIntent()
}

sealed class SearchState {
    data object Idle: SearchState()
    data object Loading: SearchState()
    data class Success(val data: List<SearchSubject>): SearchState()
    data class Error(val message: String): SearchState()
    data object LoadMore: SearchState()
    data class LoadMoreError(val message: String): SearchState()
}
