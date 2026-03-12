package com.gaoshiqi.otakumap.search.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadCover
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.data.bean.SearchSubject
import com.gaoshiqi.otakumap.detail.BangumiDetailActivity
import com.gaoshiqi.otakumap.search.filter.SubjectType
import com.gaoshiqi.otakumap.utils.BangumiUtils

/**
 * 新版搜索结果 Adapter
 * 使用 SearchSubject 数据模型
 */
class SearchResultAdapter : RecyclerView.Adapter<SearchResultViewHolder>() {
    private var mData: List<SearchSubject> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<SearchSubject>) {
        mData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bangumi_card, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        mData.getOrNull(position)?.let { holder.setView(it) }
    }

    override fun getItemCount(): Int = mData.size

    override fun getItemId(position: Int): Long = position.toLong()
}

class SearchResultViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    private val container: ConstraintLayout = view.findViewById(R.id.bangumi_card_container)
    private val image: ImageView = view.findViewById(R.id.iv_cover)
    private val title: TextView = view.findViewById(R.id.tv_title)
    private val titleCn: TextView = view.findViewById(R.id.tv_title_cn)
    private val sidContainer: LinearLayout = view.findViewById(R.id.sid_container)
    private val sid: TextView = view.findViewById(R.id.tv_sid)
    private val airDate: TextView = view.findViewById(R.id.tv_air_date)
    private val onDoing: TextView = view.findViewById(R.id.tv_on_doing)
    private val score: TextView = view.findViewById(R.id.tv_score)
    private val scoreCount: TextView = view.findViewById(R.id.tv_score_count)
    private val typeBadge: TextView = view.findViewById(R.id.tv_type_badge)
    private val tagContainer: LinearLayout = view.findViewById(R.id.tag_container)

    companion object {
        private const val MAX_TAGS = 3
    }

    fun setView(data: SearchSubject) {
        val context = view.context
        // 图片优先使用 images.large，如果没有则使用 image 字段
        val imageUrl = data.images?.large ?: data.image
        image.loadCover(imageUrl, R.drawable.placeholder_cover)

        title.text = data.name
        titleCn.text = data.nameCn
        sid.text = "ID: ${data.id}"

        // 日期 + 平台
        val dateText = data.date ?: ""
        if (!data.platform.isNullOrBlank()) {
            airDate.text = context.getString(R.string.card_air_date_platform_format, dateText, data.platform)
        } else {
            airDate.text = context.getString(R.string.card_air_date_format, dateText)
        }

        onDoing.text = context.getString(
            R.string.card_watching_count_format,
            BangumiUtils.convertCount(data.collection?.doing ?: 0)
        )
        score.text = data.rating?.score?.toString() ?: "0.0"
        scoreCount.text = context.getString(
            R.string.card_score_count_format,
            BangumiUtils.convertCount(data.rating?.total ?: 0)
        )

        // 类型角标
        bindTypeBadge(data.type)

        // 标签
        bindTags(data)

        sidContainer.setOnClickListener {
            BangumiUtils.copyContentToClipboard(data.id.toString(), context)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }

        container.setOnClickListener {
            data.id?.let { BangumiDetailActivity.start(view.context, it) }
        }
    }

    private fun bindTypeBadge(type: Int?) {
        val subjectType = type?.let { SubjectType.fromTypeId(it) }
        if (subjectType != null) {
            typeBadge.setText(subjectType.labelResId)
            typeBadge.visibility = View.VISIBLE
        } else {
            typeBadge.visibility = View.GONE
        }
    }

    private fun bindTags(data: SearchSubject) {
        tagContainer.removeAllViews()

        val topTags = data.tags
            ?.sortedByDescending { it.count ?: 0 }
            ?.take(MAX_TAGS)

        if (topTags.isNullOrEmpty()) {
            tagContainer.visibility = View.GONE
            return
        }

        tagContainer.visibility = View.VISIBLE
        val context = view.context

        topTags.forEachIndexed { index, tag ->
            val tagView = TextView(context).apply {
                text = tag.name
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, R.color.black_60))
                setBackgroundResource(R.drawable.bg_card_tag)
                setPadding(
                    dpToPx(6f).toInt(), dpToPx(2f).toInt(),
                    dpToPx(6f).toInt(), dpToPx(2f).toInt()
                )
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index > 0) {
                params.marginStart = dpToPx(4f).toInt()
            }
            tagContainer.addView(tagView, params)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * view.context.resources.displayMetrics.density
    }
}
