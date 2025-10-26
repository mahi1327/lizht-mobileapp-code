package com.lizht.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.lizht.app.repository.AuthRepository
import com.lizht.app.data.remote.UserDto
import com.lizht.app.utils.SecuredStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val currentUser: UserDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false
)

class AuthViewModel(
    app: Application,
    private val repo: AuthRepository
) : AndroidViewModel(app) {

    private val store = SecuredStore(app.applicationContext)
    private val gson = Gson()

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    init {
        // Restore persisted session
        val has = store.hasSession()
        val userJson = store.getUserJson()
        val user = userJson?.let { runCatching { gson.fromJson(it, UserDto::class.java) }.getOrNull() }
        _state.value = _state.value.copy(isSignedIn = has, currentUser = user)
    }

    fun register(name: String, email: String, password: String) = viewModelScope.launch {
        try {
            _state.value = _state.value.copy(loading = true, error = null)
            val res = repo.register(name, email, password)
            store.saveTokens(res.accessToken, res.refreshToken)
            store.saveUserJson(gson.toJson(res.user))
            _state.value = AuthState(currentUser = res.user, isSignedIn = true)
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = e.message ?: "Sign up failed")
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        try {
            _state.value = _state.value.copy(loading = true, error = null)
            val res = repo.login(email, password)
            store.saveTokens(res.accessToken, res.refreshToken)
            store.saveUserJson(gson.toJson(res.user))
            _state.value = AuthState(currentUser = res.user, isSignedIn = true)
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = e.message ?: "Sign in failed")
        }
    }

    fun logout() = viewModelScope.launch {
        // optional: call repo.logout(userId) first
        store.clear()
        _state.value = AuthState()
    }
}
