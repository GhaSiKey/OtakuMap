package com.gaoshiqi.otakumap.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadCover
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.databinding.ItemSecondaryChildCardBinding
import com.gaoshiqi.otakumap.databinding.ItemSecondaryCreationBinding

/**
 * Secondary Creation 卡片 ViewHolder
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * 布局规范 (Layout Specification)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【布局模式】
 * ┌─────────────────┬────────────┬────────────┐
 * │ 子卡片数量       │ 布局方式    │ 横滑支持    │
 * ├─────────────────┼────────────┼────────────┤
 * │ 1-3 张          │ 单列垂直    │ 否         │
 * │ 4-5 张          │ 2 行多列    │ 是         │
 * │ 6-9 张          │ 3 行多列    │ 是         │
 * └─────────────────┴────────────┴────────────┘
 *
 * 【排列顺序】列优先 (Column-First / Snake Pattern)
 * 4-5张 (2行):          6-9张 (3行):
 * ┌───┬───┬───┐         ┌───┬───┬───┐
 * │ 1 │ 3 │ 5 │         │ 1 │ 4 │ 7 │
 * ├───┼───┼───┤         ├───┼───┼───┤
 * │ 2 │ 4 │   │         │ 2 │ 5 │ 8 │
 * └───┴───┴───┘         ├───┼───┼───┤
 *                       │ 3 │ 6 │ 9 │
 *                       └───┴───┴───┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * 尺寸规范 (Dimension Specification)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【外层容器】
 * - 头部高度: 40dp (标题 + 箭头按钮)
 * - 头部左右内边距: 16dp
 * - 内容区底部内边距: 16dp
 *
 * 【子卡片尺寸】基于 360dp 设计稿宽度
 * - 封面宽度: screenWidth × (124/360) ≈ 34.44%
 * - 封面高度: coverWidth × (69/124) ≈ 宽高比 124:69
 * - 横滑模式卡片宽度: screenWidth × 77.5%
 *
 * 【间距规范】
 * - 屏幕边距: 16dp (卡片与屏幕左右边缘)
 * - 列间距: 24dp (横滑模式下列与列之间)
 * - 行间距: 12dp (第2/3行子卡片顶部)
 * - 封面与文字间距: 8dp
 *
 * 【子卡片内部布局】
 * ┌──────────┬─────────────────────────────┬───┐
 * │          │ 标题 (最多2行, 12sp)          │   │
 * │  封面     │ 头像(14dp) + 作者名 (10sp)   │ ⋮ │ ← 三点菜单
 * │ (序号)   │ 播放图标 + 播放量 (10sp)      │   │
 * │ [时长]   │                             │   │
 * └──────────┴─────────────────────────────┴───┘
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * 数据模型 (Data Model)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * data class ChildCardData(
 *     val id: String,           // 唯一标识
 *     val title: String,        // 标题 (推荐最多2行)
 *     val coverUrl: String,     // 封面图URL
 *     val authorName: String,   // 作者名称
 *     val authorAvatar: String, // 作者头像URL
 *     val duration: String,     // 时长 (如 "04:25")
 *     val playCount: String     // 播放量 (如 "22.4K")
 * )
 *
 * data class SecondaryCreationSection(
 *     val sectionTitle: String,       // 区块标题
 *     val items: List<ChildCardData>  // 子卡片列表 (1-9个)
 * )
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 * 使用示例 (Usage Example)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * // 在 Adapter 中创建 ViewHolder
 * override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SecondaryCreationViewHolder {
 *     val binding = ItemSecondaryCreationBinding.inflate(
 *         LayoutInflater.from(parent.context), parent, false
 *     )
 *     return SecondaryCreationViewHolder(
 *         binding = binding,
 *         onItemClick = { item -> /* 处理卡片点击 */ },
 *         onMoreClick = { item -> /* 处理三点菜单点击 */ }
 *     )
 * }
 *
 * // 绑定数据
 * override fun onBindViewHolder(holder: SecondaryCreationViewHolder, position: Int) {
 *     holder.bind(sections[position])
 * }
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
class SecondaryCreationViewHolder(
    private val binding: ItemSecondaryCreationBinding,
    private val onItemClick: (ChildCardData) -> Unit,
    private val onMoreClick: (ChildCardData) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val context: Context = binding.root.context
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val screenWidth: Int = context.resources.displayMetrics.widthPixels

    // 封面固定宽度 = 屏幕宽度 * 124/360
    private val coverWidth: Int = (screenWidth * 124f / 360f).toInt()

    // 封面固定高度 = 封面宽度 * 69/124
    private val coverHeight: Int = (coverWidth * 69f / 124f).toInt()

    // 横滑模式下子卡片宽度 = 屏幕宽度 * 0.775
    private val childCardWidth: Int = (screenWidth * 0.775).toInt()

    // 行间距 12dp
    private val rowSpacing: Int = dpToPx(12)

    // 列间距 24dp
    private val columnSpacing: Int = dpToPx(24)

    // 屏幕边距 16dp
    private val screenMargin: Int = dpToPx(16)

    fun bind(section: SecondaryCreationSection) {
        binding.tvSectionTitle.text = section.sectionTitle
        binding.flContentContainer.removeAllViews()

        when {
            section.items.size <= 3 -> setupVerticalLayout(section.items)
            section.items.size <= 5 -> setupScrollableLayout(section.items, rowCount = 2)
            else -> setupScrollableLayout(section.items, rowCount = 3)
        }
    }

    /**
     * 1-3 张：单列垂直排列，宽度撑满，左右各 16dp 边距
     */
    private fun setupVerticalLayout(items: List<ChildCardData>) {
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = screenMargin
                marginEnd = screenMargin
            }
        }

        items.forEachIndexed { index, item ->
            val isFirstRow = index == 0
            val childView = createChildCard(item, index + 1, isFirstRow)
            linearLayout.addView(childView)
        }

        binding.flContentContainer.addView(linearLayout)
    }

    /**
     * 多行横滑布局
     * @param rowCount 行数 (4-5张为2行，6-9张为3行)
     */
    private fun setupScrollableLayout(items: List<ChildCardData>, rowCount: Int) {
        val scrollView = createHorizontalScrollView()
        val container = createHorizontalContainer()
        val columns = (items.size + rowCount - 1) / rowCount

        for (col in 0 until columns) {
            val columnLayout = createVerticalColumn(
                isFirstColumn = col == 0,
                isLastColumn = col == columns - 1
            )

            for (row in 0 until rowCount) {
                val itemIndex = col * rowCount + row
                if (itemIndex < items.size) {
                    val childView = createChildCard(
                        item = items[itemIndex],
                        index = itemIndex + 1,
                        isFirstRow = row == 0
                    )
                    columnLayout.addView(childView)
                }
            }
            container.addView(columnLayout)
        }

        scrollView.addView(container)
        binding.flContentContainer.addView(scrollView)
    }

    private fun createHorizontalScrollView(): HorizontalScrollView {
        return HorizontalScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    private fun createHorizontalContainer(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createVerticalColumn(isFirstColumn: Boolean, isLastColumn: Boolean): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                childCardWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // 第一列左边距 16dp（屏幕边距），其他列左边距 24dp（列间距）
                marginStart = if (isFirstColumn) screenMargin else columnSpacing
                // 最后一列右边距 16dp（屏幕边距）
                marginEnd = if (isLastColumn) screenMargin else 0
            }
        }
    }

    /**
     * 创建子卡片视图
     * @param isFirstRow 是否是第一行（第一行无顶部间距，其他行顶部 12dp）
     */
    private fun createChildCard(item: ChildCardData, index: Int, isFirstRow: Boolean): View {
        val childBinding = ItemSecondaryChildCardBinding.inflate(inflater)

        childBinding.root.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            // 第一行无顶部间距，其他行顶部 12dp
            topMargin = if (isFirstRow) 0 else rowSpacing
        }

        // 固定封面尺寸
        childBinding.cardCover.layoutParams =
            (childBinding.cardCover.layoutParams as ConstraintLayout.LayoutParams).apply {
                width = coverWidth
                height = coverHeight
                matchConstraintPercentWidth = 1f
                dimensionRatio = null
            }

        // 绑定数据
        childBinding.tvIndex.text = index.toString()
        childBinding.tvTitle.text = item.title
        childBinding.tvAuthor.text = item.authorName
        childBinding.tvDuration.text = item.duration
        childBinding.tvPlayCount.text = item.playCount

        // 加载封面图
        if (item.coverUrl.isNotEmpty()) {
            childBinding.ivCover.loadCover(item.coverUrl, R.drawable.placeholder_cover)
        } else {
            childBinding.ivCover.setImageResource(R.drawable.placeholder_cover)
        }

        // 加载头像
        if (item.authorAvatar.isNotEmpty()) {
            childBinding.ivAvatar.loadCover(item.authorAvatar, R.drawable.placeholder_avatar)
        }

        // 点击事件
        childBinding.root.setOnClickListener { onItemClick(item) }
        childBinding.ivMore.setOnClickListener { onMoreClick(item) }

        return childBinding.root
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
