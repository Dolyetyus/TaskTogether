package com.example.tasktogetherbeta

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

data class TaskNotification(
    val title: String = "",
    val message: String = ""
)

// This class is used for sending notifications if a task is created or edited or deleted by partner
// It would be easier if done using Firebase functions and FCM but it is a paid service.
// Planned to be implemented in the future (probably). The same story as NotificationManagerUtil class

class TaskListener(private val context: Context) {

    fun setupNotificationListener() {
        val currentUserUid = Firebase.auth.currentUser?.uid ?: return
        val notificationRef = FirebaseDatabase.getInstance().getReference("users/$currentUserUid/notifications")

        notificationRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Use the TaskNotification class for deserialization
                val notification = snapshot.getValue(TaskNotification::class.java)
                notification?.let {
                    sendTaskNotification(it.title, it.message)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendTaskNotification(title: String, message: String) {
        val notificationId = message.hashCode() // Unique ID for each notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, "task_channel")
            .setSmallIcon(R.drawable.baseline_handshake_24)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}


class NotificationUtils(private val context: Context) {

    companion object {
        const val TASK_NOTIFICATION_CHANNEL_ID = "task_channel"
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(TASK_NOTIFICATION_CHANNEL_ID, "Task Notifications", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for task-related notifications"
        }

        val notificationManager: NotificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}

