package com.example.savingsgoal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.savingsgoal.MainActivity
import com.example.savingsgoal.R
import com.example.savingsgoal.data.SavingsCalculations
import com.example.savingsgoal.data.SavingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavingsGoalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repository = SavingsRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            val goals = repository.getActiveGoals()
            val primaryGoal = goals.firstOrNull()
            val currency = repository.getDefaultCurrency()

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_savings_goal)

                if (primaryGoal != null) {
                    val goalCur = if (primaryGoal.currency.isNullOrBlank() || primaryGoal.currency == "null") currency else primaryGoal.currency
                    views.setTextViewText(R.id.widget_goal_name, primaryGoal.name)
                    views.setTextViewText(R.id.widget_goal_percent, "${primaryGoal.progressPercent}%")
                    views.setProgressBar(R.id.widget_progress_bar, 100, primaryGoal.progressPercent, false)
                    views.setTextViewText(R.id.widget_saved_amount, "Saved: ${SavingsCalculations.formatCurrency(primaryGoal.totalSaved, goalCur)}")
                    views.setTextViewText(R.id.widget_target_amount, "Target: ${SavingsCalculations.formatCurrency(primaryGoal.amount, goalCur)}")
                } else {
                    views.setTextViewText(R.id.widget_goal_name, "No Goals Yet")
                    views.setTextViewText(R.id.widget_goal_percent, "0%")
                    views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                    views.setTextViewText(R.id.widget_saved_amount, "Tap to add a goal")
                    views.setTextViewText(R.id.widget_target_amount, "")
                }

                // Open App on click
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}

