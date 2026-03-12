package com.gaoshiqi.otakumap.collection.v2.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.room.AnimeEntity
import com.gaoshiqi.room.CollectionStatus

/**
 * 状态角标颜色映射 - 使用马卡龙配色与轮盘按钮保持一致
 * 返回字符串资源 ID 和颜色的配对
 */
private fun getStatusBadgeInfo(status: Int): Pair<Int, Color>? = when (status) {
    CollectionStatus.DOING -> R.string.collection_doing to MacaronColors.Watching
    CollectionStatus.WISH -> R.string.collection_wish to MacaronColors.Wish
    CollectionStatus.COLLECT -> R.string.collection_collect to MacaronColors.Completed
    CollectionStatus.ON_HOLD -> R.string.collection_on_hold to MacaronColors.OnHold
    else -> null
}

/**
 * 状态角标组件
 * 使用马卡龙配色，深色文字确保可读性
 *
 * @param status 收藏状态
 * @param onClick 点击回调，用于唤起状态选择弹窗
 */
@Composable
fun StatusBadge(
    status: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val badgeInfo = getStatusBadgeInfo(status) ?: return
    val (labelResId, backgroundColor) = badgeInfo

    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(labelResId),
            color = Color(0xFF333333),  // 深灰色文字，在马卡龙浅色背景上更清晰
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AnimeCard(
    anime: AnimeEntity,
    onClick: () -> Unit,
    onStatusChange: (Int) -> Unit,
    onRemove: () -> Unit,
    showStatusBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // 底部弹窗状态
    var showStatusDial by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = {
                            // 触发震动反馈
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            showStatusDial = true
                        }
                    )
                },
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column {
                // 封面图片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    GlideImage(
                        model = anime.imageUrl,
                        contentDescription = anime.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = placeholder(R.drawable.placeholder_cover),
                        failure = placeholder(R.drawable.placeholder_cover)
                    )

                    // 状态角标（仅在「全部」Tab 中显示）
                    if (showStatusBadge) {
                        StatusBadge(
                            status = anime.collectionStatus,
                            onClick = {
                                // 点击角标唤起底部弹窗
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                showStatusDial = true
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }

                    // 进度条（如果有总集数）
                    if (anime.totalEpisodes > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${anime.watchedEpisodes}/${anime.totalEpisodes}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                                LinearProgressIndicator(
                                    progress = { anime.watchedEpisodes.toFloat() / anime.totalEpisodes },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }

                // 标题（中文名优先，完整展示）
                Text(
                    text = anime.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // 底部弹窗
    if (showStatusDial) {
        StatusDialBottomSheet(
            anime = anime,
            onStatusSelected = { newStatus ->
                if (newStatus == -1) {
                    // 取消收藏需要二次确认
                    showRemoveDialog = true
                } else {
                    onStatusChange(newStatus)
                }
                showStatusDial = false
            },
            onDismiss = { showStatusDial = false }
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.collection_remove_confirm_title)) },
            text = { Text(stringResource(R.string.collection_remove_confirm_message, anime.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    }
                ) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
