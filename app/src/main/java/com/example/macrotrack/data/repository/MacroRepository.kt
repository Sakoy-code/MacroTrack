package com.example.macrotrack.data.repository

import android.content.Context
import com.example.macrotrack.data.datastore.UserPreferences
import com.example.macrotrack.data.health.HealthConnectManager
import com.example.macrotrack.data.local.AppDatabase
import com.example.macrotrack.data.local.entities.DailyGoalEntity
import com.example.macrotrack.data.local.entities.MealEntity
import com.example.macrotrack.data.local.entities.UserStatsEntity
import com.example.macrotrack.domain.BmrCalculator
import com.example.macrotrack.domain.MacroCalculator
import com.example.macrotrack.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Point d'entree unique pour les ViewModels : combine la base Room (memoire
 * long terme), les preferences (profil + suivi hebdo) et Health Connect
 * (donnees Mi Fitness du jour).
 */
class MacroRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    val userStatsDao = db.userStatsDao()
    val dailyGoalDao = db.dailyGoalDao()
    val mealDao = db.mealDao()

    val prefs = UserPreferences(context)
    val healthConnect = HealthConnectManager(context)

    /** True si on doit proposer la pesee (nouvelle semaine ISO non encore renseignee). */
    suspend fun needsWeighIn(): Boolean {
        val currentWeek = DateUtils.weekId(LocalDate.now())
        val lastWeek = prefs.getLastWeighInWeekId()
        return lastWeek != currentWeek
    }

    /**
     * Enregistre la pesee du lundi : recalcule le BMR avec le nouveau poids et
     * marque la semaine courante comme "faite".
     */
    suspend fun recordWeighIn(weightKg: Float, heightCm: Float, age: Int, isMale: Boolean) {
        val bmr = BmrCalculator.calculate(weightKg, heightCm, age, isMale)
        val today = LocalDate.now()
        userStatsDao.insert(
            UserStatsEntity(
                epochDay = today.toEpochDay(),
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                isMale = isMale,
                bmr = bmr
            )
        )
        prefs.setLastWeighIn(DateUtils.weekId(today), today.toEpochDay())
        // On recalcule tout de suite l'objectif du jour avec le nouveau BMR.
        refreshTodayGoal(weightKg)
    }

    /**
     * Recalcule l'objectif du jour : BMR le plus recent + depense active
     * remontee par Health Connect (Mi Fitness), puis sauvegarde en base pour
     * l'historique.
     */
    suspend fun refreshTodayGoal(currentWeightKg: Float): DailyGoalEntity? {
        val latestStats = userStatsDao.getLatest() ?: return null
        val today = LocalDate.now()

        val activeCalories = try {
            healthConnect.getActiveCaloriesForDay(today, latestStats.bmr)
        } catch (e: Exception) {
            0f
        }

        val goals = MacroCalculator.calculate(latestStats.bmr, activeCalories, currentWeightKg)
        val entity = DailyGoalEntity(
            epochDay = today.toEpochDay(),
            bmr = latestStats.bmr,
            activeCalories = activeCalories,
            tdee = goals.tdee,
            targetCalories = goals.targetCalories,
            targetProteinG = goals.targetProteinG,
            targetCarbsG = goals.targetCarbsG,
            targetFatG = goals.targetFatG
        )
        dailyGoalDao.upsert(entity)
        return entity
    }

    fun observeTodayGoal(): Flow<DailyGoalEntity?> = dailyGoalDao.observeForDay(DateUtils.todayEpochDay())

    fun observeTodayMeals(): Flow<List<MealEntity>> = mealDao.observeForDay(DateUtils.todayEpochDay())

    fun observeAllGoals(): Flow<List<DailyGoalEntity>> = dailyGoalDao.observeAll()

    fun observeAllMeals(): Flow<List<MealEntity>> = mealDao.observeAll()

    suspend fun addMeal(meal: MealEntity) = mealDao.insert(meal)

    suspend fun latestUserStats(): UserStatsEntity? = userStatsDao.getLatest()

    companion object {
        @Volatile
        private var INSTANCE: MacroRepository? = null

        fun getInstance(context: Context): MacroRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MacroRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
