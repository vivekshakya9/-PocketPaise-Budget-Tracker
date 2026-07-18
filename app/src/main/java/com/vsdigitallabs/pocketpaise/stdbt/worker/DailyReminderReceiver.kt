package com.vsdigitallabs.pocketpaise.stdbt.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vsdigitallabs.pocketpaise.stdbt.MainActivity
import com.vsdigitallabs.pocketpaise.stdbt.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("PocketPaisePrefs", Context.MODE_PRIVATE)
        val reminderTimeStr = prefs.getString("daily_reminder_time", "21:00") ?: "21:00"

        // Fire off network request in Coroutine
        CoroutineScope(Dispatchers.IO).launch {
            var reminderMsg = "आज के खर्च (Expenses) ऐप में दर्ज कर लें।"
            try {
                val response = RetrofitClient.apiService.getConfig()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val appMap = data?.get("app") as? Map<String, Any>
                    
                    val adminMessage = appMap?.get("daily_reminder_message")?.toString()
                    if (!adminMessage.isNullOrEmpty()) {
                        reminderMsg = adminMessage
                    }
                    
                    val adminTime = appMap?.get("daily_reminder_time")?.toString()
                    if (!adminTime.isNullOrEmpty() && adminTime != reminderTimeStr) {
                        prefs.edit().putString("daily_reminder_time", adminTime).apply()
                        // Reschedule with new time
                        DailyReminderManager.scheduleReminder(context, adminTime)
                    }
                }
            } catch (e: Exception) {
                Log.e("DailyReminderReceiver", "Failed to fetch config for reminder", e)
            }

            showNotification(context, reminderMsg)
        }

        // Reschedule for next day using existing time
        DailyReminderManager.scheduleReminder(context, reminderTimeStr)
    }

    private fun showNotification(context: Context, message: String) {
        val channelId = "pocketpaise_daily_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Expense Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to add daily expenses"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle("PocketPaise Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }
}
