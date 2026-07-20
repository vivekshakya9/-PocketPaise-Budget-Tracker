package com.vsdigitallabs.pocketpaise.stdbt.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vsdigitallabs.pocketpaise.stdbt.MainActivity
import com.vsdigitallabs.pocketpaise.stdbt.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.vsdigitallabs.pocketpaise.stdbt.data.local.AppDatabase
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Notification

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "PocketPaise"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: ""
        val type = remoteMessage.data["type"] ?: "GLOBAL_ALERT"

        // Save to local Room DB
        saveNotificationToDb(title, message, type)

        // Show system notification
        sendNotification(title, message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to PHP server if needed (handled in ViewModel via GoogleSignIn)
    }

    private fun saveNotificationToDb(title: String, message: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                
                val prefs = applicationContext.getSharedPreferences("PocketPaisePrefs", Context.MODE_PRIVATE)
                val userId = prefs.getLong("currentUserId", -1L)
                
                if (userId != -1L) {
                    db.notificationDao().insertNotification(
                        Notification(
                            userId = userId,
                            title = title,
                            message = message,
                            timestamp = System.currentTimeMillis(),
                            type = type
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "pocketpaise_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "PocketPaise Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
