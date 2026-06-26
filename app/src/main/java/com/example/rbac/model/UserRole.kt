package com.example.rbac.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TextMuted

enum class UserRole(val key: String, val label: String, val color: Color) {
    OWNER("owner", "Owner", NeonMagenta),
    ADMIN("admin", "Admin", NeonTeal),
    USER("user", "User", TextMuted);

    companion object {
        fun fromKey(key: String?): UserRole {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: USER
        }
    }
}

enum class UserStatus(val key: String, val label: String, val color: Color) {
    ACTIVE("active", "Active", Color(0xFF00FF88)),
    SUSPENDED("suspended", "Suspended", Color(0xFFFF3366));

    companion object {
        fun fromKey(key: String?): UserStatus {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: ACTIVE
        }
    }
}
