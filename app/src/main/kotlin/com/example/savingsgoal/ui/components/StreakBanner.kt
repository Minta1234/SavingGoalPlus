package com.example.savingsgoal.ui.components

import androidx.compose.ui.res.stringResource
import com.example.savingsgoal.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savingsgoal.data.StreakInfo

private val StreakOrange = Color(0xFFF88C31)

@Composable
fun StreakBanner(
    streakInfo: StreakInfo,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val flameColor = if (streakInfo.currentStreak > 0) StreakOrange else MaterialTheme.colorScheme.onSurfaceVariant
    val status = when {
        streakInfo.isStreakActiveToday -> stringResource(R.string.ui_saved_today)
        streakInfo.currentStreak > 0 -> stringResource(R.string.ui_save_today)
        else -> stringResource(R.string.ui_start_saving)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.ui_view_streak_details)) { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = flameColor)
        Text(
            text = stringResource(R.string.streak_days, streakInfo.currentStreak),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
    }

    if (showDialog) {
        StreakMilestonesDialog(
            streakInfo = streakInfo,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun StreakMilestonesDialog(
    streakInfo: StreakInfo,
    onDismiss: () -> Unit
) {
    val tiers = listOf(
        Triple("👑 Savings Titan", R.string.streak_tier_0, R.string.streak_tier_range_0),
        Triple("💎 Month Master", R.string.streak_tier_1, R.string.streak_tier_range_1),
        Triple("🏆 Fortnight Legend", R.string.streak_tier_2, R.string.streak_tier_range_2),
        Triple("⚡ Week Champion", R.string.streak_tier_3, R.string.streak_tier_range_3),
        Triple("🔥 Warming Up", R.string.streak_tier_4, R.string.streak_tier_range_4),
        Triple("🌱 Habit Spark", R.string.streak_tier_5, R.string.streak_tier_range_5)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ui_savings_streak),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.streak_days, streakInfo.currentStreak),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        streakInfo.isStreakActiveToday -> stringResource(R.string.streak_done_today)
                        streakInfo.currentStreak > 0 -> stringResource(R.string.streak_keep_going, streakInfo.currentStreak)
                        else -> stringResource(R.string.streak_first_deposit)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(stringResource(R.string.ui_last_7_days), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    streakInfo.recentDays.forEach { day ->
                        val label = if (day.isToday) stringResource(R.string.ui_today) else day.description
                        val status = stringResource(if (day.saved) R.string.ui_saved else R.string.ui_no_deposit)
                        val dayDescription = stringResource(R.string.streak_day_status, label, status)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = dayDescription
                            }
                        ) {
                            Text(day.label, style = MaterialTheme.typography.labelSmall)
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape)
                                    .background(if (day.saved) StreakOrange else MaterialTheme.colorScheme.surfaceVariant)
                                    .then(if (day.isToday) Modifier.border(2.dp, StreakOrange, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day.saved) Icon(Icons.Default.Check, null, tint = Color(0xFF2B1808), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StreakStatItem(title = stringResource(R.string.ui_current), value = stringResource(R.string.streak_count_days, streakInfo.currentStreak))
                    StreakStatItem(title = stringResource(R.string.ui_best), value = stringResource(R.string.streak_count_days, streakInfo.bestStreak))
                    StreakStatItem(title = stringResource(R.string.ui_total_days), value = stringResource(R.string.streak_count_days, streakInfo.totalDaysSaved))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.ui_motivational_tiers),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                tiers.forEach { (name, titleResource, requirementResource) ->
                    val isCurrentTier = streakInfo.levelTitle == name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isCurrentTier) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(titleResource),
                            fontWeight = if (isCurrentTier) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isCurrentTier) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(requirementResource),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_got_it))
            }
        }
    )
}

@Composable
private fun StreakStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

