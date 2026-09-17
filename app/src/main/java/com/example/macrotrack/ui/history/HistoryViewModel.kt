package com.example.macrotrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.macrotrack.data.local.entities.DailyGoalEntity
import com.example.macrotrack.data.local.entities.MealEntity
import com.example.macrotrack.data.repository.MacroRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DayHistory(
    val epochDay: Long,
    val goal: DailyGoalEntity?,
    val meals: List<MealEntity>
) {
    val totalCalories: Int get() = meals.sumOf { it.calories }
    val totalProtein: Int get() = meals.sumOf { it.proteinG }
    val totalCarbs: Int get() = meals.sumOf { it.carbsG }
    val totalFat: Int get() = meals.sumOf { it.fatG }
}

data class HistoryUiState(
    val days: List<DayHistory> = emptyList()
)

class HistoryViewModel(private val repository: MacroRepository) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAllGoals(),
        repository.observeAllMeals()
    ) { goals, meals ->
        val mealsByDay = meals.groupBy { it.epochDay }
        val allDays = (goals.map { it.epochDay } + mealsByDay.keys).toSortedSet(compareByDescending { it })

        val days = allDays.map { epochDay ->
            DayHistory(
                epochDay = epochDay,
                goal = goals.firstOrNull { it.epochDay == epochDay },
                meals = mealsByDay[epochDay].orEmpty().sortedByDescending { it.timestampMillis }
            )
        }
        HistoryUiState(days)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )
}
