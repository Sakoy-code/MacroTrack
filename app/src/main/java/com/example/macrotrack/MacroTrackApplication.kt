package com.example.macrotrack

import android.app.Application
import com.example.macrotrack.data.repository.MacroRepository

class MacroTrackApplication : Application() {

    val repository: MacroRepository by lazy { MacroRepository.getInstance(this) }
}
