package com.example.macrotrack.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "macrotrack_prefs")

/**
 * Profil declare une seule fois (onboarding) puis modifiable dans les reglages.
 * Sert au calcul du BMR (taille/age/sexe) et au suivi de la pesee hebdomadaire.
 */
data class UserProfile(
    val heightCm: Float,
    val age: Int,
    val isMale: Boolean,
    val onboardingDone: Boolean
)

class UserPreferences(private val context: Context) {

    private object Keys {
        val HEIGHT = floatPreferencesKey("height_cm")
        val AGE = intPreferencesKey("age")
        val IS_MALE = booleanPreferencesKey("is_male")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_WEIGH_IN_WEEK_ID = stringPreferencesKey("last_weigh_in_week_id") // format "2026-W07"
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val LAST_WEIGH_IN_EPOCH_DAY = longPreferencesKey("last_weigh_in_epoch_day")
    }

    val profileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            heightCm = prefs[Keys.HEIGHT] ?: 175f,
            age = prefs[Keys.AGE] ?: 30,
            isMale = prefs[Keys.IS_MALE] ?: true,
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false
        )
    }

    suspend fun saveProfile(heightCm: Float, age: Int, isMale: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HEIGHT] = heightCm
            prefs[Keys.AGE] = age
            prefs[Keys.IS_MALE] = isMale
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    val lastWeighInWeekIdFlow: Flow<String?> =
        context.dataStore.data.map { it[Keys.LAST_WEIGH_IN_WEEK_ID] }

    suspend fun getLastWeighInWeekId(): String? = lastWeighInWeekIdFlow.first()

    suspend fun setLastWeighIn(weekId: String, epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_WEIGH_IN_WEEK_ID] = weekId
            prefs[Keys.LAST_WEIGH_IN_EPOCH_DAY] = epochDay
        }
    }

    /** Nom du modele Gemini utilise (modifiable si Google en deprecie un). */
    val geminiModelFlow: Flow<String> = context.dataStore.data.map {
        it[Keys.GEMINI_MODEL] ?: "gemini-flash-latest"
    }

    suspend fun setGeminiModel(model: String) {
        context.dataStore.edit { it[Keys.GEMINI_MODEL] = model }
    }
}
