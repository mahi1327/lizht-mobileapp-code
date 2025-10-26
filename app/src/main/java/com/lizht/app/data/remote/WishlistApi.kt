package com.lizht.app.data.remote

import retrofit2.http.*

data class WishlistItemDto(
    val _id: String? = null,
    val productId: String,
    val title: String,
    val image: String,
    val price: Double,
    val affiliateLink: String
)

interface WishlistApi {
    @GET("api/wishlist")
    suspend fun getWishlist(): List<WishlistItemDto>

    @POST("api/wishlist")
    suspend fun add(@Body item: WishlistItemDto): WishlistItemDto

    @DELETE("api/wishlist/{productId}")
    suspend fun remove(@Path("productId") productId: String)
}
