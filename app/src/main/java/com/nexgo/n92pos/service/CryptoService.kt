package com.nexgo.n92pos.service

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

class CryptoService(private val context: Context) {
    
    companion object {
        private const val TAG = "CryptoService"
        private const val WALLET_ADDRESS = "0x29014271d71e3691cBaF01a26A4AC1502e2C4048"
        
        // Crypto APIs
        private const val ETHEREUM_API_URL = "https://api.etherscan.io/api"
        private const val CRYPTO_PRICE_API = "https://api.coingecko.com/api/v3/simple/price"
        private const val CRYPTO_PAYMENT_API = "https://api.cryptopayment.com/v1/payments"
    }
    
    interface CryptoCallback {
        fun onSuccess(txHash: String, amount: String, currency: String)
        fun onFailure(error: String)
    }
    
    fun processCryptoPayment(
        usdAmount: String,
        transactionId: String,
        callback: CryptoCallback
    ) {
        Thread {
            try {
                // Convert USD to ETH
                val ethAmount = convertUsdToEth(usdAmount)
                if (ethAmount == null) {
                    callback.onFailure("Failed to convert USD to ETH")
                    return@Thread
                }
                
                // Create crypto payment
                createCryptoPayment(ethAmount, transactionId) { success, txHash, error ->
                    if (success) {
                        callback.onSuccess(txHash ?: "unknown", ethAmount, "ETH")
                    } else {
                        callback.onFailure(error ?: "Crypto payment failed")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing crypto payment", e)
                callback.onFailure("Crypto payment error: ${e.message}")
            }
        }.start()
    }
    
    private fun convertUsdToEth(usdAmount: String): String? {
        return try {
            val url = URL("$CRYPTO_PRICE_API?ids=ethereum&vs_currencies=usd")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val ethPrice = json.getJSONObject("ethereum").getDouble("usd")
                
                val usdValue = usdAmount.toDouble()
                val ethValue = usdValue / ethPrice
                
                // Round to 6 decimal places
                BigDecimal(ethValue).setScale(6, BigDecimal.ROUND_HALF_UP).toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting USD to ETH", e)
            null
        }
    }
    
    private fun createCryptoPayment(
        ethAmount: String,
        transactionId: String,
        callback: (Boolean, String?, String?) -> Unit
    ) {
        try {
            val url = URL(CRYPTO_PAYMENT_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${getCryptoApiKey()}")
            
            val requestBody = JSONObject().apply {
                put("amount", ethAmount)
                put("currency", "ETH")
                put("wallet_address", WALLET_ADDRESS)
                put("transaction_id", transactionId)
                put("network", "ethereum")
                put("gas_limit", "21000")
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
                val txHash = responseJson.optString("transaction_hash", null)
                callback(true, txHash, null)
            } else {
                Log.e(TAG, "Crypto payment API error: $responseCode - $response")
                callback(false, null, "Crypto payment API error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating crypto payment", e)
            callback(false, null, "Network error: ${e.message}")
        }
    }
    
    fun getWalletBalance(): String {
        return try {
            val url = URL("$ETHEREUM_API_URL?module=account&action=balance&address=$WALLET_ADDRESS&tag=latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val weiBalance = json.getString("result")
                
                // Convert from Wei to ETH (1 ETH = 10^18 Wei)
                val ethBalance = BigDecimal(weiBalance).divide(BigDecimal("1000000000000000000"))
                ethBalance.setScale(6, BigDecimal.ROUND_HALF_UP).toString()
            } else {
                "0.000000"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting wallet balance", e)
            "0.000000"
        }
    }
    
    fun getWalletAddress(): String {
        return WALLET_ADDRESS
    }
    
    fun getTransactionHistory(): List<CryptoTransaction> {
        return try {
            val url = URL("$ETHEREUM_API_URL?module=account&action=txlist&address=$WALLET_ADDRESS&startblock=0&endblock=99999999&sort=desc")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val transactions = json.getJSONArray("result")
                
                val cryptoTransactions = mutableListOf<CryptoTransaction>()
                for (i in 0 until transactions.length()) {
                    val tx = transactions.getJSONObject(i)
                    cryptoTransactions.add(
                        CryptoTransaction(
                            hash = tx.getString("hash"),
                            from = tx.getString("from"),
                            to = tx.getString("to"),
                            value = tx.getString("value"),
                            timestamp = tx.getLong("timeStamp"),
                            blockNumber = tx.getString("blockNumber")
                        )
                    )
                }
                cryptoTransactions
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transaction history", e)
            emptyList()
        }
    }
    
    private fun getCryptoApiKey(): String {
        // In production, store this securely
        return "crypto_api_key_here"
    }
    
    data class CryptoTransaction(
        val hash: String,
        val from: String,
        val to: String,
        val value: String,
        val timestamp: Long,
        val blockNumber: String
    )
}
