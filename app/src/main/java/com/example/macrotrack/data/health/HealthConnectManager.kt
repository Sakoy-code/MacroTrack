package com.example.macrotrack.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

/**
 * Mi Fitness n'expose pas d'API publique : la seule voie officielle est
 * Mi Fitness -> Google Health Connect -> notre app lit Health Connect.
 * L'utilisateur doit activer la synchronisation dans l'app Mi Fitness
 * (Profil -> Compte et confidentialite -> Synchro avec Health Connect).
 */
sealed class HealthConnectAvailability {
    data object Available : HealthConnectAvailability()
    data object NotInstalled : HealthConnectAvailability()
    data object NotSupported : HealthConnectAvailability()
}

class HealthConnectManager(private val context: Context) {

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    fun availability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NotInstalled
            else -> HealthConnectAvailability.NotSupported
        }
    }

    private val client: HealthConnectClient? by lazy {
        if (availability() is HealthConnectAvailability.Available) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return permissions.all { it in granted }
    }

    /**
     * Depense active (kcal) pour la journee donnee (locale), agregee depuis
     * les calories actives brulees synchronisees par Mi Fitness. On tombe
     * en fallback sur les calories totales - BMR si l'appareil ne remonte
     * que la depense totale.
     */
    suspend fun getActiveCaloriesForDay(day: LocalDate, fallbackBmr: Float): Float {
        val c = client ?: return 0f
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        val timeRange = TimeRangeFilter.between(start, end)

        return try {
            val aggregate = c.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = timeRange
                )
            )
            val active = aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            active?.inKilocalories?.toFloat() ?: getActiveFromTotal(c, timeRange, fallbackBmr)
        } catch (e: Exception) {
            0f
        }
    }

    private suspend fun getActiveFromTotal(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter,
        fallbackBmr: Float
    ): Float {
        return try {
            val aggregate = client.aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = timeRange
                )
            )
            val total = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toFloat() ?: 0f
            (total - fallbackBmr).coerceAtLeast(0f)
        } catch (e: Exception) {
            0f
        }
    }

    suspend fun getStepsForDay(day: LocalDate): Long {
        val c = client ?: return 0L
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        return try {
            val aggregate = c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            aggregate[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

// Alias local pour eviter d'importer HealthPermission un peu partout
private typealias HealthPermission = androidx.health.connect.client.permission.HealthPermission
