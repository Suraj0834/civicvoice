package com.example.civicvoice.network

data class CommentRequest(
    val content: String,
    val isOfficial: Boolean,
    val author: String
)