package com.nexgo.n92pos.service

import android.util.Log
import com.nexgo.n92pos.model.CardInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class FlutterwavePaymentProcessor {
    
    data class FlutterwavePaymentResult(
        val success: Boolean,
        val transactionId: String?,
        val message: String,
        val paymentUrl: String? = null
    )
    
    companion object {
        private const val TAG = "FlutterwavePaymentProcessor"
        
        // Your Flutterwave credentials (replace with your actual keys)
        private const val PUBLIC_KEY = "FLWPUBK_TEST-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" // Your public key
        private const val SECRET_KEY = "FLWSECK_TEST-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" // Your secret key
        
        // Flutterwave API endpoints
        private const val BASE_URL = "https://api.flutterwave.com/v3"
        private const val PAYMENT_ENDPOINT = "/payments"
        private const val VERIFY_ENDPOINT = "/transactions"
        
        // Currency and country settings
        private const val CURRENCY = "USD"
        private const val COUNTRY = "US"
    }
    
    private val client = OkHttpClient()
    
    suspend fun processPayment(cardInfo: CardInfo, amount: Double): FlutterwavePaymentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing Flutterwave payment: $${amount} for card ending in ${cardInfo.cardNumber.takeLast(4)}")
            
            // Step 1: Initialize payment
            val initResult = initializePayment(cardInfo, amount)
            
            if (initResult.success) {
                Log.d(TAG, "Flutterwave payment initialized successfully: ${initResult.transactionId}")
                
                // Step 2: Verify payment (in real implementation, this would be done via webhook)
                val verifyResult = verifyPayment(initResult.transactionId!!)
                
                if (verifyResult.success) {
                    Log.d(TAG, "Flutterwave payment verified successfully")
                    FlutterwavePaymentResult(
                        success = true,
                        transactionId = initResult.transactionId,
                        message = "Payment successful! Amount: $${amount} processed via Flutterwave"
                    )
                } else {
                    Log.e(TAG, "Flutterwave payment verification failed: ${verifyResult.message}")
                    FlutterwavePaymentResult(
                        success = false,
                        transactionId = initResult.transactionId,
                        message = "Payment verification failed: ${verifyResult.message}"
                    )
                }
            } else {
                Log.e(TAG, "Failed to initialize Flutterwave payment: ${initResult.message}")
                FlutterwavePaymentResult(
                    success = false,
                    transactionId = null,
                    message = "Payment initialization failed: ${initResult.message}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Flutterwave payment error", e)
            FlutterwavePaymentResult(
                success = false,
                transactionId = null,
                message = "Payment processing error: ${e.message}"
            )
        }
    }
    
    private suspend fun initializePayment(cardInfo: CardInfo, amount: Double): FlutterwavePaymentResult = withContext(Dispatchers.IO) {
        try {
            val txRef = "POS_${System.currentTimeMillis()}_${cardInfo.cardNumber.takeLast(4)}"
            
            // Create payment request body
            val requestBody = JSONObject().apply {
                put("tx_ref", txRef)
                put("amount", amount)
                put("currency", CURRENCY)
                put("redirect_url", "https://your-pos-app.com/payment-callback")
                put("payment_options", "card")
                put("customer", JSONObject().apply {
                    put("email", "customer@pos.com")
                    put("name", "POS Customer")
                    put("phone_number", "1234567890")
                })
                put("customizations", JSONObject().apply {
                    put("title", "POS Payment")
                    put("description", "Point of Sale Transaction")
                    put("logo", "https://your-pos-app.com/logo.png")
                })
                put("card", JSONObject().apply {
                    put("card_number", cardInfo.cardNumber)
                    put("cvv", "123") // In real implementation, get from user input
                    put("expiry_month", cardInfo.expiryDate?.take(2) ?: "12")
                    put("expiry_year", "20${cardInfo.expiryDate?.takeLast(2) ?: "25"}")
                    put("currency", CURRENCY)
                    put("amount", amount)
                })
            }.toString()
            
            val request = Request.Builder()
                .url("$BASE_URL$PAYMENT_ENDPOINT")
                .addHeader("Authorization", "Bearer $SECRET_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            Log.d(TAG, "Making Flutterwave payment request to: $BASE_URL$PAYMENT_ENDPOINT")
            Log.d(TAG, "Request body: $requestBody")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "Flutterwave response: ${response.code} - $responseBody")
            
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                if (json.getString("status") == "success") {
                    val data = json.getJSONObject("data")
                    val transactionId = data.getString("tx_ref")
                    val paymentUrl = data.optString("link", null)
                    
                    Log.d(TAG, "Flutterwave payment initialized: $transactionId")
                    FlutterwavePaymentResult(
                        success = true,
                        transactionId = transactionId,
                        message = "Payment initialized successfully",
                        paymentUrl = paymentUrl
                    )
                } else {
                    Log.e(TAG, "Flutterwave payment initialization failed: ${json.optString("message", "Unknown error")}")
                    FlutterwavePaymentResult(
                        success = false,
                        transactionId = null,
                        message = json.optString("message", "Payment initialization failed")
                    )
                }
            } else {
                Log.e(TAG, "Flutterwave API error: ${response.code} - $responseBody")
                FlutterwavePaymentResult(
                    success = false,
                    transactionId = null,
                    message = "API error: ${response.code}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Flutterwave payment", e)
            FlutterwavePaymentResult(
                success = false,
                transactionId = null,
                message = "Payment initialization error: ${e.message}"
            )
        }
    }
    
    private suspend fun verifyPayment(transactionId: String): FlutterwavePaymentResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL$VERIFY_ENDPOINT/$transactionId/verify")
                .addHeader("Authorization", "Bearer $SECRET_KEY")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()
            
            Log.d(TAG, "Verifying Flutterwave payment: $transactionId")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "Flutterwave verification response: ${response.code} - $responseBody")
            
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                if (json.getString("status") == "success") {
                    val data = json.getJSONObject("data")
                    val status = data.getString("status")
                    
                    if (status == "successful") {
                        Log.d(TAG, "Flutterwave payment verified successfully")
                        FlutterwavePaymentResult(
                            success = true,
                            transactionId = transactionId,
                            message = "Payment verified successfully"
                        )
                    } else {
                        Log.e(TAG, "Flutterwave payment verification failed: $status")
                        FlutterwavePaymentResult(
                            success = false,
                            transactionId = transactionId,
                            message = "Payment verification failed: $status"
                        )
                    }
                } else {
                    Log.e(TAG, "Flutterwave verification failed: ${json.optString("message", "Unknown error")}")
                    FlutterwavePaymentResult(
                        success = false,
                        transactionId = transactionId,
                        message = json.optString("message", "Verification failed")
                    )
                }
            } else {
                Log.e(TAG, "Flutterwave verification API error: ${response.code} - $responseBody")
                FlutterwavePaymentResult(
                    success = false,
                    transactionId = transactionId,
                    message = "Verification API error: ${response.code}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying Flutterwave payment", e)
            FlutterwavePaymentResult(
                success = false,
                transactionId = transactionId,
                message = "Payment verification error: ${e.message}"
            )
        }
    }
    
    // Get account balance (if supported by Flutterwave)
    suspend fun getAccountBalance(): Double = withContext(Dispatchers.IO) {
        try {
            // Flutterwave doesn't have a direct balance API, but you can get transaction history
            // For now, return a placeholder
            Log.d(TAG, "Flutterwave balance check not directly supported")
            0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Flutterwave balance", e)
            0.0
        }
    }
}
