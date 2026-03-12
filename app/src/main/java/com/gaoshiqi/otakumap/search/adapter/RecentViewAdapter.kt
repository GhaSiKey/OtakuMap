package com.gaoshiqi.otakumap.search.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadCover
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.detail.BangumiDetailActivity
import com.gaoshiqi.room.RecentViewEntity

class RecentViewAdapter : RecyclerView.Adapter<RecentViewAdapter.ViewHolder>() {

    private var mData: List<RecentViewEntity> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<RecentViewEntity>) {
        mData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_view_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        mData.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun getItemCount(): Int = mData.size

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val cover: ImageView = view.findViewById(R.id.iv_cover)
        private val title: TextView = view.findViewById(R.id.tv_title)
        private val score: TextView = view.findViewById(R.id.tv_score)

        fun bind(data: RecentViewEntity) {
            val context = view.context
            cover.loadCover(data.imageUrl, R.drawable.ic_cover_placeholder_36)
            title.text = data.displayName

            if (data.score > 0) {
                score.text = context.getString(R.string.recent_view_score_format, data.score)
                score.visibility = View.VISIBLE
            } else {
                score.text = context.getString(R.string.recent_view_no_score)
                score.visibility = View.VISIBLE
            }

            view.setOnClickListener {
                BangumiDetailActivity.start(context, data.id)
            }
        }
    }
}
