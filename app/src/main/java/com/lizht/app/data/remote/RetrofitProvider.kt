package com.lizht.app.data.remote

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    fun build(context: Context, baseUrl: String): Retrofit {
        // Retrofit wants trailing slash; TokenAuthenticator used the no-slash version previously
        val cleanBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val noSlashBase = cleanBase.trimEnd('/')

        // Use debuggable flag instead of BuildConfig.DEBUG
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val logging = HttpLoggingInterceptor { msg -> Log.d("HTTP", msg) }.apply {
            level = if (isDebug) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val defaultHeaders = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Accept", "application/json")
                .build()
            chain.proceed(req)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(defaultHeaders)
            .addInterceptor(AuthInterceptor(context))                 // Authorization: Bearer <token>
            .addInterceptor(logging)                                  // Wire logs (debug only)
            .authenticator(TokenAuthenticator(context, noSlashBase))  // 401 → refresh → retry
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(cleanBase)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}
