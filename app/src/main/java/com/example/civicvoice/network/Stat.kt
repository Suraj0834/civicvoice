package com.example.civicvoice.network

data class Stat(
    val title: String,
    val value: String,
    val iconRes: Int,
    val iconTint: Int,
    val filterStatus: String? = null
)