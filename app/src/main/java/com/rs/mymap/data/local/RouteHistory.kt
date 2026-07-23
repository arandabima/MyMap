package com.rs.mymap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_history")
data class RouteHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val origin: String,
    val destination: String,
    val mode: String,
    val distance: String,
    val duration: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)