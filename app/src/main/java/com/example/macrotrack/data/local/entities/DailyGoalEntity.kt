package com.example.macrotrack.data.local.entities

import androidx.room.Entity

/**
 * Objectifs du jour, recalcules chaque matin a partir du dernier BMR connu
 * + de la depense active recuperee via Health Connect (Mi Fitness).
 * epochDay est la cle primaire : un seul objectif par jour.
 */
@Entity(tableName = "daily_goal", primaryKeys = ["epochDay"])
data class DailyGoalEntity(
    val epochDay: Long,
    val bmr: Float,
    val activeCalories: Float,   // depense active du jour (Health Connect / Mi Fitness)
    val tdee: Float,             // BMR + activeCalories
    val targetCalories: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int
)
