package com.lizht.app.data.remote


import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AuthInterceptor(private val ctx: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val store = com.lizht.app.utils.SecuredStore(ctx)
        val req = chain.request()
        val access = store.getAccess()
        val newReq = if (access != null) {
            req.newBuilder().addHeader("Authorization", "Bearer $access").build()
        } else req
        return chain.proceed(newReq)
    }
}

class TokenAuthenticator(
    private val ctx: Context,
    private val baseUrl: String
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid loops
        if (responseCount(response) >= 2) return null
        val store = com.lizht.app.utils.SecuredStore(ctx)
        val refresh = store.getRefresh() ?: return null

        // Synchronously refresh
        val client = OkHttpClient()
        val body = """{"refreshToken":"$refresh"}""".toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$baseUrl/api/auth/refresh").post(body).build()

        val res = client.newCall(req).execute()
        if (!res.isSuccessful) return null

        val json = res.body?.string() ?: return null
        val (newAccess, newRefresh) = parseTokens(json) ?: return null
        store.saveTokens(newAccess, newRefresh)

        // Retry original request with new access token
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var r = response
        var count = 1
        while (r.priorResponse != null) { r = r.priorResponse!!; count++ }
        return count
    }

    private fun parseTokens(json: String): Pair<String, String>? {
        // quick & dirty parsing (use kotlinx.serialization or Moshi ideally)
        val a = """"accessToken"\s*:\s*"([^"]+)"""".toRegex().find(json)?.groupValues?.get(1)
        val r = """"refreshToken"\s*:\s*"([^"]+)"""".toRegex().find(json)?.groupValues?.get(1)
        return if (a != null && r != null) a to r else null
    }
}
