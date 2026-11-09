package com.example.savingsgoal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.savingsgoal.data.Goal
import com.example.savingsgoal.data.SavingsCalculations
import com.example.savingsgoal.ui.theme.DepositGreen
import com.example.savingsgoal.ui.theme.ProgressEmerald
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var vr: Float,
    var width: Float,
    var height: Float,
    val color: Color,
    val isCircle: Boolean
)

@Composable
fun ConfettiCelebrationDialog(
    goal: Goal,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    var triggerBurst by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background Canvas Confetti Layer
            ConfettiCanvas(triggerBurst = triggerBurst)

            // Foreground Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trophy / Award Badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🎉 Goal Completed!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProgressEmerald
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Congratulations! You have reached your target for",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = goal.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Saved Amount Pill
                    Box(
                        modifier = Modifier
                            .background(
                                DepositGreen.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Total Saved: ${SavingsCalculations.formatCurrency(goal.totalSaved, currencySymbol)}",
                            fontWeight = FontWeight.Bold,
                            color = DepositGreen,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { triggerBurst++ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("More Confetti")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ProgressEmerald)
                        ) {
                            Text("Awesome!")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfettiCanvas(
    triggerBurst: Int,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val colors = listOf(
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Gold
        Color(0xFF3B82F6), // Blue
        Color(0xFFEC4899), // Pink
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEF4444)  // Red
    )

    // Trigger burst on start or when triggerBurst changes
    LaunchedEffect(triggerBurst) {
        val count = 80
        val newParticles = (0 until count).map {
            val angle = Random.nextDouble(Math.PI * 0.2, Math.PI * 0.8) // Upward burst
            val speed = Random.nextDouble(400.0, 900.0).toFloat()
            ConfettiParticle(
                x = 540f + Random.nextInt(-150, 150),
                y = 800f,
                vx = (-cos(angle) * speed).toFloat(),
                vy = (-sin(angle) * speed).toFloat(),
                rotation = Random.nextFloat() * 360f,
                vr = Random.nextFloat() * 400f - 200f,
                width = Random.nextFloat() * 12f + 8f,
                height = Random.nextFloat() * 18f + 10f,
                color = colors.random(),
                isCircle = Random.nextBoolean()
            )
        }
        particles.clear()
        particles.addAll(newParticles)
    }

    // Animation physics loop
    LaunchedEffect(particles.size) {
        var lastTime = 0L
        while (particles.isNotEmpty()) {
            withFrameNanos { time ->
                if (lastTime != 0L) {
                    val dt = ((time - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    val gravity = 980f

                    for (p in particles) {
                        p.vy += gravity * dt
                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        p.rotation += p.vr * dt
                        // Air drag
                        p.vx *= 0.985f
                    }

                    // Remove offscreen
                    particles.removeAll { it.y > 2400f }
                }
                lastTime = time
            }
        }
    }

    Canvas(modifier = modifier) {
        for (p in particles) {
            rotate(p.rotation, pivot = Offset(p.x + p.width / 2, p.y + p.height / 2)) {
                if (p.isCircle) {
                    drawCircle(
                        color = p.color,
                        radius = p.width / 2,
                        center = Offset(p.x, p.y)
                    )
                } else {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(p.x, p.y),
                        size = Size(p.width, p.height)
                    )
                }
            }
        }
    }
}

