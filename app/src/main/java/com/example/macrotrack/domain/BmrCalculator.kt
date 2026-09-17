package com.example.macrotrack.domain

/**
 * Formule de Mifflin-St Jeor - la plus fiable pour un adulte en bonne sante,
 * homme ou femme. Le resultat est le metabolisme de base (BMR), c'est a dire
 * la depense au repos strict, AVANT d'ajouter l'activite du jour.
 */
object BmrCalculator {

    fun calculate(weightKg: Float, heightCm: Float, age: Int, isMale: Boolean): Float {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * age
        return if (isMale) base + 5f else base - 161f
    }
}
