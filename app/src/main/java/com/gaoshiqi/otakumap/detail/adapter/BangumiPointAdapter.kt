package com.gaoshiqi.otakumap.detail.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadCover
import com.gaoshiqi.image.viewer.ImageViewerActivity
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.map.data.LitePoint

/**
 * Created by gaoshiqi
 * on 2025/6/22 21:46
 * email: gaoshiqi@bilibili.com
 */
class BangumiPointAdapter(
    private val OnItemClickListener: (LitePoint) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_POINT = 1
    }
    
    private var items: List<PointListItem> = emptyList()

    fun updateList(list: List<PointListItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PointListItem.Header -> TYPE_HEADER
            is PointListItem.Point -> TYPE_POINT
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_episode_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_POINT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_point, parent, false)
                PointViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = items[position]) {
            is PointListItem.Header -> {
                (holder as HeaderViewHolder).bind(item.episode)
            }
            is PointListItem.Point -> {
                (holder as PointViewHolder).bind(item.litePoint)
                holder.itemView.setOnClickListener {
                    OnItemClickListener(item.litePoint)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class HeaderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val tvEpisodeTitle: TextView = itemView.findViewById(R.id.tvEpisodeTitle)

        fun bind(episode: String) {
            tvEpisodeTitle.text = formatEpisodeTitle(episode)
        }
        
        private fun formatEpisodeTitle(episode: String): String {
            return when {
                episode.isBlank() || episode == "null" -> "其他"
                episode.matches("\\d+".toRegex()) -> "第${episode}集"
                else -> episode
            }
        }
    }

    inner class PointViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val pointImage: ImageView = itemView.findViewById(R.id.pointImage)
        private val pointName: TextView = itemView.findViewById(R.id.pointName)
        private val pointId: TextView = itemView.findViewById(R.id.pointId)
        private val pointEp: TextView = itemView.findViewById(R.id.pointEpisode)

        fun bind(point: LitePoint) {
            pointImage.loadCover(point.image, R.drawable.placeholder_landscape)
            pointName.text = point.name
            pointId.text = "ID: " + point.id
            setEpAndTimeText(pointEp, point.ep, point.s)

            // 点击图片打开图片查看器
            point.image.let { imageUrl ->
                pointImage.setOnClickListener {
                    val activity = itemView.context as? Activity ?: return@setOnClickListener
                    ImageViewerActivity.startWithSharedElement(activity, imageUrl, pointImage)
                }
            }
        }

        private fun setEpAndTimeText(textView: TextView, ep: String?, s: String?) {
            val epText = if (!ep.isNullOrEmpty()) "EP: $ep" else "EP: 未知"
            if (!s.isNullOrEmpty()) {
                val seconds = s.toIntOrNull()
                if (seconds != null) {
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    textView.text = epText + "  " + String.format("%02d:%02d", minutes, remainingSeconds)
                    return
                }
            }
            textView.text = epText
        }
    }
}