package com.gaoshiqi.otakumap.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaoshiqi.otakumap.data.api.BangumiClient
import com.gaoshiqi.otakumap.data.bean.SubjectSmall
import kotlinx.coroutines.launch

/**
 * 旧版搜索测试 ViewModel
 * 使用 /search/subject/{keywords} 接口
 */
class SearchOldTestViewModel : ViewModel() {

    private val _state = MutableLiveData<SearchOldTestState>(SearchOldTestState.Idle)
    val state = _state

    private var query: String? = null
    private var start = 0
    private var limit = 10
    private var total = 0

    private var mList: List<SubjectSmall> = emptyList()

    fun search(q: String?) {
        start = 0
        query = q
        mList = emptyList()
        if (query.isNullOrEmpty()) {
            _state.value = SearchOldTestState.Idle
            return
        }
        viewModelScope.launch {
            _state.value = SearchOldTestState.Loading
            try {
                val result = BangumiClient.instance.searchSubject(
                    keywords = query!!,
                    start = start,
                    limit = limit
                )
                total = result.results ?: 0
                mList = result.list ?: emptyList()
                _state.value = SearchOldTestState.Success(mList, total)
            } catch (e: Exception) {
                _state.value = SearchOldTestState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun loadMore() {
        // 已加载的数量 >= 总数，没有更多数据
        if (start + limit >= total || query.isNullOrEmpty()) {
            return
        }
        val nextStart = start + limit
        viewModelScope.launch {
            _state.value = SearchOldTestState.LoadMore
            try {
                val result = BangumiClient.instance.searchSubject(
                    keywords = query!!,
                    start = nextStart,
                    limit = limit
                )
                start = nextStart
                total = result.results ?: total
                result.list?.let { mList = mList + it }
                _state.value = SearchOldTestState.Success(mList, total)
            } catch (e: Exception) {
                _state.value = SearchOldTestState.LoadMoreError(e.message ?: "Load failed")
            }
        }
    }

    fun hasMore(): Boolean = start + limit < total
}

sealed class SearchOldTestState {
    data object Idle : SearchOldTestState()
    data object Loading : SearchOldTestState()
    data class Success(val data: List<SubjectSmall>, val total: Int) : SearchOldTestState()
    data class Error(val message: String) : SearchOldTestState()
    data object LoadMore : SearchOldTestState()
    data class LoadMoreError(val message: String) : SearchOldTestState()
}
