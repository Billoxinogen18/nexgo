package com.nexgo.n92pos.service

import android.content.Context
import android.util.Log
import com.nexgo.n92pos.model.Transaction
import com.nexgo.n92pos.model.TransactionStatus
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class PaymentProcessingService(private val context: Context) {
    
    companion object {
        private const val TAG = "PaymentProcessingService"
        
        // Crypto wallet address
        private const val CRYPTO_WALLET_ADDRESS = "0x29014271d71e3691cBaF01a26A4AC1502e2C4048"
        
        // Payment processor endpoints
        private const val PAYMENT_PROCESSOR_URL = "https://api.paymentprocessor.com/v1/transactions"
        private const val CRYPTO_PROCESSOR_URL = "https://api.cryptoprocessor.com/v1/payments"
        
        // Protocol codes
        private const val PROTOCOL_101 = "101" // Credit Card Sale
        private const val PROTOCOL_201 = "201" // Debit Card Sale
    }
    
    interface PaymentCallback {
        fun onSuccess(transaction: Transaction, authCode: String, cryptoTxHash: String?)
        fun onFailure(error: String)
    }
    
    fun processPayment(
        amount: String,
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        cardholderName: String,
        callback: PaymentCallback
    ) {
        try {
            val transaction = createTransaction(amount, cardNumber)
            
            // Step 1: Process with payment processor (Protocol 101/201)
            processWithPaymentProcessor(transaction, cardNumber, expiryDate, cvv, cardholderName) { success, authCode, error ->
                if (success && authCode != null) {
                    // Step 2: Process crypto payment
                    processCryptoPayment(transaction, authCode) { cryptoSuccess, cryptoTxHash ->
                        if (cryptoSuccess) {
                            val finalTransaction = transaction.copy(
                                status = TransactionStatus.SUCCESS,
                                authCode = authCode
                            )
                            callback.onSuccess(finalTransaction, authCode, cryptoTxHash)
                        } else {
                            callback.onFailure("Crypto processing failed")
                        }
                    }
                } else {
                    callback.onFailure(error ?: "Payment processing failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing payment", e)
            callback.onFailure("Payment processing error: ${e.message}")
        }
    }
    
    private fun createTransaction(amount: String, cardNumber: String): Transaction {
        return Transaction(
            id = generateTransactionId(),
            amount = amount,
            type = "SALE",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.PENDING,
            cardNumber = maskCardNumber(cardNumber)
        )
    }
    
    private fun processWithPaymentProcessor(
        transaction: Transaction,
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        cardholderName: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        Thread {
            try {
                val url = URL(PAYMENT_PROCESSOR_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${getApiKey()}")
                
                val requestBody = JSONObject().apply {
                    put("amount", (transaction.amount.toDouble() * 100).toInt()) // Convert to cents
                    put("currency", "USD")
                    put("card_number", cardNumber)
                    put("expiry_date", expiryDate)
                    put("cvv", cvv)
                    put("cardholder_name", cardholderName)
                    put("transaction_id", transaction.id)
                    put("protocol", if (isDebitCard(cardNumber)) PROTOCOL_201 else PROTOCOL_101)
                    put("merchant_id", getMerchantId())
                    put("terminal_id", getTerminalId())
                }
                
                connection.doOutput = true
                connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }
                
                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream.bufferedReader().readText()
                }
                
                if (responseCode == 200) {
                    val responseJson = JSONObject(response)
                    val authCode = responseJson.optString("auth_code", generateAuthCode())
                    callback(true, authCode, null)
                } else {
                    Log.e(TAG, "Payment processor error: $responseCode - $response")
                    callback(false, null, "Payment processor error: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling payment processor", e)
                callback(false, null, "Network error: ${e.message}")
            }
        }.start()
    }
    
    private fun processCryptoPayment(
        transaction: Transaction,
        authCode: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Thread {
            try {
                val url = URL(CRYPTO_PROCESSOR_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${getCryptoApiKey()}")
                
                val requestBody = JSONObject().apply {
                    put("amount", transaction.amount)
                    put("currency", "USD")
                    put("wallet_address", CRYPTO_WALLET_ADDRESS)
                    put("transaction_id", transaction.id)
                    put("auth_code", authCode)
                    put("merchant_id", getMerchantId())
                }
                
                connection.doOutput = true
                connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }
                
                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream.bufferedReader().readText()
                }
                
                if (responseCode == 200) {
                    val responseJson = JSONObject(response)
                    val cryptoTxHash = responseJson.optString("transaction_hash", null)
                    callback(true, cryptoTxHash)
                } else {
                    Log.e(TAG, "Crypto processor error: $responseCode - $response")
                    callback(false, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling crypto processor", e)
                callback(false, null)
            }
        }.start()
    }
    
    private fun isDebitCard(cardNumber: String): Boolean {
        // Simple BIN check for debit cards (this is a simplified example)
        val bin = cardNumber.take(6)
        val debitBins = listOf("400000", "500000", "600000") // Example BINs
        return debitBins.any { bin.startsWith(it) }
    }
    
    private fun maskCardNumber(cardNumber: String): String {
        return if (cardNumber.length >= 4) {
            "**** **** **** ${cardNumber.takeLast(4)}"
        } else {
            "**** **** **** $cardNumber"
        }
    }
    
    private fun generateTransactionId(): String {
        val timestamp = System.currentTimeMillis()
        val random = Random().nextInt(1000)
        return "TXN${timestamp}${random.toString().padStart(3, '0')}"
    }
    
    private fun generateAuthCode(): String {
        return (100000 + Random().nextInt(900000)).toString()
    }
    
    private fun getApiKey(): String {
        // In production, store this securely
        return "pk_test_your_payment_processor_key"
    }
    
    private fun getCryptoApiKey(): String {
        // In production, store this securely
        return "crypto_api_key_here"
    }
    
    private fun getMerchantId(): String {
        // Get from shared preferences or configuration
        return "000000000000001"
    }
    
    private fun getTerminalId(): String {
        // Get from shared preferences or configuration
        return "00000001"
    }
    
    fun getCryptoWalletAddress(): String {
        return CRYPTO_WALLET_ADDRESS
    }
    
    fun getCryptoBalance(): String {
        // This would call a crypto API to get balance
        return "0.00 ETH"
    }
}
