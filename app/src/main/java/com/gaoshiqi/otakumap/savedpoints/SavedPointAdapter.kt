package com.gaoshiqi.otakumap.savedpoints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadCover
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.databinding.ItemSavedPointBinding
import com.gaoshiqi.room.SavedPointEntity

/**
 * 横向地点列表适配器
 */
class SavedPointAdapter(
    private val onPointClick: (SavedPointEntity) -> Unit
) : ListAdapter<SavedPointEntity, SavedPointAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedPointBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSavedPointBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(point: SavedPointEntity) {
            // 地点图片
            binding.ivPointImage.loadCover(point.pointImage, R.drawable.placeholder_cover)

            // 地点名称（优先中文名）
            binding.tvPointName.text = point.pointNameCn.ifEmpty { point.pointName }

            // 出现时间
            binding.tvEpisodeTime.text = formatEpisodeTime(point.episode, point.timeInEpisode)

            // 点击事件
            binding.root.setOnClickListener {
                onPointClick(point)
            }
        }

        private fun formatEpisodeTime(episode: String?, time: String?): String {
            val epStr = episode?.let { "EP$it" } ?: ""
            val timeStr = time?.toIntOrNull()?.let { seconds ->
                val min = seconds / 60
                val sec = seconds % 60
                String.format("%02d:%02d", min, sec)
            } ?: ""
            return listOf(epStr, timeStr).filter { it.isNotEmpty() }.joinToString(" ")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SavedPointEntity>() {
        override fun areItemsTheSame(oldItem: SavedPointEntity, newItem: SavedPointEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavedPointEntity, newItem: SavedPointEntity): Boolean {
            return oldItem == newItem
        }
    }
}
