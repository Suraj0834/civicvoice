package com.example.civicvoice.service

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.civicvoice.NotificationHelper
import com.example.civicvoice.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFirebaseMessagingService : FirebaseMessagingService() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Civic Voice Update"
        val body = message.notification?.body ?: "You have a new notification"
        val postId = message.data["postId"]
        val userId = message.data["userId"] ?: return

        NotificationHelper.sendNotification(
            context = this,
            userId = userId,
            title = title,
            message = body,
            postId = postId
        )
    }

    override fun onNewToken(token: String) {
        val apiService = RetrofitClient.instance
        apiService.updateFcmToken(mapOf("fcmToken" to token)).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) Log.d("FCM", "Token updated")
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("FCM", "Token update failed: ${t.message}")
            }
        })
    }
}