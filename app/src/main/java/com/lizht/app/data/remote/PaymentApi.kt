package com.lizht.app.data.remote


import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/* ---------- DTO mirrors your /api/pay/... responses ---------- */
data class TransactionDto(
    val transactionId: String,
    val userId: String?,
    val storeName: String?,
    val billAmount: Double?,
    val discountPercent: Double?,
    val discountedAmount: Double?,
    val paymentStatus: String,
    val provider: String?,
    val providerPaymentId: String?,
    val method: String?,
    val currency: String?,
    val metadata: Any?,
    val createdAt: String
)

/* ---------- Razorpay user-scoped transaction endpoints ---------- */
interface PaymentApi {
    @GET("/api/pay/transactions/mine")
    suspend fun getMyTransactions(): List<TransactionDto>

    @GET("/api/pay/transactions/store/{storeId}/mine")
    suspend fun getMyStoreTransactions(@Path("storeId") storeId: String): List<TransactionDto>
}

/* ---------- Simple auth interceptor that injects a Bearer token ---------- */
class TokenInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val req = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        return chain.proceed(req)
    }
}

/* ---------- Build a PaymentApi that always uses the latest token ---------- */
fun providePaymentApi(
    baseUrl: String,
    tokenProvider: () -> String?
): PaymentApi {
    val client = OkHttpClient.Builder()
        .addInterceptor(TokenInterceptor(tokenProvider))
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://backend-lizht.onrender.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(PaymentApi::class.java)
}
