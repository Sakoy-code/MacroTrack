package com.example.macrotrack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.macrotrack.ui.theme.OverLimit

/**
 * Anneau circulaire pour une macro (proteines/glucides/lipides). Passe
 * automatiquement en rouge quand la consommation depasse l'objectif, comme
 * demande dans le cahier des charges.
 */
@Composable
fun MacroRing(
    label: String,
    consumed: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (target > 0) consumed.toFloat() / target.toFloat() else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceAtMost(1f),
        animationSpec = tween(durationMillis = 600),
        label = "macroRingProgress"
    )
    val isOver = consumed > target
    val ringColor = if (isOver) OverLimit else color

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(84.dp),
                color = ringColor.copy(alpha = 0.15f),
                strokeWidth = 8.dp,
            )
            CircularProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier.size(84.dp),
                color = ringColor,
                strokeWidth = 8.dp,
            )
            Text(
                text = "${consumed}g",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = "objectif ${target}g",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
