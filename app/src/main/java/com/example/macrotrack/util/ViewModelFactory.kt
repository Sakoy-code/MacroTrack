package com.example.macrotrack.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.macrotrack.data.remote.GeminiRepository
import com.example.macrotrack.data.repository.MacroRepository
import com.example.macrotrack.ui.camera.MealCaptureViewModel
import com.example.macrotrack.ui.dashboard.DashboardViewModel
import com.example.macrotrack.ui.history.HistoryViewModel
import com.example.macrotrack.ui.onboarding.OnboardingViewModel

/**
 * Petite factory maison (pas besoin de Hilt/Dagger pour ce projet) qui
 * fournit le MacroRepository (Room + DataStore + Health Connect) et le
 * GeminiRepository a chaque ViewModel.
 */
class AppViewModelFactory(private val repository: MacroRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
            OnboardingViewModel(repository) as T

        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(repository) as T

        modelClass.isAssignableFrom(MealCaptureViewModel::class.java) ->
            MealCaptureViewModel(repository, GeminiRepository.default()) as T

        modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
            HistoryViewModel(repository) as T

        else -> throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
    }
}
