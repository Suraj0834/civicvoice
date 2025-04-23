package com.example.civicvoice.network

data class NotificationRequest(
    val title: String,
    val message: String,
    val postId: String?,
    val userId: String
)
