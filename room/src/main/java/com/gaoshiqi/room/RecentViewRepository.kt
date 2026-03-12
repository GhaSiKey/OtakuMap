package com.gaoshiqi.room

import android.content.Context
import kotlinx.coroutines.flow.Flow

class RecentViewRepository(context: Context) {

    companion object {
        const val MAX_RECENT_COUNT = 20
    }

    private val recentViewDao = AppDatabase.getDatabase(context).recentViewDao()

    val allRecentViews: Flow<List<RecentViewEntity>> =
        recentViewDao.getRecent(MAX_RECENT_COUNT)

    suspend fun addRecentView(entity: RecentViewEntity) {
        recentViewDao.insert(entity)
        recentViewDao.trimToLimit(MAX_RECENT_COUNT)
    }

    suspend fun clearAll() {
        recentViewDao.clearAll()
    }
}
