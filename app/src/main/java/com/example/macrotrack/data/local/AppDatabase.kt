package com.example.macrotrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.macrotrack.data.local.dao.DailyGoalDao
import com.example.macrotrack.data.local.dao.MealDao
import com.example.macrotrack.data.local.dao.UserStatsDao
import com.example.macrotrack.data.local.entities.DailyGoalEntity
import com.example.macrotrack.data.local.entities.MealEntity
import com.example.macrotrack.data.local.entities.UserStatsEntity

@Database(
    entities = [UserStatsEntity::class, DailyGoalEntity::class, MealEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userStatsDao(): UserStatsDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "macrotrack.db"
                ).build().also { INSTANCE = it }
            }
    }
}
