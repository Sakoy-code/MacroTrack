package com.example.macrotrack.domain

import kotlin.math.roundToInt

data class MacroGoals(
    val tdee: Float,
    val targetCalories: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int
)

/**
 * Objectifs pour une recomposition corporelle (perte de gras + prise de muscle
 * en simultane) :
 *  - Deficit modere (200 a 300 kcal) pour ne pas sacrifier le muscle.
 *  - Proteines elevees : ~2 g/kg de poids de corps, pilier de la synthese musculaire.
 *  - Lipides : 0.8 a 1 g/kg pour la production hormonale.
 *  - Glucides : le reste des calories, pour l'energie et la performance a l'entrainement.
 */
object MacroCalculator {

    private const val DEFICIT_KCAL = 250f

    fun calculate(bmr: Float, activeCalories: Float, weightKg: Float): MacroGoals {
        val tdee = bmr + activeCalories
        val targetCalories = (tdee - DEFICIT_KCAL).coerceAtLeast(1200f)

        val proteinG = weightKg * 2f
        val fatG = weightKg * 0.9f

        val caloriesFromProtein = proteinG * 4f
        val caloriesFromFat = fatG * 9f
        val remainingForCarbs = (targetCalories - caloriesFromProtein - caloriesFromFat).coerceAtLeast(0f)
        val carbsG = remainingForCarbs / 4f

        return MacroGoals(
            tdee = tdee,
            targetCalories = targetCalories.roundToInt(),
            targetProteinG = proteinG.roundToInt(),
            targetCarbsG = carbsG.roundToInt(),
            targetFatG = fatG.roundToInt()
        )
    }
}
