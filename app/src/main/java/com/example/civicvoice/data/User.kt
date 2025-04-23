package com.example.civicvoice.data

data class User(
    val userId: String,
    val username: String,
    val password: String,
    val name: String,
    val email: String,
    val userType: String,
    val isOfficial: Boolean,
    val isAnonymous: Boolean,
    val department: String?,
    val phone: String?,
    val profilePic: String?,
    val fcmToken: String?
)
