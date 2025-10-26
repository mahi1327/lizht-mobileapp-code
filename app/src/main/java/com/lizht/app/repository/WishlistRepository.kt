package com.lizht.app.repository


import com.lizht.app.data.remote.WishlistApi
import com.lizht.app.data.remote.WishlistItemDto

class WishlistRepository(private val api: WishlistApi) {
    suspend fun fetch() = api.getWishlist()
    suspend fun add(item: WishlistItemDto) = api.add(item)
    suspend fun remove(productId: String) = api.remove(productId)
}
