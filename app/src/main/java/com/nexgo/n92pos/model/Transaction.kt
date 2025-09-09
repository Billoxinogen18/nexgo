package com.nexgo.n92pos.model

import java.util.*

data class Transaction(
    val id: String,
    val amount: String,
    val type: String, // SALE, REFUND, VOID, etc.
    val timestamp: Long,
    val status: TransactionStatus,
    val cardNumber: String? = null,
    val cardType: String? = null, // VISA, MASTERCARD, etc.
    val authCode: String? = null,
    val referenceNumber: String? = null,
    val traceNumber: String? = null,
    val merchantId: String? = null,
    val terminalId: String? = null,
    val emvData: String? = null,
    val pinBlock: String? = null
)

enum class TransactionStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    VOIDED
}

data class CardInfo(
    val cardNumber: String,
    val cardType: String,
    val expiryDate: String?,
    val serviceCode: String?,
    val track2Data: String?,
    val emvData: String?
)

data class PaymentRequest(
    val amount: String,
    val currency: String = "USD",
    val transactionType: String = "SALE",
    val merchantId: String,
    val terminalId: String
)

data class PaymentResponse(
    val success: Boolean,
    val transactionId: String?,
    val authCode: String?,
    val responseCode: String?,
    val message: String?
)
