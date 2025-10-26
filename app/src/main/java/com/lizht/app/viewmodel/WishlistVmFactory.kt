package com.lizht.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lizht.app.repository.WishlistRepository

class WishlistVmFactory(private val repo: WishlistRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WishlistViewModel(repo) as T
    }
}
