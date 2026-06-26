package com.example.payment.model

enum class PaymentStatus {
    PENDING,        // Menunggu Pembayaran
    VERIFYING,      // Sedang Diverifikasi
    SUCCESS,        // Berhasil
    FAILED,         // Gagal
    EXPIRED         // Kedaluwarsa
}

data class PaymentTransaction(
    val id: String,
    val invoiceId: String,
    val transactionCode: String,
    val amount: Double,
    val uniqueCode: Int,
    val storeName: String = "Memet Store",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (15 * 60 * 1000), // 15 Menit sesuai permintaan
    val qrisData: String? = null
) {
    val totalAmount: Double get() = amount + uniqueCode
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
}
