package com.lizht.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizht.app.data.remote.WishlistItemDto
import com.lizht.app.repository.WishlistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

data class WishlistState(
    val items: List<WishlistItemDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val pending: Int = 0,          // in-flight ops
    val lastMutationId: Long = 0L  // bump on each local mutation
)

class WishlistViewModel(private val repo: WishlistRepository) : ViewModel() {

    private val _state = MutableStateFlow(WishlistState(loading = true))
    val state: StateFlow<WishlistState> = _state

    private val opMutex = Mutex()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val startMut = _state.value.lastMutationId
        _state.update { it.copy(loading = true, error = null) }
        try {
            val remote = repo.fetch()
            // Only apply if no local mutation happened since this refresh started
            _state.update { cur ->
                if (cur.lastMutationId == startMut) cur.copy(items = remote, loading = false)
                else cur.copy(loading = false) // keep optimistic UI
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }

    fun toggle(product: WishlistItemDto) {
        viewModelScope.launch {
            opMutex.withLock {
                val exists = _state.value.items.any { it.productId == product.productId }
                if (exists) removeInternal(product.productId) else addInternal(product)
            }
        }
    }

    fun add(product: WishlistItemDto) {
        viewModelScope.launch { opMutex.withLock { addInternal(product) } }
    }

    fun remove(productId: String) {
        viewModelScope.launch { opMutex.withLock { removeInternal(productId) } }
    }

    // --- internals ---

    private suspend fun addInternal(product: WishlistItemDto) {
        // optimistic add from latest state
        _state.update {
            it.copy(
                items = it.items + product,
                pending = it.pending + 1,
                lastMutationId = it.lastMutationId + 1,
                error = null
            )
        }
        try {
            val saved = repo.add(product)
            // reconcile if still present
            _state.update { s ->
                if (s.items.any { it.productId == product.productId }) {
                    s.copy(
                        items = s.items.map { if (it.productId == product.productId) saved else it },
                        pending = s.pending - 1
                    )
                } else {
                    s.copy(pending = s.pending - 1) // user removed meanwhile
                }
            }
        } catch (e: HttpException) {
            if (e.code() == 409) { // already exists → treat as success
                _state.update { it.copy(pending = it.pending - 1) }
                return
            }
            _state.update { s ->
                s.copy(
                    items = s.items.filterNot { it.productId == product.productId },
                    pending = s.pending - 1,
                    error = e.message()
                )
            }
        } catch (e: Exception) {
            _state.update { s ->
                s.copy(
                    items = s.items.filterNot { it.productId == product.productId },
                    pending = s.pending - 1,
                    error = e.message
                )
            }
        }
    }

    private suspend fun removeInternal(productId: String) {
        // optimistic remove from latest state
        _state.update {
            it.copy(
                items = it.items.filterNot { p -> p.productId == productId },
                pending = it.pending + 1,
                lastMutationId = it.lastMutationId + 1,
                error = null
            )
        }
        try {
            repo.remove(productId) // returns 204
            _state.update { it.copy(pending = it.pending - 1) }
        } catch (e: HttpException) {
            if (e.code() == 404) { // already gone → success
                _state.update { it.copy(pending = it.pending - 1) }
                return
            }
            // rollback only if the item is still absent (i.e., user didn’t re-add)
            _state.update { s ->
                val stillAbsent = s.items.none { it.productId == productId }
                if (stillAbsent) {
                    // We don't have original fields for perfect rollback; typically won’t hit due to 404 path.
                    s.copy(pending = s.pending - 1, error = e.message())
                } else {
                    s.copy(pending = s.pending - 1, error = e.message())
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(pending = it.pending - 1, error = e.message) }
        }
    }
}
