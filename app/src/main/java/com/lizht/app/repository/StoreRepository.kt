package com.lizht.app.repository

import com.lizht.app.model.Store
import com.lizht.app.network.StoreApi

class StoreRepository(private val api: StoreApi) {
    suspend fun getAllStores(): List<Store> {
        return api.getStores()
    }
}
