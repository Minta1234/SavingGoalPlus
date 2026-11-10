package com.example.savingsgoal.ui.components

import androidx.compose.ui.res.stringResource
import com.example.savingsgoal.R
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savingsgoal.data.Goal
import com.example.savingsgoal.data.SavingsCalculations
import com.example.savingsgoal.data.SavingsRatePlan
import com.example.savingsgoal.ui.theme.ProgressAmber
import com.example.savingsgoal.ui.theme.ProgressCyan
import com.example.savingsgoal.ui.theme.ProgressEmerald
import java.io.File

@Composable
fun GoalCard(
    goal: Goal,
    defaultCurrency: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
    onSaveToday: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val prefs = LocalContext.current.getSharedPreferences("goal_card_colors", Context.MODE_PRIVATE)
    val colorKey = goal.id.toString()
    var cardColor by remember(colorKey) {
        mutableStateOf(if (prefs.contains(colorKey)) prefs.getInt(colorKey, 0) else null)
    }
    val surface = MaterialTheme.colorScheme.surface
    val background = cardColor?.let { Color(it).copy(alpha = 0.18f).compositeOver(surface) } ?: surface
    val currency = if (goal.currency.isNullOrBlank() || goal.currency == "null") defaultCurrency else goal.currency

    val animatedProgress by animateFloatAsState(
        targetValue = goal.progressFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "goal_progress"
    )

    val progressColor = when {
        goal.progressPercent <= 30 -> ProgressAmber
        goal.progressPercent <= 60 -> ProgressCyan
        else -> ProgressEmerald
    }

    val ratePlan = remember(goal.date, goal.remainingAmount, goal.isReached) {
        SavingsCalculations.calculateRatePlan(goal.date, goal.remainingAmount, goal.isReached)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Goal Image / Thumbnail
                val imageBitmap = remember(goal.imagePath) {
                    goal.imagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
                    }
                }

                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = goal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(progressColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = goal.name.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (goal.isReached) stringResource(R.string.ui_goal_achieved) else stringResource(R.string.target_amount_value, SavingsCalculations.formatCurrency(goal.amount, currency)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Options Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.ui_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ui_edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ui_card_color)) },
                            leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showColorPicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArchived) stringResource(R.string.ui_unarchive) else stringResource(R.string.ui_archive)) },
                            leadingIcon = {
                                Icon(
                                    if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ui_delete), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Metrics Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SavingsCalculations.formatCurrency(goal.totalSaved, currency),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = progressColor
                    )
                    Text(
                        text = stringResource(R.string.saved_percent, goal.progressPercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (ratePlan != null) {
                    Text(
                        text = ratePlan.localizedDeadlineText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ratePlan.isExpired && !ratePlan.isReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showColorPicker) {
        ColorChoiceDialog(
            title = stringResource(R.string.ui_card_color),
            initialColor = cardColor ?: MaterialTheme.colorScheme.primary.toArgb(),
            onDismiss = { showColorPicker = false },
            onSave = { color ->
                val editor = prefs.edit()
                if (color == null) editor.remove(colorKey) else editor.putInt(colorKey, color)
                editor.apply()
                cardColor = color
                showColorPicker = false
            }
        )
    }
}

@Composable
internal fun SavingsRatePlan.localizedDeadlineText(): String {
    if (LocalResources.current.configuration.locales[0].language != "th") return formattedTimeToGo
    return when {
        isReached -> stringResource(R.string.ui_goal_achieved)
        isExpired -> stringResource(R.string.goal_deadline_passed)
        daysToGo == 0L -> stringResource(R.string.ui_today)
        else -> stringResource(R.string.goal_deadline_remaining, daysToGo)
    }
}
