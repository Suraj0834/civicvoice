package com.example.civicvoice.data

data class Comment(
    val id: String,
    val content: String,
    val date: String,
    val username: String?, // Make nullable
    val isOfficial: Boolean
)