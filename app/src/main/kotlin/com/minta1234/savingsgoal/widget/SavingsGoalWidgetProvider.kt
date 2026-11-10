package com.minta1234.savingsgoal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.minta1234.savingsgoal.MainActivity
import com.minta1234.savingsgoal.R
import com.minta1234.savingsgoal.data.Goal
import com.minta1234.savingsgoal.data.SavingsCalculations
import com.minta1234.savingsgoal.data.SavingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SavingsGoalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            for (widgetId in appWidgetIds) {
                updateSingleWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (action == ACTION_NEXT_GOAL || action == ACTION_PREV_GOAL) {
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

            CoroutineScope(Dispatchers.IO).launch {
                val repository = SavingsRepository(context)
                val goals = repository.getActiveGoals()
                if (goals.isEmpty()) return@launch

                val currentGoalId = getWidgetGoalId(context, widgetId)
                val currentIndex = goals.indexOfFirst { it.id == currentGoalId }.let {
                    if (it >= 0) it else 0
                }

                val nextIndex = if (action == ACTION_NEXT_GOAL) {
                    (currentIndex + 1) % goals.size
                } else {
                    (currentIndex - 1 + goals.size) % goals.size
                }

                val targetGoal = goals[nextIndex]
                setWidgetGoalId(context, widgetId, targetGoal.id)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateSingleWidget(context, appWidgetManager, widgetId)
            }
        } else if (action == ACTION_TRIGGER_UPDATE) {
            triggerUpdate(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in appWidgetIds) {
            editor.remove(KEY_WIDGET_PREFIX + id)
        }
        editor.apply()
    }

    companion object {
        const val ACTION_PREV_GOAL = "com.minta1234.savingsgoal.widget.ACTION_PREV_GOAL"
        const val ACTION_NEXT_GOAL = "com.minta1234.savingsgoal.widget.ACTION_NEXT_GOAL"
        const val ACTION_TRIGGER_UPDATE = "com.minta1234.savingsgoal.widget.ACTION_TRIGGER_UPDATE"
        const val EXTRA_WIDGET_ID = "com.minta1234.savingsgoal.widget.EXTRA_WIDGET_ID"

        private const val PREFS_NAME = "savings_goal_widgets"
        private const val KEY_WIDGET_PREFIX = "widget_goal_"

        fun getWidgetGoalId(context: Context, widgetId: Int): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_WIDGET_PREFIX + widgetId, -1L)
        }

        fun setWidgetGoalId(context: Context, widgetId: Int, goalId: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_WIDGET_PREFIX + widgetId, goalId).apply()
        }

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SavingsGoalWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component) ?: intArrayOf()
            if (ids.isNotEmpty()) {
                val intent = Intent(context, SavingsGoalWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }

        suspend fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val repository = SavingsRepository(context)
            val goals = repository.getActiveGoals()
            val currency = repository.getDefaultCurrency()
            val views = RemoteViews(context.packageName, R.layout.widget_savings_goal)

            if (goals.isEmpty()) {
                views.setTextViewText(R.id.widget_goal_name, "No Goals Yet")
                views.setTextViewText(R.id.widget_goal_counter, "")
                views.setTextViewText(R.id.widget_goal_percent, "0%")
                views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                views.setTextViewText(R.id.widget_saved_amount, "Tap to add a goal")
                views.setTextViewText(R.id.widget_target_amount, "")
                views.setViewVisibility(R.id.widget_btn_prev, View.GONE)
                views.setViewVisibility(R.id.widget_btn_next, View.GONE)
            } else {
                val savedGoalId = getWidgetGoalId(context, widgetId)
                val targetIndex = goals.indexOfFirst { it.id == savedGoalId }.let {
                    if (it >= 0) it else 0
                }
                val goal = goals[targetIndex]

                // Ensure the active widget has a valid saved goal id
                if (savedGoalId != goal.id) {
                    setWidgetGoalId(context, widgetId, goal.id)
                }

                val goalCur = if (goal.currency.isNullOrBlank() || goal.currency == "null") {
                    currency
                } else {
                    goal.currency
                }

                views.setTextViewText(R.id.widget_goal_name, goal.name)
                views.setTextViewText(R.id.widget_goal_counter, "${targetIndex + 1}/${goals.size}")
                views.setTextViewText(R.id.widget_goal_percent, "${goal.progressPercent}%")
                views.setProgressBar(R.id.widget_progress_bar, 100, goal.progressPercent, false)
                views.setTextViewText(
                    R.id.widget_saved_amount,
                    "Saved: ${SavingsCalculations.formatCurrency(goal.totalSaved, goalCur)}"
                )
                views.setTextViewText(
                    R.id.widget_target_amount,
                    "Target: ${SavingsCalculations.formatCurrency(goal.amount, goalCur)}"
                )

                // If only 1 goal, hide prev/next navigation arrows; show them if multiple goals exist
                if (goals.size > 1) {
                    views.setViewVisibility(R.id.widget_btn_prev, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_btn_next, View.VISIBLE)

                    // Prev Button Intent
                    val prevIntent = Intent(context, SavingsGoalWidgetProvider::class.java).apply {
                        action = ACTION_PREV_GOAL
                        putExtra(EXTRA_WIDGET_ID, widgetId)
                    }
                    val prevPending = PendingIntent.getBroadcast(
                        context,
                        widgetId * 10 + 1,
                        prevIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

                    // Next Button Intent
                    val nextIntent = Intent(context, SavingsGoalWidgetProvider::class.java).apply {
                        action = ACTION_NEXT_GOAL
                        putExtra(EXTRA_WIDGET_ID, widgetId)
                    }
                    val nextPending = PendingIntent.getBroadcast(
                        context,
                        widgetId * 10 + 2,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)
                } else {
                    views.setViewVisibility(R.id.widget_btn_prev, View.GONE)
                    views.setViewVisibility(R.id.widget_btn_next, View.GONE)
                }
            }

            // Select/Settings Button Intent -> opens configuration activity
            val configIntent = Intent(context, SavingsGoalWidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val configPending = PendingIntent.getActivity(
                context,
                widgetId * 10 + 3,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_select, configPending)

            // Open App on widget body click
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPending = PendingIntent.getActivity(
                context,
                widgetId * 10,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPending)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
