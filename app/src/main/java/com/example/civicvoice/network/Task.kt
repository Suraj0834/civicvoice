package com.example.civicvoice.network

import com.google.gson.annotations.SerializedName

data class Task(
    @SerializedName("_id") val _id: String,
    val title: String,
    val description: String,
    val dueDate: String,
    val status: String,
    val officialId: String,
    val createdBy: String,
    val createdAt: String

)
