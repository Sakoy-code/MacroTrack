package com.example.macrotrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.macrotrack.data.local.entities.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {

    @Insert
    suspend fun insert(stats: UserStatsEntity)

    /** La derniere pesee connue (pour connaitre le BMR actuel). */
    @Query("SELECT * FROM user_stats ORDER BY epochDay DESC LIMIT 1")
    suspend fun getLatest(): UserStatsEntity?

    @Query("SELECT * FROM user_stats ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<UserStatsEntity?>

    /** Tout l'historique de poids, pour tracer une courbe si besoin. */
    @Query("SELECT * FROM user_stats ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<UserStatsEntity>>
}
