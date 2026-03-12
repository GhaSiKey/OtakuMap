package com.gaoshiqi.otakumap.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.databinding.ActivitySearchTestBinding
import com.gaoshiqi.otakumap.search.adapter.SearchResultAdapter
import com.gaoshiqi.otakumap.utils.KeyboardUtils

/**
 * 新搜索接口测试页面
 * 用于验证 /v0/search/subjects 接口
 */
class SearchTestActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivitySearchTestBinding
    private val mViewModel: SearchTestViewModel by viewModels()
    private val mAdapter = SearchResultAdapter()
    private var isLoadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivitySearchTestBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
        initObserver()
    }

    private fun initView() {
        mBinding.rvResult.adapter = mAdapter
        mBinding.rvResult.layoutManager = LinearLayoutManager(this)

        // 返回按钮
        mBinding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 搜索按钮
        mBinding.tvSearch.setOnClickListener {
            performSearch()
        }

        // 键盘搜索
        mBinding.etSearch.setOnEditorActionListener { _, actionId, event ->
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

        // 滚动加载更多
        mBinding.rvResult.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                if (!isLoadingMore && lastVisibleItem >= totalItemCount - 2) {
                    mViewModel.loadMore()
                }
            }
        })

        // 初始显示空态
        showEmpty()
    }

    private fun initObserver() {
        mViewModel.state.observe(this) { state ->
            when (state) {
                is SearchTestState.Idle -> {
                    mAdapter.setData(emptyList())
                    showEmpty()
                    hideResultCount()
                }
                is SearchTestState.Loading -> {
                    isLoadingMore = false
                    mAdapter.setData(emptyList())
                    showLoading()
                    hideResultCount()
                }
                is SearchTestState.Success -> {
                    isLoadingMore = false
                    if (state.data.isEmpty()) {
                        mAdapter.setData(emptyList())
                        showNoResult()
                        hideResultCount()
                    } else {
                        mAdapter.setData(state.data)
                        hideLoading()
                        showResultCount(state.total)
                    }
                }
                is SearchTestState.Error -> {
                    isLoadingMore = false
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    mAdapter.setData(emptyList())
                    showEmpty()
                    hideResultCount()
                }
                is SearchTestState.LoadMore -> {
                    isLoadingMore = true
                }
                is SearchTestState.LoadMoreError -> {
                    isLoadingMore = false
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performSearch() {
        val query = mBinding.etSearch.text?.toString()?.trim()
        if (!query.isNullOrBlank()) {
            mViewModel.search(query)
            mBinding.etSearch.clearFocus()
            KeyboardUtils.hideSoftKeyboard(this, mBinding.etSearch)
        } else {
            Toast.makeText(this, R.string.search_please_input, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading() {
        mBinding.loadingStateView.showLoading()
        mBinding.rvResult.visibility = View.GONE
    }

    private fun showEmpty() {
        mBinding.loadingStateView.showEmpty(message = getString(R.string.search_input_hint))
        mBinding.rvResult.visibility = View.GONE
    }

    private fun showNoResult() {
        mBinding.loadingStateView.showEmpty(message = getString(R.string.search_no_result))
        mBinding.rvResult.visibility = View.GONE
    }

    private fun hideLoading() {
        mBinding.loadingStateView.hide()
        mBinding.rvResult.visibility = View.VISIBLE
    }

    private fun showResultCount(total: Int) {
        mBinding.tvResultCount.text = getString(R.string.search_result_count, total)
        mBinding.tvResultCount.visibility = View.VISIBLE
    }

    private fun hideResultCount() {
        mBinding.tvResultCount.visibility = View.GONE
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SearchTestActivity::class.java)
            context.startActivity(intent)
        }
    }
}
