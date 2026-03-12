package com.gaoshiqi.otakumap.detail.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadWithOriginalRatio
import com.gaoshiqi.image.viewer.ImageViewerActivity
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.data.bean.BangumiCharacter
import com.gaoshiqi.otakumap.utils.BangumiUtils

/**
 * Created by gaoshiqi
 * on 2025/6/22 15:48
 * email: gaoshiqi@bilibili.com
 */
class BangumiCharacterAdapter(
    private val context: Context,
    private val columnCount: Int
): RecyclerView.Adapter<BangumiCharacterAdapter.BangumiCharacterViewHolder>() {

    private var characters: List<BangumiCharacter> = emptyList()
    private val columnWidth: Int by lazy {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val padding = (columnCount + 1) * 8 // 8dp padding between items
        (screenWidth - padding) / columnCount
    }

    fun updateList(list: List<BangumiCharacter>) {
        characters = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BangumiCharacterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.gaoshiqi.otakumap.R.layout.item_character, parent, false)
        return BangumiCharacterViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: BangumiCharacterViewHolder,
        position: Int
    ) {
        val character = characters[position]
        holder.bind(character, columnWidth)
    }

    override fun getItemCount(): Int {
        return characters.size
    }

    inner class BangumiCharacterViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val characterId: TextView = itemView.findViewById(com.gaoshiqi.otakumap.R.id.characterId)
        private val characterImage: ImageView = itemView.findViewById(com.gaoshiqi.otakumap.R.id.characterImage)
        private val characterName: TextView = itemView.findViewById(com.gaoshiqi.otakumap.R.id.characterName)
        private val characterActor: TextView = itemView.findViewById(com.gaoshiqi.otakumap.R.id.characterActor)

        fun bind(character: BangumiCharacter, columnWidth: Int) {
            characterId.text = "ID: " + character.id
            characterName.text = character.name
            val actor = character.actors.firstOrNull()
            if (actor != null) {
                characterActor.text = actor.name
            }

            characterImage.layoutParams.width = columnWidth
            characterImage.requestLayout()
            val imageUrl = character.images?.medium
            if (!imageUrl.isNullOrEmpty()) {
                characterImage.loadWithOriginalRatio(imageUrl, R.drawable.placeholder_cover, columnWidth)

                // 点击图片打开图片查看器（使用大图）
                val largeImageUrl = character.images.large ?: imageUrl
                characterImage.setOnClickListener {
                    val activity = itemView.context as? Activity ?: return@setOnClickListener
                    ImageViewerActivity.startWithSharedElement(activity, largeImageUrl, characterImage)
                }
            }

            characterId.setOnClickListener {
                BangumiUtils.copyContentToClipboard(character.id.toString(), itemView.context)
                Toast.makeText(itemView.context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        }
    }
}