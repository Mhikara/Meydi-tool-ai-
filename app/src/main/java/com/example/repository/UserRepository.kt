package com.example.repository

import com.example.utils.FirebaseManager
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    private val _userState = MutableStateFlow(getUserStatus())
    val userState: StateFlow<UserStatus> = _userState

    fun getUserStatus(): UserStatus {
        return if (firebaseManager.isGuest()) UserStatus.Guest else UserStatus.Member
    }

    suspend fun signInAsGuest(): Result<Boolean> {
        val result = firebaseManager.signInAnonymously()
        if (result.isSuccess) _userState.value = UserStatus.Guest
        return result
    }

    suspend fun upgradeToPermanent(credential: AuthCredential): Result<Boolean> {
        val result = firebaseManager.linkCredential(credential)
        if (result.isSuccess) _userState.value = UserStatus.Member
        return result
    }

    fun logout() {
        firebaseManager.logout()
        _userState.value = UserStatus.LoggedOut
    }
}

sealed class UserStatus {
    object Guest : UserStatus()
    object Member : UserStatus()
    object LoggedOut : UserStatus()
}
