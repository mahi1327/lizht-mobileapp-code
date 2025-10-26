package com.lizht.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object StoreRetrofitInstance {
    private const val BASE_URL = "https://backend-lizht.onrender.com/" // Replace with your IP

    val storeApi: StoreApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StoreApi::class.java)
    }
}
