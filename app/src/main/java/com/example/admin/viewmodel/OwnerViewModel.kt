package com.example.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.model.SystemStats
import com.example.admin.repository.OwnerRepository
import com.example.auth.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OwnerViewModel : ViewModel() {

    private val ownerRepository = OwnerRepository(FirebaseFirestore.getInstance())


    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _stats = MutableStateFlow(SystemStats())
    val stats: StateFlow<SystemStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _users.value = ownerRepository.getAllUsers()
                ownerRepository.getSystemStats().collect {
                    _stats.value = it
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeUserRole(uid: String, role: String, adminUid: String) {
        viewModelScope.launch {
            ownerRepository.updateUserRole(uid, role, adminUid)
            loadDashboardData()
        }
    }

    fun toggleUserBlock(uid: String, currentStatus: Boolean, adminUid: String) {
        viewModelScope.launch {
            ownerRepository.setUserBlockStatus(uid, !currentStatus, adminUid)
            loadDashboardData()
        }
    }
}
