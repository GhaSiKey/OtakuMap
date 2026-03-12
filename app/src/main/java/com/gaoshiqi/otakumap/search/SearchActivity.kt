package com.gaoshiqi.otakumap.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextWatcher
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.databinding.ActivitySearchBinding
import com.gaoshiqi.otakumap.databinding.LayoutSearchHistoryBinding
import com.gaoshiqi.otakumap.search.adapter.SearchResultAdapter
import com.gaoshiqi.otakumap.search.filter.ActiveFilterChip
import com.gaoshiqi.otakumap.search.filter.SearchFilterBottomSheet
import com.gaoshiqi.otakumap.search.filter.SearchFilterState
import com.gaoshiqi.otakumap.utils.KeyboardUtils
import com.gaoshiqi.otakumap.widget.SearchHistoryTagGroupView
import com.gaoshiqi.otakumap.widget.TagGroupView
import com.gaoshiqi.room.SearchHistoryEntity
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivitySearchBinding
    private lateinit var mHistoryBinding: LayoutSearchHistoryBinding
    private val mViewModel: SearchViewModel by viewModels()
    private val mAdapter = SearchResultAdapter()
    private var isLoadingMore = false
    private var hasSearchResult = false
    private var isEditMode = false

    companion object {
        private const val EXTRA_TAG = "extra_tag"

        /**
         * 携带标签跳转搜索页，自动以该标签为筛选条件执行搜索
         */
        fun startWithTag(context: Context, tag: String) {
            val intent = Intent(context, SearchActivity::class.java).apply {
                putExtra(EXTRA_TAG, tag)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
        initObserver()
        setupBackPressHandler()
        setupFilterResult()
        handleTagIntent()
    }

    private fun initView() {
        mBinding.searchBar.requestFocus()
        mBinding.rvSearchResult.adapter = mAdapter
        mBinding.rvSearchResult.layoutManager = LinearLayoutManager(this)

        setupSearchBar()
        setupHistoryView()
        setupFilterButton()

        Handler(Looper.getMainLooper()).postDelayed({
            KeyboardUtils.showSoftKeyboard(this, mBinding.searchBar)
        }, 300)
    }

    private fun setupFilterButton() {
        mBinding.ivFilter.setOnClickListener {
            KeyboardUtils.hideSoftKeyboard(this, mBinding.searchBar)
            val bottomSheet = SearchFilterBottomSheet.newInstance(mViewModel.filterState.value)
            bottomSheet.show(supportFragmentManager, SearchFilterBottomSheet.TAG)
        }
    }

    private fun setupFilterResult() {
        supportFragmentManager.setFragmentResultListener(
            SearchFilterBottomSheet.REQUEST_KEY,
            this
        ) { _, bundle ->
            val filterState = bundle.getParcelable<SearchFilterState>(
                SearchFilterBottomSheet.RESULT_FILTER_STATE
            ) ?: return@setFragmentResultListener
            mViewModel.handleIntent(SearchIntent.UpdateFilter(filterState))
        }
    }

    /**
     * 处理从详情页标签点击跳转过来的场景：
     * 设置标签筛选条件并自动触发搜索
     */
    private fun handleTagIntent() {
        val tag = intent.getStringExtra(EXTRA_TAG) ?: return
        val filterState = SearchFilterState.DEFAULT.copy(tags = listOf(tag))
        mViewModel.handleIntent(SearchIntent.UpdateFilter(filterState))
    }

    private fun setupHistoryView() {
        mHistoryBinding = LayoutSearchHistoryBinding.bind(mBinding.searchHistoryContainer.root)

        // 垃圾箱点击 -> 进入编辑模式
        mHistoryBinding.ivTrash.setOnClickListener {
            enterEditMode()
        }

        // Delete all 点击 -> 弹窗确认清空
        mHistoryBinding.tvDeleteAll.setOnClickListener {
            showClearHistoryDialog()
        }

        // Finish 点击 -> 退出编辑模式
        mHistoryBinding.tvFinish.setOnClickListener {
            exitEditMode()
        }

        // TagGroupView 回调
        mHistoryBinding.tagGroupHistory.setOnHistoryTagActionListener(
            object : SearchHistoryTagGroupView.OnHistoryTagActionListener {
                override fun onTagClick(keyword: String, position: Int) {
                    mBinding.searchBar.setText(keyword)
                    mBinding.searchBar.setSelection(keyword.length)
                    performSearch()
                }

                override fun onTagDelete(keyword: String, position: Int) {
                    mViewModel.handleIntent(SearchIntent.DeleteHistory(keyword))
                }
            }
        )
    }

    private fun enterEditMode() {
        isEditMode = true
        updateEditModeUI()
        mHistoryBinding.tagGroupHistory.setEditMode(true)
    }

    private fun exitEditMode() {
        isEditMode = false
        updateEditModeUI()
        mHistoryBinding.tagGroupHistory.setEditMode(false)
        mHistoryBinding.tagGroupHistory.setExpanded(false)
    }

    private fun updateEditModeUI() {
        if (isEditMode) {
            mHistoryBinding.ivTrash.visibility = View.GONE
            mHistoryBinding.tvDeleteAll.visibility = View.VISIBLE
            mHistoryBinding.divider.visibility = View.VISIBLE
            mHistoryBinding.tvFinish.visibility = View.VISIBLE
        } else {
            mHistoryBinding.ivTrash.visibility = View.VISIBLE
            mHistoryBinding.tvDeleteAll.visibility = View.GONE
            mHistoryBinding.divider.visibility = View.GONE
            mHistoryBinding.tvFinish.visibility = View.GONE
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditMode) {
                    exitEditMode()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.search_history_clear_confirm_title)
            .setMessage(R.string.search_history_clear_confirm_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                mViewModel.handleIntent(SearchIntent.ClearAllHistory)
                exitEditMode()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateHistoryView(history: List<SearchHistoryEntity>) {
        if (history.isEmpty()) {
            mBinding.searchHistoryContainer.root.visibility = View.GONE
            if (isEditMode) {
                exitEditMode()
            }
        } else if (hasSearchResult) {
            mBinding.searchHistoryContainer.root.visibility = View.GONE
        } else {
            mBinding.searchHistoryContainer.root.visibility = View.VISIBLE
            val tags = history.map { TagGroupView.Tag(text = it.keyword) }
            mHistoryBinding.tagGroupHistory.setTags(tags)

            if (!isEditMode) {
                mHistoryBinding.tagGroupHistory.setExpanded(false)
            }
        }
    }

    private fun setupSearchBar() {
        mBinding.searchBar.setOnEditorActionListener { _, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    performSearch()
                    true
                }
                else -> {
                    if (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                        performSearch()
                        true
                    } else {
                        false
                    }
                }
            }
        }

        mBinding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()
                if (query.isNullOrEmpty()) {
                    resetToInitialState()
                }
            }
        })
    }

    private fun resetToInitialState() {
        hasSearchResult = false
        mAdapter.setData(emptyList())
        showEmpty()
        updateHistoryVisibility()
    }

    private fun performSearch() {
        val query = mBinding.searchBar.text?.toString()?.trim()
        val hasFilter = mViewModel.filterState.value.hasActiveFilters()
        if (!query.isNullOrBlank() || hasFilter) {
            if (isEditMode) {
                exitEditMode()
            }
            mViewModel.handleIntent(SearchIntent.Search(query ?: ""))
            mBinding.searchBar.clearFocus()
            KeyboardUtils.hideSoftKeyboard(this, mBinding.searchBar)
        } else {
            Toast.makeText(this, R.string.search_please_input, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initObserver() {
        mBinding.rvSearchResult.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                if (!isLoadingMore && lastVisibleItem >= totalItemCount - 2) {
                    mViewModel.handleIntent(SearchIntent.LoadMore)
                }
            }
        })

        mBinding.tvSearch.setOnClickListener {
            performSearch()
        }

        mBinding.action.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        mViewModel.searchHistory.observe(this) { history ->
            updateHistoryView(history)
        }

        mViewModel.state.observe(this) { state ->
            when (state) {
                is SearchState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    isLoadingMore = false
                    hasSearchResult = false
                    mAdapter.setData(emptyList())
                    showEmpty()
                }
                SearchState.Idle -> {
                    hasSearchResult = false
                    showEmpty()
                }
                SearchState.LoadMore -> {
                    isLoadingMore = true
                }
                is SearchState.LoadMoreError -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    isLoadingMore = false
                }
                SearchState.Loading -> {
                    hasSearchResult = false
                    mAdapter.setData(emptyList())
                    showLoading()
                }
                is SearchState.Success -> {
                    isLoadingMore = false
                    hasSearchResult = state.data.isNotEmpty()
                    if (state.data.isEmpty()) {
                        mAdapter.setData(emptyList())
                        showNoResult()
                    } else {
                        mAdapter.setData(state.data)
                        hideLoading()
                    }
                }
            }
            updateHistoryVisibility()
        }

        // 观察筛选状态 → 更新筛选图标
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.filterState.collectLatest { filterState ->
                    updateFilterIcon(filterState.hasActiveFilters())
                }
            }
        }

        // 观察激活筛选标签
        mViewModel.activeFilterChips.observe(this) { chips ->
            updateActiveFilterChips(chips)
        }
    }

    private fun updateFilterIcon(hasActiveFilters: Boolean) {
        val iconRes = if (hasActiveFilters) R.drawable.ic_filter_active else R.drawable.ic_filter
        mBinding.ivFilter.setImageResource(iconRes)
    }

    private fun updateActiveFilterChips(chips: List<ActiveFilterChip>) {
        val chipGroup = mBinding.chipGroupActiveFilters
        chipGroup.removeAllViews()

        if (chips.isEmpty()) {
            mBinding.filterChipsScroll.visibility = View.GONE
            return
        }

        mBinding.filterChipsScroll.visibility = View.VISIBLE
        chips.forEach { filterChip ->
            val chip = Chip(this).apply {
                text = if (filterChip.displayText.isNotBlank()) {
                    filterChip.displayText
                } else {
                    getString(filterChip.labelResId)
                }
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    mViewModel.handleIntent(SearchIntent.RemoveFilterChip(filterChip))
                }
                textSize = 12f
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateHistoryVisibility() {
        val history = mViewModel.searchHistory.value ?: emptyList()
        updateHistoryView(history)
    }

    private fun showLoading() {
        mBinding.loadingStateView.showLoading()
        mBinding.rvSearchResult.visibility = View.GONE
    }

    private fun showEmpty() {
        mBinding.loadingStateView.showEmpty(message = getString(R.string.search_hint))
        mBinding.rvSearchResult.visibility = View.GONE
    }

    private fun showNoResult() {
        mBinding.loadingStateView.showEmpty(message = getString(R.string.search_no_result))
        mBinding.rvSearchResult.visibility = View.GONE
    }

    private fun hideLoading() {
        mBinding.loadingStateView.hide()
        mBinding.rvSearchResult.visibility = View.VISIBLE
    }
}
