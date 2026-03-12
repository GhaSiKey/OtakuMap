package com.gaoshiqi.map.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.gaoshiqi.image.ImageLoader
import com.gaoshiqi.map.R
import com.gaoshiqi.map.databinding.CustomInfoViewBinding
import com.google.android.gms.maps.model.Marker

class CustomInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): LinearLayout(context, attrs) {

    private val mBinding = CustomInfoViewBinding.inflate(LayoutInflater.from(context), this, true)

    fun setMarker(marker: Marker, imageUrl: String?) {
        mBinding.pointName.text = marker.title
        mBinding.tvTitle.text = marker.snippet

        // Google Maps InfoWindow是静态快照，异步加载图片时需要手动刷新InfoWindow，否则第一次看不到图片。
        ImageLoader.loadCoverWithCallback(
            imageView = mBinding.cover,
            url = imageUrl,
            placeholder = R.drawable.placeholder_landscape,
            onSuccess = {
                if (marker.isInfoWindowShown) {
                    marker.hideInfoWindow()
                    marker.showInfoWindow()
                }
            }
        )
    }
}