package com.example.tasktogetherbeta

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar

// This feature is not included in the final app.
// There were daily reminder notifications planned every morning to motivate the users to use the app.
// I could not make it work like the way I imagined, so I excluded this from the app for now. The same story as TaskListener class

const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class Notification : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(intent.getStringExtra(titleExtra))
            .setContentText(intent.getStringExtra(messageExtra))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set priority for visibility
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)
    }
}

class NotificationManagerUtil(private val context: Context) {

    init {
        createNotificationChannel()
        scheduleDailyNotification()
    }

    private fun scheduleDailyNotification()
    {
        val intent = Intent(context, Notification::class.java).apply {
            putExtra(titleExtra, "Daily Reminder")
            putExtra(messageExtra, "Don't forget to complete your tasks today!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)  // Set time to 9:00 AM
            set(Calendar.MINUTE, 53)
            set(Calendar.SECOND, 30)
            /*
            if (timeInMillis < System.currentTimeMillis()) {
                // If the time is before the current time, schedule for the next day
                add(Calendar.DAY_OF_MONTH, 1)
            }*/
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun createNotificationChannel() {
        val name = "Notification Channel"
        val descriptionText = "Channel for daily reminders"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = descriptionText
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
