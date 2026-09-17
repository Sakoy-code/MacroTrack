package com.example.macrotrack.util

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

object DateUtils {

    /** Identifiant unique de semaine ISO, ex: "2026-W38". Sert a savoir si on a
     * deja demande la pesee de la semaine en cours. */
    fun weekId(date: LocalDate): String {
        val weekFields = WeekFields.ISO
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.get(weekFields.weekBasedYear())
        return String.format(Locale.US, "%d-W%02d", year, week)
    }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
}
