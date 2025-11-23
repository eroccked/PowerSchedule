package com.example.powerschedule

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class PowerScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences("PowerSchedule", Context.MODE_PRIVATE)

        val queueName = prefs.getString("queue_name_0", "Додайте чергу")
        val queueValue = prefs.getString("queue_value_0", "")

        val views = RemoteViews(context.packageName, R.layout.widget_power_schedule)

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        views.setTextViewText(R.id.widgetTime, currentTime)

        if (queueValue?.isNotEmpty() == true) {
            views.setTextViewText(R.id.widgetQueueName, "$queueName ($queueValue)")

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when {
                currentHour in 6..9 || currentHour in 14..17 -> {
                    views.setTextViewText(R.id.widgetStatus, "🔴 ЗАРАЗ ВІДКЛЮЧЕННЯ")
                    views.setTextViewText(R.id.widgetNextShutdown, "Світло з'явиться незабаром")
                }
                else -> {
                    views.setTextViewText(R.id.widgetStatus, "🟢 ЗАРАЗ Є СВІТЛО")
                    views.setTextViewText(R.id.widgetNextShutdown, "Наступне відключення:\nперевірте в додатку")
                }
            }
        } else {
            views.setTextViewText(R.id.widgetQueueName, "Додайте чергу в додатку")
            views.setTextViewText(R.id.widgetStatus, "")
            views.setTextViewText(R.id.widgetNextShutdown, "")
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetQueueName, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}