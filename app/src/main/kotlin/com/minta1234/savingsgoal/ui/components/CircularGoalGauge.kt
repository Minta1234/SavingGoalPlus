package com.minta1234.savingsgoal.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minta1234.savingsgoal.ui.theme.ProgressAmber
import com.minta1234.savingsgoal.ui.theme.ProgressCyan
import com.minta1234.savingsgoal.ui.theme.ProgressEmerald

@Composable
fun CircularGoalGauge(
    progressPercent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    strokeWidth: Dp = 14.dp,
    subText: String = "SAVED"
) {
    val clampedPercent = progressPercent.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedPercent / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "gauge_progress"
    )

    val progressColor = when {
        clampedPercent <= 30 -> ProgressAmber
        clampedPercent <= 60 -> ProgressCyan
        else -> ProgressEmerald
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = size.toPx() - strokePx
            val topLeft = strokePx / 2f

            val startAngle = 140f
            val totalSweep = 260f

            // Background Track Arc
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Foreground Progress Arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = totalSweep * animatedProgress,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$clampedPercent%",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

