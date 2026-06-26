package com.example.rbac.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rbac.model.RbacUser
import com.example.rbac.model.UserRole
import com.example.rbac.model.UserStatus
import com.example.rbac.repository.RbacRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RbacViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RbacRepository(application)
    
    val currentUserState: StateFlow<RbacUser?> = repository.currentUserState

    private val _allUsers = MutableStateFlow<List<RbacUser>>(emptyList())
    val allUsers: StateFlow<List<RbacUser>> = _allUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // Automatically sync profile of current logged in user
        refreshActiveUserProfile()
    }

    fun refreshActiveUserProfile() {
        val auth = try {
            com.google.firebase.auth.FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
        val user = auth?.currentUser
        if (user != null) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    repository.fetchUserProfile(user.uid, user.email)
                } catch (e: Exception) {
                    _error.value = e.localizedMessage
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            // For mock/simulation logins
            val cachedUser = repository.currentUserState.value
            if (cachedUser != null) {
                viewModelScope.launch {
                    repository.fetchUserProfile(cachedUser.uid, cachedUser.email)
                }
            }
        }
    }

    fun loginSimulatedUser(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mockUid = "mock_uid_" + email.hashCode()
                val user = repository.fetchUserProfile(mockUid, email)
                repository.cacheCurrentUser(user)
                _toastMessage.emit("Berhasil masuk sebagai ${user.email} (${user.userRole.label})")
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearCache()
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                // Firebase not initialized
            }
            _allUsers.value = emptyList()
            _toastMessage.emit("Berhasil Log Out dari sistem RBAC")
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.loadAllUsers()
                _allUsers.value = list
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeUserRole(targetUser: RbacUser, newRole: UserRole) {
        val editor = currentUserState.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateUserRole(targetUser.uid, newRole, editor.userRole)
            if (success) {
                _toastMessage.emit("Role ${targetUser.email} berhasil diubah ke ${newRole.label}!")
                loadAllUsers()
                // If we edited our own role, refresh active profile
                if (targetUser.uid == editor.uid) {
                    refreshActiveUserProfile()
                }
            } else {
                _error.value = "Gagal mengubah role. Pastikan Anda memiliki akses Owner."
            }
            _isLoading.value = false
        }
    }

    fun toggleUserStatus(targetUser: RbacUser) {
        val editor = currentUserState.value ?: return
        val newStatus = if (targetUser.userStatus == UserStatus.ACTIVE) UserStatus.SUSPENDED else UserStatus.ACTIVE
        
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateUserStatus(
                targetUid = targetUser.uid,
                targetEmail = targetUser.email,
                newStatus = newStatus,
                editorRole = editor.userRole,
                targetRole = targetUser.userRole
            )
            if (success) {
                _toastMessage.emit("Status ${targetUser.email} sekarang: ${newStatus.label}!")
                loadAllUsers()
            } else {
                _error.value = "Gagal mengubah status. Anda tidak berwenang menangguhkan pengguna ini."
            }
            _isLoading.value = false
        }
    }

    fun createMockUser(name: String, email: String, role: UserRole) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.createMockUser(name, email, role)
            if (success) {
                _toastMessage.emit("Berhasil membuat user simulasi: $email")
                loadAllUsers()
            } else {
                _error.value = "Gagal membuat user simulasi."
            }
            _isLoading.value = false
        }
    }

    fun deleteMockUser(targetUser: RbacUser) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteMockUser(targetUser.uid)
            if (success) {
                _toastMessage.emit("Berhasil menghapus user: ${targetUser.email}")
                loadAllUsers()
            } else {
                _error.value = "Gagal menghapus user. Anda tidak bisa menghapus Owner."
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
