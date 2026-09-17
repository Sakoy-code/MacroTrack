package com.example.macrotrack.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macrotrack.data.local.entities.MealEntity
import com.example.macrotrack.ui.components.MacroRing
import com.example.macrotrack.ui.theme.Carbs
import com.example.macrotrack.ui.theme.Fat
import com.example.macrotrack.ui.theme.OverLimit
import com.example.macrotrack.ui.theme.Protein
import com.example.macrotrack.util.AppViewModelFactory

@Composable
fun DashboardScreen(factory: AppViewModelFactory) {
    val viewModel: DashboardViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.showWeighInSheet) {
        WeighInSheet(
            onDismiss = { viewModel.dismissWeighInSheet() },
            onConfirm = { weight -> viewModel.submitWeighIn(weight) }
        )
    }

    if (state.isLoadingGoal) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CaloriesCard(state) }
        item { MacrosCard(state) }
        val advice = viewModel.catchUpAdvice(state)
        if (advice != null) {
            item { AdviceCard(advice) }
        }
        item {
            Text(
                "Repas d'aujourd'hui",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (state.meals.isEmpty()) {
            item {
                Text(
                    "Aucun repas enregistre pour le moment. Utilise l'onglet Camera pour ajouter ton premier repas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.meals) { meal -> MealRow(meal) }
        }
    }
}

@Composable
private fun CaloriesCard(state: DashboardUiState) {
    val goal = state.goal
    val target = goal?.targetCalories ?: 0
    val consumed = state.consumedCalories
    val ratio = if (target > 0) consumed.toFloat() / target.toFloat() else 0f
    val isOver = consumed > target

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Calories aujourd'hui", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$consumed / $target kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOver) OverLimit else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { ratio.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (isOver) OverLimit else MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "TDEE estime : ${goal?.tdee?.toInt() ?: 0} kcal (BMR ${goal?.bmr?.toInt() ?: 0} + activite Mi Fitness ${goal?.activeCalories?.toInt() ?: 0})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacrosCard(state: DashboardUiState) {
    val goal = state.goal
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroRing("Proteines", state.consumedProtein, goal?.targetProteinG ?: 0, Protein)
            MacroRing("Glucides", state.consumedCarbs, goal?.targetCarbsG ?: 0, Carbs)
            MacroRing("Lipides", state.consumedFat, goal?.targetFatG ?: 0, Fat)
        }
    }
}

@Composable
private fun AdviceCard(advice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                text = advice,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun MealRow(meal: MealEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        meal.description.ifBlank { "Repas" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "P ${meal.proteinG}g  •  G ${meal.carbsG}g  •  L ${meal.fatG}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text("${meal.calories} kcal", style = MaterialTheme.typography.titleMedium)
        }
    }
}
