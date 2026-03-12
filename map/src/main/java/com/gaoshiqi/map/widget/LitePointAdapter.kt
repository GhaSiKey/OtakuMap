package com.gaoshiqi.map.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadCover
import com.gaoshiqi.map.R
import com.gaoshiqi.map.data.LitePoint
import com.gaoshiqi.map.utils.GoogleMapUtils

/**
 * Created by gaoshiqi
 * on 2025/7/11 17:57
 * email: gaoshiqi@bilibili.com
 */
class LitePointAdapter(
    private val points: List<LitePoint>
): RecyclerView.Adapter<LitePointAdapter.LitePointViewHolder>() {

    inner class LitePointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cover: ImageView = itemView.findViewById(R.id.item_cover)
        private val title: TextView = itemView.findViewById(R.id.item_title)
        private val description: TextView = itemView.findViewById(R.id.item_description)
        private val btnOpenMaps: ImageButton = itemView.findViewById(R.id.btn_open_maps)

        fun bind(litePoint: LitePoint) {
            cover.loadCover(litePoint.image, R.drawable.placeholder_landscape)

            title.text = litePoint.name
            description.text = "坐标: ${litePoint.geo[0]}, ${litePoint.geo[1]}"

            btnOpenMaps.setOnClickListener {
                if (litePoint.geo.size >= 2) {
                    GoogleMapUtils.openInGoogleMaps(
                        itemView.context,
                        litePoint.geo[0],
                        litePoint.geo[1],
                        litePoint.displayName()
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LitePointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lite_point, parent, false)
        return LitePointViewHolder(view)
    }

    override fun onBindViewHolder(holder: LitePointViewHolder, position: Int) {
        holder.bind(points[position])
    }

    override fun getItemCount() = points.size
}