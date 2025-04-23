package com.example.civicvoice.network

import com.google.gson.annotations.SerializedName

data class Feedback(
    @SerializedName("_id") val _id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("complaintId") val complaintId: String,
    @SerializedName("content") val content: String,
    @SerializedName("date") val date: String
)

data class FeedbackRequest(
    @SerializedName("complaintId") val complaintId: String,
    @SerializedName("content") val content: String,
    @SerializedName("userId") val userId: String
)