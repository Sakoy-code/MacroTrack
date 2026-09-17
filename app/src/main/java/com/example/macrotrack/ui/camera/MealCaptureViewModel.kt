package com.example.macrotrack.ui.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.macrotrack.data.local.entities.MealEntity
import com.example.macrotrack.data.remote.GeminiAnalysisException
import com.example.macrotrack.data.remote.GeminiRepository
import com.example.macrotrack.data.remote.MealAnalysisResult
import com.example.macrotrack.data.repository.MacroRepository
import com.example.macrotrack.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class MealCaptureState {
    data object Idle : MealCaptureState()
    data object Analyzing : MealCaptureState()
    data class Result(val analysis: MealAnalysisResult, val photoPath: String, val description: String) : MealCaptureState()
    data class Error(val message: String) : MealCaptureState()
    data object Saved : MealCaptureState()
}

class MealCaptureViewModel(
    private val repository: MacroRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MealCaptureState>(MealCaptureState.Idle)
    val state: StateFlow<MealCaptureState> = _state.asStateFlow()

    fun analyze(photoBitmap: Bitmap, photoPath: String, description: String) {
        viewModelScope.launch {
            _state.value = MealCaptureState.Analyzing
            try {
                val model = repository.prefs.geminiModelFlow.first()
                val result = geminiRepository.analyzeMeal(photoBitmap, description, model)
                _state.value = MealCaptureState.Result(result, photoPath, description)
            } catch (e: GeminiAnalysisException) {
                _state.value = MealCaptureState.Error(e.message ?: "Erreur inconnue")
            } catch (e: Exception) {
                _state.value = MealCaptureState.Error("Erreur reseau : ${e.message}")
            }
        }
    }

    fun confirmSave(analysis: MealAnalysisResult, photoPath: String, description: String) {
        viewModelScope.launch {
            val today = DateUtils.todayEpochDay()
            repository.addMeal(
                MealEntity(
                    epochDay = today,
                    timestampMillis = System.currentTimeMillis(),
                    photoPath = photoPath,
                    description = description,
                    calories = analysis.calories,
                    proteinG = analysis.proteinG,
                    carbsG = analysis.carbsG,
                    fatG = analysis.fatG
                )
            )
            _state.value = MealCaptureState.Saved
        }
    }

    fun reset() {
        _state.value = MealCaptureState.Idle
    }
}
