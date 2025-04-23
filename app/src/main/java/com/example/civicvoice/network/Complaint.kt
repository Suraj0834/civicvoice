package com.example.civicvoice.network

import com.google.gson.annotations.SerializedName

data class Complaint(
    @SerializedName("_id") val _id: String,
    val userId: String,
    val title: String,
    val description: String,
    val location: String,
    val department: String,
    val status: String,
    val postId: String
)