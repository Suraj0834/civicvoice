package com.example.civicvoice.network

data class AppUser(
    val userId: String,
    val username: String,
    val email: String,
    val name: String,
    val phone: String?,
    val userType: String,
    val department: String?,
    val isOfficial: Boolean,
    val isAnonymous: Boolean,
    val profilePic: String? = null,
    val fcmToken: String? = null
)