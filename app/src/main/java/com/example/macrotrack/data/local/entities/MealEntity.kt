package com.example.macrotrack.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un repas enregistre : photo + description envoyees a Gemini, et le
 * resultat (calories/macros) que Gemini a renvoye.
 */
@Entity(tableName = "meal")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val epochDay: Long,          // jour auquel le repas est rattache (pour les totaux quotidiens)
    val timestampMillis: Long,   // heure precise, pour trier/afficher
    val photoPath: String,       // chemin local de la photo (filesDir)
    val description: String,     // description libre saisie par l'utilisateur
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)
