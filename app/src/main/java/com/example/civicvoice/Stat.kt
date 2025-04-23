package com.example.civicvoice

data class Stat(
    val title: String,
    val value: String,
    val iconRes: Int,
    val iconTint: Int,
    val filterStatus: String? = null
)