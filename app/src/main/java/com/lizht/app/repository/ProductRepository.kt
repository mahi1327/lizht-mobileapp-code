package com.lizht.app.repository

import android.util.Log
import com.lizht.app.model.Product
import com.lizht.app.network.ProductApi

class ProductRepository(private val api: ProductApi) {
    // ProductRepository.kt
    suspend fun getAllProducts(): List<Product> {
        Log.d("Repository", "Calling API...")
        return api.getProducts()
    }

}

