package com.example.civicvoice.network

import com.google.gson.annotations.SerializedName

data class Event(
    @SerializedName("_id") val _id: String,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val department: String,
    val createdBy: String,
    val status: String?
)