package com.lizht.app.network

import com.lizht.app.model.Store
import retrofit2.http.GET

interface StoreApi {
    @GET("/api/stores")
    suspend fun getStores(): List<Store>
}
