package com.lizht.app.network

import com.lizht.app.model.Product
import retrofit2.http.GET

interface ProductApi {
    @GET("/api/products")
    suspend fun getProducts(): List<Product>
}
