package com.example.macrotrack.data.local.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.Query
import com.example.macrotrack.data.local.entities.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goal WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getForDay(epochDay: Long): DailyGoalEntity?

    @Query("SELECT * FROM daily_goal WHERE epochDay = :epochDay LIMIT 1")
    fun observeForDay(epochDay: Long): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goal ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<DailyGoalEntity>>
}
