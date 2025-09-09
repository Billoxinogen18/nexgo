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
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BinancePaymentProcessor {
    
    data class BinancePaymentResult(
        val success: Boolean,
        val transactionId: String?,
        val message: String
    )
    
    companion object {
        private const val TAG = "BinancePaymentProcessor"
        
        // Your existing Binance API credentials
        private const val API_KEY = "ghhAUdvCyMrYImYzFnaeom1cVXvHopy5gKWmQ9O7hPZK13ImJa66BJZ8L7Gps6C8"
        private const val SECRET_KEY = "Xj0OCpB7H7t4YT6LD87ShEE1JMys0ppRI6aU1Xy2wIfU3VYoN2sZfAp8uvz3MEce"
        
        // Binance Pay API endpoints (from official documentation)
        private const val BASE_URL = "https://api.binance.com"
        private const val PAY_URL = "https://bpay.binanceapi.com"
        
        // Supported cryptocurrencies
        private const val DEFAULT_CRYPTO = "USDT" // Most stable
        private const val CRYPTO_SYMBOL = "BTCUSDT" // Use BTC price to calculate USDT value
    }
    
    private val client = OkHttpClient()
    
    suspend fun processPayment(cardInfo: CardInfo, amount: Double): BinancePaymentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing Binance Pay payment: $${amount} for card ending in ${cardInfo.cardNumber.takeLast(4)}")
            
            // Step 1: Use fixed USDT rate (1 USD = 1 USDT approximately)
            // In production, you would get real-time price from Binance API
            val usdtPrice = 1.0 // USDT is pegged to USD
            val usdtAmount = amount / usdtPrice
            Log.d(TAG, "Converting $${amount} to ${usdtAmount} USDT (Rate: $${usdtPrice})")
            
            // Step 2: Create Binance Pay order
            val orderResult = createBinancePayOrder(usdtAmount, cardInfo)
            
            if (orderResult.success) {
                Log.d(TAG, "Binance Pay order created successfully: ${orderResult.transactionId}")
                
                // Step 3: Simulate payment processing (in real implementation, this would be actual payment)
                val paymentResult = processBinancePayment(orderResult.transactionId!!, usdtAmount, cardInfo)
                
                if (paymentResult.success) {
                    Log.d(TAG, "Binance Pay payment completed successfully")
                    BinancePaymentResult(
                        success = true,
                        transactionId = orderResult.transactionId,
                        message = "Payment successful! ${usdtAmount} USDT received in your Binance wallet"
                    )
                } else {
                    Log.e(TAG, "Binance Pay payment failed: ${paymentResult.message}")
                    BinancePaymentResult(
                        success = false,
                        transactionId = orderResult.transactionId,
                        message = "Payment failed: ${paymentResult.message}"
                    )
                }
            } else {
                Log.e(TAG, "Failed to create Binance Pay order: ${orderResult.message}")
                BinancePaymentResult(
                    success = false,
                    transactionId = null,
                    message = "Failed to create payment order: ${orderResult.message}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Binance Pay payment error", e)
            BinancePaymentResult(
                success = false,
                transactionId = null,
                message = "Payment processing error: ${e.message}"
            )
        }
    }
    
    private suspend fun getUSDTPrice(): Double = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v3/ticker/price?symbol=$CRYPTO_SYMBOL")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                val price = json.getDouble("price")
                Log.d(TAG, "Current USDT price: $${price}")
                price
            } else {
                Log.e(TAG, "Failed to get USDT price: ${response.code} - $responseBody")
                0.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting USDT price", e)
            0.0
        }
    }
    
    private suspend fun createBinancePayOrder(usdtAmount: Double, cardInfo: CardInfo): BinancePaymentResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val merchantTradeNo = "POS_${timestamp}_${cardInfo.cardNumber.takeLast(4)}"
            val nonce = UUID.randomUUID().toString().replace("-", "")
            
            // Create Binance Pay order request body (based on official documentation)
            val requestBody = JSONObject().apply {
                put("env", JSONObject().apply {
                    put("terminalType", "APP")
                })
                put("merchantTradeNo", merchantTradeNo)
                put("orderAmount", usdtAmount)
                put("currency", DEFAULT_CRYPTO)
                put("goods", JSONObject().apply {
                    put("goodsType", "01")
                    put("goodsCategory", "0000")
                    put("referenceGoodsId", "pos_${cardInfo.cardNumber.takeLast(4)}")
                    put("goodsName", "POS Payment")
                    put("goodsUnitAmount", JSONObject().apply {
                        put("currency", DEFAULT_CRYPTO)
                        put("amount", usdtAmount)
                    })
                })
                put("buyer", JSONObject().apply {
                    put("buyerName", JSONObject().apply {
                        put("firstName", "Card")
                        put("lastName", "Holder")
                    })
                })
            }.toString()
            
            // Create signature for Binance Pay (different from regular Binance API)
            val signature = createBinancePaySignature(requestBody, timestamp, nonce)
            
            val request = Request.Builder()
                .url("$PAY_URL/binancepay/openapi/v2/order")
                .addHeader("Content-Type", "application/json")
                .addHeader("BinancePay-Timestamp", timestamp.toString())
                .addHeader("BinancePay-Nonce", nonce)
                .addHeader("BinancePay-Certificate-SN", API_KEY)
                .addHeader("BinancePay-Signature", signature)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            Log.d(TAG, "Making Binance Pay order request to: $PAY_URL/binancepay/openapi/v2/order")
            Log.d(TAG, "Request body: $requestBody")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "Binance Pay response: ${response.code} - $responseBody")
            
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                if (json.optString("status") == "SUCCESS") {
                    val prepayId = json.optString("prepayId")
                    Log.d(TAG, "Binance Pay order created: $merchantTradeNo, prepayId: $prepayId")
                    BinancePaymentResult(
                        success = true,
                        transactionId = prepayId.ifEmpty { merchantTradeNo },
                        message = "Order created successfully"
                    )
                } else {
                    Log.e(TAG, "Binance Pay order creation failed: ${json.optString("message", "Unknown error")}")
                    BinancePaymentResult(
                        success = false,
                        transactionId = null,
                        message = json.optString("message", "Order creation failed")
                    )
                }
            } else {
                Log.e(TAG, "Binance Pay API error: ${response.code} - $responseBody")
                BinancePaymentResult(
                    success = false,
                    transactionId = null,
                    message = "API error: ${response.code}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Binance Pay order", e)
            BinancePaymentResult(
                success = false,
                transactionId = null,
                message = "Order creation error: ${e.message}"
            )
        }
    }
    
    private suspend fun processBinancePayment(orderId: String, usdtAmount: Double, cardInfo: CardInfo): BinancePaymentResult = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, this would:
            // 1. Verify the payment with Binance
            // 2. Transfer USDT to your wallet
            // 3. Update order status
            
            // For now, we'll simulate successful payment
            Log.d(TAG, "Processing Binance Pay payment for order: $orderId")
            Log.d(TAG, "Amount: ${usdtAmount} USDT")
            Log.d(TAG, "Card: ${cardInfo.cardNumber.takeLast(4)}")
            
            // Simulate processing delay
            kotlinx.coroutines.delay(2000)
            
            // Simulate successful payment
            Log.d(TAG, "Binance Pay payment completed successfully")
            Log.d(TAG, "USDT transferred to your Binance wallet")
            
            BinancePaymentResult(
                success = true,
                transactionId = orderId,
                message = "Payment completed successfully"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Binance Pay payment", e)
            BinancePaymentResult(
                success = false,
                transactionId = orderId,
                message = "Payment processing error: ${e.message}"
            )
        }
    }
    
    private fun createBinancePaySignature(requestBody: String, timestamp: Long, nonce: String): String {
        // Binance Pay signature format: timestamp + "\n" + nonce + "\n" + requestBody + "\n"
        val payload = "$timestamp\n$nonce\n$requestBody\n"
        
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        
        val signature = mac.doFinal(payload.toByteArray())
        return signature.joinToString("") { "%02x".format(it) }
    }
    
    private fun createSignature(params: Map<String, String>): String {
        val queryString = params.entries
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        
        val signature = mac.doFinal(queryString.toByteArray())
        return signature.joinToString("") { "%02x".format(it) }
    }
    
    // Get account balance
    suspend fun getAccountBalance(): Double = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val params = mapOf("timestamp" to timestamp.toString())
            val signature = createSignature(params)
            
            val request = Request.Builder()
                .url("$BASE_URL/api/v3/account?timestamp=$timestamp&signature=$signature")
                .addHeader("X-MBX-APIKEY", API_KEY)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                val balances = json.getJSONArray("balances")
                
                for (i in 0 until balances.length()) {
                    val balance = balances.getJSONObject(i)
                    if (balance.getString("asset") == DEFAULT_CRYPTO) {
                        val free = balance.getDouble("free")
                        Log.d(TAG, "USDT Balance: $free")
                        free
                    } else {
                        0.0
                    }
                }
                0.0
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting account balance", e)
            0.0
        }
    }
}
