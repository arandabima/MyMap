package com.rs.mymap.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteHistoryDao {
    @Query("SELECT * FROM route_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<RouteHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routeHistory: RouteHistory)

    @Delete
    suspend fun delete(routeHistory: RouteHistory)

    @Update
    suspend fun update(routeHistory: RouteHistory)

    @Query("UPDATE route_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)
}