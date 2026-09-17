package com.example.macrotrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.macrotrack.data.datastore.UserProfile
import com.example.macrotrack.ui.navigation.MacroNavHost
import com.example.macrotrack.ui.theme.MacroTrackTheme
import com.example.macrotrack.util.AppViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as MacroTrackApplication).repository
        val factory = AppViewModelFactory(repository)

        setContent {
            MacroTrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val healthConnectLauncher = rememberLauncherForActivityResult(
                        repository.healthConnect.requestPermissionsContract()
                    ) { /* granted set, rien de plus a faire : les lectures suivantes en profiteront */ }

                    // Demande les permissions Health Connect (donnees Mi Fitness) une fois
                    // que l'app est prete, si elles ne sont pas encore accordees.
                    LaunchedEffect(Unit) {
                        launch {
                            if (repository.healthConnect.availability() is
                                com.example.macrotrack.data.health.HealthConnectAvailability.Available &&
                                !repository.healthConnect.hasAllPermissions()
                            ) {
                                healthConnectLauncher.launch(repository.healthConnect.permissions)
                            }
                        }
                    }

                    val profile by repository.prefs.profileFlow.collectAsState(
                        initial = UserProfile(heightCm = 175f, age = 30, isMale = true, onboardingDone = false)
                    )

                    MacroNavHost(factory = factory, profile = profile)
                }
            }
        }
    }
}
