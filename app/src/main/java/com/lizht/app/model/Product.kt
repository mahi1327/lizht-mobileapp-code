package com.lizht.app.model

data class Product(
    val _id: String,
    val title: String,
    val image: String,
    val price: Int,
    val affiliateLink: String,
    val category: String,
    val addedAt: String
)
