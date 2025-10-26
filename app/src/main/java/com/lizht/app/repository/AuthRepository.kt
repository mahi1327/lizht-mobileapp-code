package com.lizht.app.repository


import android.content.Context
import com.lizht.app.data.remote.AuthApi
import com.lizht.app.data.remote.AuthResponse
import com.lizht.app.utils.SecuredStore

class AuthRepository(
    private val api: AuthApi,
    private val ctx: Context
) {
    private val store = SecuredStore(ctx)

    suspend fun register(name: String, email: String, password: String): AuthResponse {
        val res = api.register(mapOf("name" to name, "email" to email, "password" to password))
        store.saveTokens(res.accessToken, res.refreshToken)
        return res
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val res = api.login(mapOf("email" to email, "password" to password))
        store.saveTokens(res.accessToken, res.refreshToken)
        return res
    }

    suspend fun logout(userId: String) {
        api.logout(mapOf("userId" to userId))
        store.clear()
    }

    fun accessToken(): String? = store.getAccess()
    fun refreshToken(): String? = store.getRefresh()
}
