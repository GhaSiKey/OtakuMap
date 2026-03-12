package com.gaoshiqi.otakumap.schedule.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.gaoshiqi.image.loadCover
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.data.bean.SubjectSmall
import com.gaoshiqi.otakumap.detail.BangumiDetailActivity
import com.gaoshiqi.otakumap.utils.BangumiUtils

/**
 * Created by gaoshiqi
 * on 2025/5/30 14:05
 * email: gaoshiqi@bilibili.com
 */
class BangumiAdapter: RecyclerView.Adapter<BangumiViewHolder>() {
    private var mData: List<SubjectSmall>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<SubjectSmall>) {
        mData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BangumiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bangumi_card, parent, false)
        return BangumiViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: BangumiViewHolder,
        position: Int
    ) {
        mData?.get(position)?.apply {
            holder.setView(this)
        } ?: return
    }

    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}

class BangumiViewHolder(val view: View): RecyclerView.ViewHolder(view) {
    val container: ConstraintLayout = view.findViewById(R.id.bangumi_card_container)
    val image: ImageView = view.findViewById(R.id.iv_cover)
    val title: TextView = view.findViewById(R.id.tv_title)
    val titleCn: TextView = view.findViewById(R.id.tv_title_cn)
    val sidContainer: LinearLayout = view.findViewById(R.id.sid_container)
    val sid: TextView = view.findViewById(R.id.tv_sid)
    val airDate: TextView = view.findViewById(R.id.tv_air_date)
    val onDoing: TextView = view.findViewById(R.id.tv_on_doing)
    val score: TextView = view.findViewById(R.id.tv_score)
    val scoreCount: TextView = view.findViewById(R.id.tv_score_count)

    @SuppressLint("SetTextI18n")
    fun setView(data: SubjectSmall) {
        image.loadCover(data.images?.large, R.drawable.placeholder_cover)
        title.text = data.name
        titleCn.text = data.nameCn
        sid.text = "ID: " + data.id
        airDate.text = data.airDate + "开播"
        onDoing.text = BangumiUtils.convertCount(data.collection?.doing?: 0) + " 人在追"
        score.text = data.rating?.score?.toString() ?: "0.0"
        scoreCount.text = BangumiUtils.convertCount(data.rating?.total ?: 0) + " 人打分"

        sidContainer.setOnClickListener {
            BangumiUtils.copyContentToClipboard(data.id.toString(), view.context)
            Toast.makeText(view.context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }

        container.setOnClickListener {
            data.id?.let {
                BangumiDetailActivity.start(view.context, it)
            }
        }
    }


}