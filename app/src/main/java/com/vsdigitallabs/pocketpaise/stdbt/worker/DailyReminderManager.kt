package com.vsdigitallabs.pocketpaise.stdbt.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object DailyReminderManager {

    fun scheduleReminder(context: Context, timeString: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyReminderReceiver::class.java)
            
            // Use FLAG_UPDATE_CURRENT to ensure we update any existing intent with new parameters
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                1001, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Parse time, e.g. "21:00"
            val parts = timeString.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: 21
            val minute = parts[1].toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If the time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Using setExactAndAllowWhileIdle for exact delivery despite Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("DailyReminderManager", "Scheduled reminder at: ${calendar.time}")
        } catch (e: Exception) {
            Log.e("DailyReminderManager", "Failed to schedule reminder", e)
        }
    }
}
