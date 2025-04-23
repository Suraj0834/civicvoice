package com.example.civicvoice.network

data class Comment(
    val _id: String,
    val content: String,
    val date: String,
    val username: String?, // Nullable
    val isOfficial: Boolean,
    val author: String?
)