package com.lizht.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizht.app.data.remote.TransactionDto
import com.lizht.app.data.remote.providePaymentApi
import com.lizht.app.repository.TransactionsRepository
import com.lizht.app.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Hardcode your backend base URL (Option 2)
private const val BASE_URL_HARDCODED = "https://backend-lizht.onrender.com/"

data class TransactionsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<TransactionDto> = emptyList()
)

class TransactionsViewModel(
    private val authRepository: AuthRepository,
    private val baseUrl: String = BASE_URL_HARDCODED
) : ViewModel() {

    private val _ui = MutableStateFlow(TransactionsUiState())
    val ui: StateFlow<TransactionsUiState> = _ui

    private var loadJob: Job? = null

    fun load(storeId: String? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val token = authRepository.accessToken()
                    ?: throw IllegalStateException("Not authenticated")

                val api = providePaymentApi(baseUrl) { token }
                val repo = TransactionsRepository(api)

                val data = if (storeId.isNullOrBlank()) {
                    repo.myTransactions()
                } else {
                    repo.myStoreTransactions(storeId)
                }

                _ui.value = TransactionsUiState(
                    loading = false,
                    items = data
                )
            } catch (e: Exception) {
                _ui.value = TransactionsUiState(
                    loading = false,
                    error = e.message ?: "Failed to load transactions"
                )
            }
        }
    }
}
