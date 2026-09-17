package com.example.macrotrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.macrotrack.data.local.entities.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Insert
    suspend fun insert(meal: MealEntity): Long

    @Query("SELECT * FROM meal WHERE epochDay = :epochDay ORDER BY timestampMillis ASC")
    fun observeForDay(epochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<MealEntity>>

    @Query("SELECT DISTINCT epochDay FROM meal ORDER BY epochDay DESC")
    fun observeDaysWithMeals(): Flow<List<Long>>
}
