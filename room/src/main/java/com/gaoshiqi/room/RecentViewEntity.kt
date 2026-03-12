package com.gaoshiqi.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_views")
data class RecentViewEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val nameCn: String,
    val imageUrl: String,
    val score: Double,
    val viewTime: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = nameCn.ifBlank { name }
}
