package com.gaoshiqi.otakumap.detail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.annotation.StringRes
import com.gaoshiqi.image.loadDetail
import com.gaoshiqi.otakumap.BangumiApplication
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.data.bean.BangumiDetail
import com.gaoshiqi.otakumap.databinding.ActivityBangumiDetailBinding
import com.gaoshiqi.otakumap.detail.adapter.BangumiDetailPagerAdapter
import com.gaoshiqi.otakumap.detail.viewmodel.BangumiDetailIntent
import com.gaoshiqi.otakumap.detail.viewmodel.BangumiDetailState
import com.gaoshiqi.otakumap.detail.viewmodel.BangumiDetailViewModel
import com.gaoshiqi.otakumap.detail.viewmodel.BangumiPointsViewModel
import com.gaoshiqi.otakumap.search.SearchActivity
import com.gaoshiqi.otakumap.utils.BangumiUtils
import com.gaoshiqi.otakumap.widget.TagGroupView
import com.gaoshiqi.room.AnimeEntity
import com.gaoshiqi.room.AnimeMarkRepository
import com.gaoshiqi.image.viewer.ImageViewerActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class BangumiDetailActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityBangumiDetailBinding
    private val mViewModel: BangumiDetailViewModel by viewModels()
    private val mPointsViewModel: BangumiPointsViewModel by viewModels()
    private var mSubjectId: Int = -1
    private lateinit var repository: AnimeMarkRepository

    // 收藏状态管理
    private var isMarked: Boolean = false
    private var isMarkOperating: Boolean = false  // 防抖标志

    companion object {
        const val ARG_SUBJECT_ID = "subject_id"

        fun start(context: Context, subjectId: Int) {
            val intent = Intent(context, BangumiDetailActivity::class.java).apply {
                putExtra(ARG_SUBJECT_ID, subjectId)
            }
            context.startActivity(intent)
        }
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBangumiDetailBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        // 设置顶部 padding 以适配状态栏
        mBinding.root.setOnApplyWindowInsetsListener { view, insets ->
            val topInset = insets.getInsets(WindowInsets.Type.statusBars()).top
            val bottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            view.setPadding(0, topInset, 0, bottomInset)
            insets
        }

        // 获取传入的subjectID
        mSubjectId = intent.getIntExtra(ARG_SUBJECT_ID, 0)
        if (mSubjectId == 0) {
            Toast.makeText(application, R.string.invalid_bangumi_id, Toast.LENGTH_SHORT).show()
            finish()
        }

        initView()
        initObserver()
        mViewModel.setSubjectId(mSubjectId)
    }

    private fun initView() {
        val adapter = BangumiDetailPagerAdapter(this, mSubjectId)
        mBinding.viewPager.adapter = adapter
        TabLayoutMediator(mBinding.tabLayout, mBinding.viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }

    private fun initObserver() {
        mViewModel.state.observe(this) { state ->
            when (state) {
                is BangumiDetailState.LOADING -> {
                    showLoading()
                }
                is BangumiDetailState.SUCCESS -> {
                    hideLoading()
                    showBangumiDetail(state.data)

                    // 设置番剧信息到 PointsViewModel，供圣地巡礼页面使用
                    mPointsViewModel.setSubjectInfo(
                        id = mSubjectId,
                        name = state.data.displayTitle(),
                        cover = state.data.images.large
                    )

                    repository = (application as BangumiApplication).animeMarkRepository
                    setupMarkButton(state.data)
                }
                is BangumiDetailState.ERROR -> {
                    showError(state.msg)
                }
                else -> {}
            }
        }
    }

    private fun setupMarkButton(data: BangumiDetail) {
        lifecycleScope.launch {
            isMarked = repository.isBookmarked(mSubjectId)
            updateMarkButton(isMarked)
        }

        mBinding.btnMark.setOnClickListener {
            if (isMarkOperating) return@setOnClickListener  // 防抖：操作进行中则忽略点击

            lifecycleScope.launch {
                isMarkOperating = true
                mBinding.btnMark.isEnabled = false

                try {
                    if (isMarked) {
                        // 取消收藏
                        repository.removeAnimeMark(AnimeEntity(
                            id = mSubjectId,
                            name = data.name,
                            nameCn = data.nameCn,
                            imageUrl = data.images.large
                        ))
                        isMarked = false
                        updateMarkButton(false)
                        showToast(R.string.mark_removed)
                    } else {
                        // 添加收藏
                        repository.addAnimeMark(AnimeEntity(
                            id = mSubjectId,
                            name = data.name,
                            nameCn = data.nameCn,
                            imageUrl = data.images.large
                        ))
                        isMarked = true
                        updateMarkButton(true)
                        showToast(R.string.mark_added)
                    }
                } catch (e: Exception) {
                    // 操作失败，保持原状态
                    showToast(R.string.mark_operation_failed)
                } finally {
                    isMarkOperating = false
                    mBinding.btnMark.isEnabled = true
                }
            }
        }
    }

    private fun updateMarkButton(isMarked: Boolean) {
        mBinding.btnMark.text = getString(if (isMarked) R.string.mark_button_marked else R.string.mark_button_unmarked)
    }


    private fun showBangumiDetail(data: BangumiDetail) {
        // 标题
        mBinding.bangumiTitle.text = data.displayTitle()
        // 封面
        mBinding.ivCover.loadDetail(data.images.large, R.drawable.ic_cover_placeholder_36)
        // 点击封面查看大图（带共享元素转场动画）
        mBinding.ivCover.setOnClickListener {
            ImageViewerActivity.startWithSharedElement(this, data.images.large, mBinding.ivCover)
        }
        // 信息
        mBinding.bangumiAirDate.text = data.date
        mBinding.bangumiScoreCount.text = getString(R.string.detail_score_count_format, BangumiUtils.convertCount(data.rating?.total ?: 0))
        mBinding.bangumiScore.text = data.rating?.score?.toString()?: "0.0"
        // 设置标签
        val tagList = BangumiUtils.getTags(data)
        mBinding.bangumiTags.setTags(tagList)
        mBinding.bangumiTags.setOnTagClickListener(object : TagGroupView.OnTagClickListener {
            override fun onTagClick(tag: TagGroupView.Tag, position: Int) {
                SearchActivity.startWithTag(this@BangumiDetailActivity, tag.text)
            }
        })
    }


    private fun showLoading() {
        mBinding.loadingStateView.showLoading()
        mBinding.detailContainer.visibility = View.GONE
    }

    private fun hideLoading() {
        mBinding.loadingStateView.hide()
        mBinding.detailContainer.visibility = View.VISIBLE
    }

    private fun showError(msg: String) {
        mBinding.detailContainer.visibility = View.GONE
        mBinding.loadingStateView.showError(message = msg) {
            mViewModel.processIntent(BangumiDetailIntent.Retry)
        }
    }

    private fun showToast(@StringRes resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}