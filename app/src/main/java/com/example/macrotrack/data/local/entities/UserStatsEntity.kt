package com.example.macrotrack.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une ligne = une pesee (en theorie chaque lundi matin).
 * Sert a recalculer le BMR et a tracer la courbe de poids dans le temps.
 */
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val epochDay: Long,       // jour (LocalDate.toEpochDay()) de la pesee
    val weightKg: Float,
    val heightCm: Float,
    val age: Int,
    val isMale: Boolean,
    val bmr: Float            // Mifflin-St Jeor calcule a partir des champs ci-dessus
)
