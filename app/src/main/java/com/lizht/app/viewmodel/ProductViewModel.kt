package com.lizht.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizht.app.model.Product
import com.lizht.app.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            try {
                _loading.value = true
                Log.d("ProductViewModel", "Fetching...")
                val result = repository.getAllProducts()
                Log.d("ProductViewModel", "Received: ${result.size} products")
                _products.value = result
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error fetching: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }

}
