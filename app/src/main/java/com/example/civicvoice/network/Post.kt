package com.example.civicvoice.network

import com.google.gson.annotations.SerializedName

data class Post(
    @SerializedName("_id") val _id: String,
    val userId: String,
    val title: String,
    val description: String,
    val location: String,
    val hashtags: List<String>,
    val department: String,
    val status: String,
    val upvotes: Int,
    val downvotes: Int,
    val imageId: String?,
    val date: String,
    val isAnonymous: Boolean,
    val comments: List<Comment>,
    val officialComments: List<Comment>,
    val username: String,
    val profilePic: String?
)
