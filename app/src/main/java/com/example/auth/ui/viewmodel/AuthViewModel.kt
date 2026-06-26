package com.example.auth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.model.AuthState
import com.example.auth.model.User
import com.example.auth.repository.AuthRepository
import com.example.auth.repository.DynamicAuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel(
    val repository: AuthRepository = DynamicAuthRepositoryImpl()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            repository.currentUser.collectLatest { user ->
                _currentUser.value = user
                if (user == null) {
                    _authState.value = AuthState.Unauthenticated()
                } else if (!user.isEmailVerified && !repository.isEmailVerified) {
                    _authState.value = AuthState.EmailNotVerified
                } else {
                    _authState.value = AuthState.Authenticated(user)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.loginWithEmail(email, password)
                .onSuccess {
                    // State will be updated by observer
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
        }
    }

    fun register(name: String, username: String, email: String, password: String, phoneNumber: String? = null) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.registerWithEmail(name, username, email, password, phoneNumber)
                .onSuccess {
                    _authState.value = AuthState.EmailNotVerified
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                }
        }
    }

    fun updateFcmToken() {
        viewModelScope.launch {
            repository.updateFcmToken()
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.loginWithGoogle(idToken)
                .onSuccess {
                    // State will be updated by observer
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Google login failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Unauthenticated()
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            repository.sendEmailVerification()
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.sendPasswordResetEmail(email)
                .onSuccess {
                    _authState.value = AuthState.Unauthenticated("Password reset email sent")
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Failed to send reset email")
                }
        }
    }

    fun checkVerificationStatus() {
        viewModelScope.launch {
            repository.reloadUser()
            // reloadUser will trigger the observer if isEmailVerified changes
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
