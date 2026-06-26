package com.example.rbac.middleware

import android.util.Log
import com.example.rbac.model.RbacUser
import com.example.rbac.model.UserRole
import com.example.rbac.model.UserStatus
import com.example.ui.Screen

object RbacMiddleware {

    /**
     * Evaluates if a given user can navigate to a destination screen.
     * Returns a [MiddlewareResult] detailing the evaluation.
     */
    fun evaluateScreenAccess(user: RbacUser?, targetScreen: Screen): MiddlewareResult {
        // 1. Check if login is required
        if (targetScreen != Screen.SPLASH && targetScreen != Screen.LOGIN && targetScreen != Screen.EMAIL_VERIFICATION) {
            if (user == null) {
                Log.w("RbacMiddleware", "Access denied to $targetScreen: User not logged in.")
                return MiddlewareResult.Redirect(Screen.LOGIN, "Silakan login terlebih dahulu.")
            }
        }

        if (user == null) {
            return MiddlewareResult.Allow
        }

        // 2. Critical: Block suspended accounts instantly!
        if (user.userStatus == UserStatus.SUSPENDED) {
            Log.e("RbacMiddleware", "Access denied: Account ${user.email} is SUSPENDED!")
            return MiddlewareResult.Block("Akun Anda telah ditangguhkan (SUSPENDED). Silakan hubungi Owner.")
        }

        // 3. Screen Authorization Rules
        val role = user.userRole
        return when (targetScreen) {
            Screen.OWNER_DASHBOARD -> {
                if (role == UserRole.OWNER) MiddlewareResult.Allow
                else MiddlewareResult.Redirect(Screen.HOME, "Akses ditolak: Hanya Owner yang dapat mengakses dashboard ini.")
            }
            Screen.ADMIN_DASHBOARD -> {
                if (role == UserRole.OWNER || role == UserRole.ADMIN) MiddlewareResult.Allow
                else MiddlewareResult.Redirect(Screen.HOME, "Akses ditolak: Anda tidak memiliki akses Admin.")
            }
            Screen.SECURITY_DASHBOARD, Screen.ENCRYPTION_DASHBOARD, Screen.NETWORK_MONITOR -> {
                // Highly sensitive system configs
                if (role == UserRole.OWNER || role == UserRole.ADMIN) MiddlewareResult.Allow
                else MiddlewareResult.Redirect(Screen.HOME, "Akses ditolak: Fitur keamanan sensitif hanya untuk Owner/Admin.")
            }
            else -> MiddlewareResult.Allow
        }
    }
}

sealed class MiddlewareResult {
    object Allow : MiddlewareResult()
    data class Block(val reason: String) : MiddlewareResult()
    data class Redirect(val destination: Screen, val reason: String) : MiddlewareResult()
}
