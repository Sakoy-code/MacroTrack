package com.example.macrotrack.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.macrotrack.data.repository.MacroRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(private val repository: MacroRepository) : ViewModel() {

    fun completeOnboarding(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.prefs.saveProfile(heightCm, age, isMale)
            repository.recordWeighIn(weightKg, heightCm, age, isMale)
            onDone()
        }
    }
}
