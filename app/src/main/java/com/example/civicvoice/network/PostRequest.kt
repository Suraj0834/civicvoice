package com.example.civicvoice.network

data class PostRequest(
    val title: String,
    val description: String,
    val location: String,
    val department: String,
    val hashtags: List<String>
)