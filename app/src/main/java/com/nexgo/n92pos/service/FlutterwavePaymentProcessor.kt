package com.nexgo.n92pos.service

import android.content.Context
import android.util.Base64
import android.util.Log
import com.nexgo.n92pos.service.RealPaymentProcessor.PaymentTransaction
import com.nexgo.n92pos.model.CardInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Flutterwave Direct Card Payment Processor
 * Uses the official Flutterwave v3 API for direct card payments
 * Based on: https://developer.flutterwave.com/docs/collecting-payments/direct-card-charge
 */
class FlutterwavePaymentProcessor(
    private val context: Context,
    private val publicKey: String,
    private val secretKey: String,
    private val encryptionKey: String,
    private val isProduction: Boolean = true,
    private val currency: String = "KES"
) {
    
    private var currentCardInfo: com.nexgo.n92pos.model.CardInfo? = null
    
    companion object {
        private const val TAG = "FlutterwavePaymentProcessor"
        private const val RAVE_API_URL = "https://api.ravepay.co/flwv3-pug/getpaidx/api"
        private const val FLUTTERWAVE_API_URL = "https://api.flutterwave.com"
        
        // Flutterwave API credentials (replace with your actual credentials)
        private const val publicKey = "FLWPUBK_TEST-1234567890abcdef1234567890abcdef-1234567890abcdef1234567890abcdef"
        private const val secretKey = "FLWSECK_TEST-1234567890abcdef1234567890abcdef-1234567890abcdef1234567890abcdef"
        private const val encryptionKey = "1234567890abcdef1234567890abcdef" // 32 characters for 3DES-24
    }
    
    private val baseUrl = RAVE_API_URL
    private val client = OkHttpClient()
    private val random = SecureRandom()
    
    /**
     * Callback interface for payment processing events
     */
    interface FlutterwaveCallback {
        fun onPinRequired(flwRef: String, message: String = "Please enter your card PIN")
        fun onRedirectRequired(url: String, message: String = "Please complete 3DS authentication")
        fun onOtpRequired(flwRef: String, message: String = "Please enter the OTP sent to your phone")
        fun onAvsRequired(flwRef: String, message: String = "Please provide your billing address")
        fun onSuccess(transaction: PaymentTransaction)
        fun onFailure(error: String)
    }
    
    /**
     * Main entry point for processing card payments
     * Uses direct card charge API as per Flutterwave documentation
     */
    fun processPayment(
        cardInfo: com.nexgo.n92pos.model.CardInfo,
        amount: Double,
        customerEmail: String = "customer@pos.com",
        customerName: String = "POS Customer",
        callback: FlutterwaveCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting Flutterwave payment processing")
                Log.d(TAG, "Card: ${maskCardNumber(cardInfo.cardNumber)}")
                Log.d(TAG, "Amount: $${String.format("%.2f", amount)}")
                
                // Store card info for later use
                currentCardInfo = cardInfo
                
                // Parse expiry date
                val expiryParts = parseExpiryDate(cardInfo.expiryDate ?: "")
                if (expiryParts == null) {
                    withContext(Dispatchers.Main) {
                        callback.onFailure("Invalid expiry date format")
                    }
                    return@launch
                }
                
                // Create direct charge with card data
                val chargeResult = createDirectCharge(cardInfo, amount, customerEmail, customerName, expiryParts.first, expiryParts.second)
                if (chargeResult == null) {
                    withContext(Dispatchers.Main) {
                        callback.onFailure("Failed to create charge")
                    }
                    return@launch
                }
                
                // Handle the charge result
                handleChargeResult(chargeResult, callback)
                
            } catch (e: Exception) {
                Log.e(TAG, "Payment processing error", e)
                withContext(Dispatchers.Main) {
                    callback.onFailure("Payment processing failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Create direct charge using Flutterwave's direct card charge API
     */
    private suspend fun createDirectCharge(
        cardInfo: com.nexgo.n92pos.model.CardInfo,
        amount: Double,
        customerEmail: String,
        customerName: String,
        expiryMonth: String,
        expiryYear: String
    ): JSONObject? {
        return try {
            val txRef = "POS_${System.currentTimeMillis()}"
            
            // Prepare Rave API request payload (as per documentation)
            val cardData = JSONObject().apply {
                put("PBFPubKey", publicKey)
                put("cardno", cardInfo.cardNumber)
                put("cvv", "270") // Use actual CVV from your card
                put("expirymonth", expiryMonth)
                put("expiryyear", expiryYear)
                put("amount", amount.toString())
                put("email", customerEmail)
                put("phonenumber", "08012345678")
                put("firstname", customerName.split(" ").firstOrNull() ?: "Customer")
                put("lastname", customerName.split(" ").drop(1).joinToString(" ") ?: "Name")
                put("txRef", txRef)
                put("currency", currency)
                put("country", "NG")
                put("device_fingerprint", "POS_${System.currentTimeMillis()}")
            }

            // Encrypt the card data using 3DES-24 encryption (as required by Rave API)
            val encryptedCardData = encryptCardDetails(cardData.toString())

            // Prepare the final request payload (as per Rave API documentation)
            val chargeData = JSONObject().apply {
                put("PBFPubKey", publicKey)
                put("client", encryptedCardData)
                put("alg", "3DES-24")
            }
            
            // Log the request payload for debugging
            Log.d(TAG, "Rave API request payload: $chargeData")
            Log.d(TAG, "Amount being sent: $amount")
            
            val request = Request.Builder()
                .url("$baseUrl/charge")
                .post(chargeData.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "Create charge response: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                JSONObject(responseBody)
            } else {
                Log.e(TAG, "Rave API failed: ${response.code} - $responseBody")
                // Try Flutterwave v3 API as fallback
                Log.d(TAG, "Trying Flutterwave v3 API as fallback...")
                tryFlutterwaveV3API(cardInfo, amount, customerEmail, customerName, expiryMonth, expiryYear)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating charge", e)
            null
        }
    }
    
    /**
     * Fallback method to try Flutterwave v3 API
     */
    private suspend fun tryFlutterwaveV3API(
        cardInfo: com.nexgo.n92pos.model.CardInfo,
        amount: Double,
        customerEmail: String,
        customerName: String,
        expiryMonth: String,
        expiryYear: String
    ): JSONObject? {
        return try {
            val txRef = "POS_${System.currentTimeMillis()}"
            
            // Prepare Flutterwave v3 API request payload
            val chargeData = JSONObject().apply {
                put("tx_ref", txRef)
                put("amount", amount.toString())
                put("currency", currency)
                put("email", customerEmail)
                put("fullname", customerName)
                put("card_number", cardInfo.cardNumber)
                put("cvv", "270") // Use actual CVV from your card
                put("expiry_month", expiryMonth)
                put("expiry_year", expiryYear)
                put("redirect_url", "https://your-pos-app.com/payment-callback")
                put("client_ip", "192.168.0.1")
                put("device_fingerprint", "POS_${System.currentTimeMillis()}")
            }
            
            Log.d(TAG, "Flutterwave v3 API request payload: $chargeData")
            
            val request = Request.Builder()
                .url("$FLUTTERWAVE_API_URL/v3/charges?type=card")
                .post(chargeData.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $secretKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "Flutterwave v3 API response: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                JSONObject(responseBody)
            } else {
                Log.e(TAG, "Flutterwave v3 API also failed: ${response.code} - $responseBody")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error with Flutterwave v3 API", e)
            null
        }
    }
    
    /**
     * Encrypt card details using 3DES-24 encryption (as required by Rave API)
     */
    private fun encryptCardDetails(cardData: String): String {
        return try {
            // Convert encryption key to 24-byte key for 3DES-24
            val keyBytes = encryptionKey.toByteArray(Charsets.UTF_8)
            val key24 = if (keyBytes.size >= 24) {
                keyBytes.copyOf(24)
            } else {
                // Pad or repeat key to make it 24 bytes
                val paddedKey = ByteArray(24)
                for (i in keyBytes.indices) {
                    paddedKey[i % 24] = keyBytes[i]
                }
                paddedKey
            }
            
            val key = javax.crypto.spec.SecretKeySpec(key24, "DESede")
            val cipher = javax.crypto.Cipher.getInstance("DESede/ECB/PKCS5Padding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
            
            val encryptedBytes = cipher.doFinal(cardData.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "3DES-24 encryption failed", e)
            throw RuntimeException("Could not encrypt card data", e)
        }
    }
    
    /**
     * Handle the charge result and determine next steps
     */
    private suspend fun handleChargeResult(chargeResult: JSONObject, callback: FlutterwaveCallback) {
        try {
            val status = chargeResult.optString("status")
            val message = chargeResult.optString("message")
            val data = chargeResult.optJSONObject("data")
            
            if (status == "success" && data != null) {
                val flwRef = data.optString("flw_ref")
                val chargeStatus = data.optString("status")
                val processorResponse = data.optString("processor_response")
                val authModel = data.optString("auth_model")
                val authUrl = data.optString("auth_url")
                
                when {
                    authModel == "PIN" -> {
                        withContext(Dispatchers.Main) {
                            callback.onPinRequired(flwRef, processorResponse)
                        }
                    }
                    authModel == "OTP" -> {
                        withContext(Dispatchers.Main) {
                            callback.onOtpRequired(flwRef, processorResponse)
                        }
                    }
                    authModel == "3DS" && authUrl.isNotEmpty() -> {
                        withContext(Dispatchers.Main) {
                            callback.onRedirectRequired(authUrl, processorResponse)
                        }
                    }
                    chargeStatus == "successful" -> {
                        // Payment successful
                        val cardInfo = currentCardInfo ?: com.nexgo.n92pos.model.CardInfo("", "", null, null, null, null)
                        val transaction = PaymentTransaction(
                            transactionId = data.optString("id"),
                            amount = data.optDouble("amount"),
                            cardNumber = cardInfo.cardNumber,
                            cardType = cardInfo.cardType,
                            authCode = flwRef,
                            timestamp = System.currentTimeMillis(),
                            status = "successful",
                            processor = "flutterwave"
                        )
                        withContext(Dispatchers.Main) {
                            callback.onSuccess(transaction)
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            callback.onFailure("Payment failed: $processorResponse")
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback.onFailure("Payment failed: $message")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling charge result", e)
            withContext(Dispatchers.Main) {
                callback.onFailure("Error processing payment: ${e.message}")
            }
        }
    }
    
    /**
     * Authorize with PIN
     */
    fun authorizeWithPin(flwRef: String, pin: String, callback: FlutterwaveCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authData = JSONObject().apply {
                    put("type", "pin")
                    put("pin", pin)
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/charges/$flwRef/authorize")
                    .put(authData.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $secretKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                Log.d(TAG, "PIN authorization response: $responseBody")
                
                if (response.isSuccessful && responseBody != null) {
                    val result = JSONObject(responseBody)
                    handleChargeResult(result, callback)
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onFailure("PIN authorization failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error authorizing with PIN", e)
                withContext(Dispatchers.Main) {
                    callback.onFailure("PIN authorization error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Validate OTP
     */
    fun validateOtp(flwRef: String, otp: String, callback: FlutterwaveCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val otpData = JSONObject().apply {
                    put("type", "otp")
                    put("otp", otp)
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/charges/$flwRef/authorize")
                    .put(otpData.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $secretKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                Log.d(TAG, "OTP validation response: $responseBody")
                
                if (response.isSuccessful && responseBody != null) {
                    val result = JSONObject(responseBody)
                    handleChargeResult(result, callback)
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onFailure("OTP validation failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error validating OTP", e)
                withContext(Dispatchers.Main) {
                    callback.onFailure("OTP validation error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Parse expiry date from various formats
     */
    private fun parseExpiryDate(expiryDate: String): Pair<String, String>? {
        return try {
            val cleaned = expiryDate.replace(Regex("[^0-9]"), "")
            
            when (cleaned.length) {
                4 -> {
                    // YYMM format (e.g., "2810" = October 2028)
                    val year = "20${cleaned.substring(0, 2)}"
                    val month = cleaned.substring(2, 4)
                    Pair(month, year)
                }
                6 -> {
                    // YYYYMM format
                    val year = cleaned.substring(0, 4)
                    val month = cleaned.substring(4, 6)
                    Pair(month, year)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing expiry date: $expiryDate", e)
            null
        }
    }
    
    
    /**
     * Mask card number for logging
     */
    private fun maskCardNumber(cardNumber: String): String {
        return if (cardNumber.length >= 4) {
            "**** **** **** ${cardNumber.takeLast(4)}"
        } else {
            "****"
        }
    }
    
}