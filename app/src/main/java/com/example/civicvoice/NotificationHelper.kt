package com.example.civicvoice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.civicvoice.MainActivity
import com.example.civicvoice.PostDetailActivity
import com.example.civicvoice.R
import com.example.civicvoice.network.Notification
import com.example.civicvoice.network.NotificationRequest
import com.example.civicvoice.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NotificationHelper {
    private const val CHANNEL_ID = "civic_voice_channel"
    private const val CHANNEL_NAME = "Civic Voice Updates"
    private const val CHANNEL_DESC = "Notifications for post updates and comments"

    fun addNotification(userId: String, title: String, message: String, postId: String?) {
        val notificationRequest = NotificationRequest(
            title = title,
            message = message,
            postId = postId,
            userId = userId
        )
        RetrofitClient.instance.createNotification(notificationRequest).enqueue(object : Callback<Notification> {
            override fun onResponse(call: Call<Notification>, response: Response<Notification>) {
                if (response.isSuccessful) {
                    println("Notification added for $userId: $title")
                } else {
                    println("Failed to add notification: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<Notification>, t: Throwable) {
                println("Failed to add notification: ${t.message}")
            }
        })
    }

    fun sendNotification(
        context: Context,
        userId: String,
        title: String,
        message: String,
        postId: String?,
        userType: String = "user"
    ) {
        createNotificationChannel(context)
        addNotification(userId, title, message, postId)
        createLocalNotification(context, userId, title, message, postId)
    }

    private fun createLocalNotification(context: Context, userId: String, title: String, message: String, postId: String?) {
        val intent = if (postId != null) {
            Intent(context, PostDetailActivity::class.java).apply {
                putExtra("post_id", postId)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                putExtra("notification_target", "notifications")
            }
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            userId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_1)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(userId.hashCode(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}