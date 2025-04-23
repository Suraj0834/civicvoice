package com.example.civicvoice.network

import com.google.gson.annotations.SerializedName

data class Notification(
    @SerializedName("_id")
    val _id: String,
    val title: String,
    val message: String,
    val postId: String?,
    val userId: String,
    val timestamp: String,
    var isRead: Boolean
)


