package com.gaoshiqi.otakumap.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaoshiqi.otakumap.data.api.BangumiClient
import com.gaoshiqi.otakumap.data.bean.SearchFilter
import com.gaoshiqi.otakumap.data.bean.SearchRequest
import com.gaoshiqi.otakumap.data.bean.SearchSubject
import kotlinx.coroutines.launch

/**
 * 新版搜索测试 ViewModel
 * 使用 /v0/search/subjects 接口
 */
class SearchTestViewModel : ViewModel() {

    private val _state = MutableLiveData<SearchTestState>(SearchTestState.Idle)
    val state = _state

    private var query: String? = null
    private var offset = 0
    private var limit = 10
    private var total = 0

    private var mList: List<SearchSubject> = emptyList()

    fun search(q: String?) {
        offset = 0
        query = q
        mList = emptyList()
        if (query.isNullOrEmpty()) {
            _state.value = SearchTestState.Idle
            return
        }
        viewModelScope.launch {
            _state.value = SearchTestState.Loading
            try {
                val result = BangumiClient.instance.searchSubjectsV2(
                    limit = limit,
                    offset = offset,
                    request = SearchRequest(
                        keyword = query!!,
                        filter = SearchFilter(type = listOf(2))
                    )
                )
                total = result.total ?: 0
                mList = result.data ?: emptyList()
                _state.value = SearchTestState.Success(mList, total)
            } catch (e: Exception) {
                _state.value = SearchTestState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun loadMore() {
        // 已加载的数量 >= 总数，没有更多数据
        if (offset + limit >= total || query.isNullOrEmpty()) {
            return
        }
        val nextOffset = offset + limit
        viewModelScope.launch {
            _state.value = SearchTestState.LoadMore
            try {
                val result = BangumiClient.instance.searchSubjectsV2(
                    limit = limit,
                    offset = nextOffset,
                    request = SearchRequest(
                        keyword = query!!,
                        filter = SearchFilter(type = listOf(2))
                    )
                )
                offset = nextOffset
                total = result.total ?: total
                result.data?.let { mList = mList + it }
                _state.value = SearchTestState.Success(mList, total)
            } catch (e: Exception) {
                _state.value = SearchTestState.LoadMoreError(e.message ?: "Load failed")
            }
        }
    }

    fun hasMore(): Boolean = offset + limit < total
}

sealed class SearchTestState {
    data object Idle : SearchTestState()
    data object Loading : SearchTestState()
    data class Success(val data: List<SearchSubject>, val total: Int) : SearchTestState()
    data class Error(val message: String) : SearchTestState()
    data object LoadMore : SearchTestState()
    data class LoadMoreError(val message: String) : SearchTestState()
}
