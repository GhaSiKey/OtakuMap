package com.gaoshiqi.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentViewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentViewEntity)

    @Query("SELECT * FROM recent_views ORDER BY viewTime DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<RecentViewEntity>>

    @Query("DELETE FROM recent_views WHERE id NOT IN (SELECT id FROM recent_views ORDER BY viewTime DESC LIMIT :limit)")
    suspend fun trimToLimit(limit: Int = 20)

    @Query("DELETE FROM recent_views")
    suspend fun clearAll()
}
