package com.example.macrotrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.macrotrack.data.local.entities.DailyGoalEntity
import com.example.macrotrack.data.local.entities.MealEntity
import com.example.macrotrack.data.repository.MacroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val goal: DailyGoalEntity? = null,
    val meals: List<MealEntity> = emptyList(),
    val showWeighInSheet: Boolean = false,
    val isLoadingGoal: Boolean = true
) {
    val consumedCalories: Int get() = meals.sumOf { it.calories }
    val consumedProtein: Int get() = meals.sumOf { it.proteinG }
    val consumedCarbs: Int get() = meals.sumOf { it.carbsG }
    val consumedFat: Int get() = meals.sumOf { it.fatG }

    val remainingProtein: Int get() = (goal?.targetProteinG ?: 0) - consumedProtein
    val remainingCarbs: Int get() = (goal?.targetCarbsG ?: 0) - consumedCarbs
    val remainingFat: Int get() = (goal?.targetFatG ?: 0) - consumedFat
    val remainingCalories: Int get() = (goal?.targetCalories ?: 0) - consumedCalories
}

class DashboardViewModel(private val repository: MacroRepository) : ViewModel() {

    private val _showWeighIn = MutableStateFlow(false)
    private val _isLoadingGoal = MutableStateFlow(true)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeTodayGoal(),
        repository.observeTodayMeals(),
        _showWeighIn,
        _isLoadingGoal
    ) { goal, meals, showWeighIn, isLoading ->
        DashboardUiState(goal, meals, showWeighIn, isLoading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        refresh()
    }

    /** A appeler au demarrage de l'ecran et quand l'app revient au premier plan. */
    fun refresh() {
        viewModelScope.launch {
            _isLoadingGoal.value = true
            _showWeighIn.value = repository.needsWeighIn()

            val latestStats = repository.latestUserStats()
            if (latestStats != null && !_showWeighIn.value) {
                repository.refreshTodayGoal(latestStats.weightKg)
            }
            _isLoadingGoal.value = false
        }
    }

    fun dismissWeighInSheet() {
        _showWeighIn.value = false
    }

    fun submitWeighIn(weightKg: Float, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val profile = repository.prefs.profileFlow.first()
            repository.recordWeighIn(weightKg, profile.heightCm, profile.age, profile.isMale)
            _showWeighIn.value = false
            onDone()
        }
    }

    /** Texte dynamique de rattrapage ("il te manque Xg de proteines..."). */
    fun catchUpAdvice(state: DashboardUiState): String? {
        val protMissing = state.remainingProtein
        val fatMissing = state.remainingFat
        val carbsMissing = state.remainingCarbs

        return when {
            protMissing > 15 ->
                "Il te manque encore ${protMissing}g de proteines aujourd'hui : un shaker de whey ou du fromage blanc t'aiderait a atteindre l'objectif."
            fatMissing > 10 ->
                "Il te reste ${fatMissing}g de lipides a combler : une poignee d'amandes ou un filet d'huile d'olive feraient l'affaire."
            carbsMissing > 30 ->
                "Il te reste ${carbsMissing}g de glucides : du riz, des patates douces ou des fruits pour finir la journee."
            else -> null
        }
    }
}
