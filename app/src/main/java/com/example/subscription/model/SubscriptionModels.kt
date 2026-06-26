package com.example.subscription.model

import java.util.Date

enum class UserRole {
    FREE, PREMIUM
}

enum class SubscriptionStatus {
    ACTIVE, EXPIRED, PENDING, CANCELED, REFUNDED
}

enum class SubscriptionPlan(val id: String, val durationDays: Int, val price: Double) {
    WEEKLY("plan_weekly", 7, 5000.0),
    MONTHLY("plan_monthly", 30, 15000.0),
    THREE_MONTHS("plan_3_months", 90, 35000.0),
    SIX_MONTHS("plan_6_months", 180, 80000.0),
    ANNUAL("plan_annual", 365, 210000.0),
    LIFETIME("plan_lifetime", 36500, 300000.0)
}

data class SubscriptionInfo(
    val role: UserRole = UserRole.FREE,
    val status: SubscriptionStatus = SubscriptionStatus.PENDING,
    val plan: SubscriptionPlan? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val autoRenewal: Boolean = false,
    val lastTransactionId: String? = null
) {
    val isPremiumActive: Boolean
        get() = role == UserRole.PREMIUM && status == SubscriptionStatus.ACTIVE && (endDate == null || endDate > System.currentTimeMillis())

    val daysRemaining: Long
        get() = endDate?.let { (it - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) } ?: 0
}
