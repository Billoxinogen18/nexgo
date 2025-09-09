package com.nexgo.n92pos.service

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

class BankService(private val context: Context) {
    
    companion object {
        private const val TAG = "BankService"
        
        // Bank API endpoints
        private const val BANK_API_URL = "https://api.bankprocessor.com/v1/transactions"
        private const val ACCOUNT_API_URL = "https://api.bankprocessor.com/v1/accounts"
        
        // Bank account details (in production, store securely)
        private const val BANK_ACCOUNT_NUMBER = "1234567890"
        private const val BANK_ROUTING_NUMBER = "021000021"
        private const val BANK_NAME = "Chase Bank"
    }
    
    interface BankCallback {
        fun onSuccess(transactionId: String, bankReference: String)
        fun onFailure(error: String)
    }
    
    fun processBankTransfer(
        amount: String,
        transactionId: String,
        callback: BankCallback
    ) {
        Thread {
            try {
                val url = URL(BANK_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${getBankApiKey()}")
                
                val requestBody = JSONObject().apply {
                    put("amount", amount)
                    put("currency", "USD")
                    put("account_number", BANK_ACCOUNT_NUMBER)
                    put("routing_number", BANK_ROUTING_NUMBER)
                    put("transaction_id", transactionId)
                    put("description", "POS Terminal Payment")
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
                    val bankReference = responseJson.optString("bank_reference", generateBankReference())
                    callback.onSuccess(transactionId, bankReference)
                } else {
                    Log.e(TAG, "Bank transfer error: $responseCode - $response")
                    callback.onFailure("Bank transfer error: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing bank transfer", e)
                callback.onFailure("Bank transfer error: ${e.message}")
            }
        }.start()
    }
    
    fun getBankAccountInfo(): BankAccountInfo {
        return BankAccountInfo(
            accountNumber = maskAccountNumber(BANK_ACCOUNT_NUMBER),
            routingNumber = BANK_ROUTING_NUMBER,
            bankName = BANK_NAME,
            accountType = "Business Checking"
        )
    }
    
    fun getAccountBalance(): String {
        return try {
            val url = URL("$ACCOUNT_API_URL/$BANK_ACCOUNT_NUMBER/balance")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer ${getBankApiKey()}")
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val balance = json.getDouble("balance")
                String.format("%.2f", balance)
            } else {
                "0.00"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting account balance", e)
            "0.00"
        }
    }
    
    fun getTransactionHistory(): List<BankTransaction> {
        return try {
            val url = URL("$ACCOUNT_API_URL/$BANK_ACCOUNT_NUMBER/transactions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer ${getBankApiKey()}")
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val transactions = json.getJSONArray("transactions")
                
                val bankTransactions = mutableListOf<BankTransaction>()
                for (i in 0 until transactions.length()) {
                    val tx = transactions.getJSONObject(i)
                    bankTransactions.add(
                        BankTransaction(
                            id = tx.getString("id"),
                            amount = tx.getString("amount"),
                            type = tx.getString("type"),
                            description = tx.getString("description"),
                            timestamp = tx.getLong("timestamp"),
                            reference = tx.optString("reference", "")
                        )
                    )
                }
                bankTransactions
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transaction history", e)
            emptyList()
        }
    }
    
    private fun maskAccountNumber(accountNumber: String): String {
        return if (accountNumber.length >= 4) {
            "****${accountNumber.takeLast(4)}"
        } else {
            "****$accountNumber"
        }
    }
    
    private fun generateBankReference(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "BANK${timestamp}${random}"
    }
    
    private fun getBankApiKey(): String {
        // In production, store this securely
        return "bank_api_key_here"
    }
    
    private fun getMerchantId(): String {
        return "000000000000001"
    }
    
    data class BankAccountInfo(
        val accountNumber: String,
        val routingNumber: String,
        val bankName: String,
        val accountType: String
    )
    
    data class BankTransaction(
        val id: String,
        val amount: String,
        val type: String,
        val description: String,
        val timestamp: Long,
        val reference: String
    )
}
