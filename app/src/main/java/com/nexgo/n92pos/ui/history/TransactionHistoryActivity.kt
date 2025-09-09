package com.nexgo.n92pos.ui.history

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityTransactionHistoryBinding
import com.nexgo.n92pos.model.Transaction
import com.nexgo.n92pos.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var adapter: TransactionHistoryAdapter
    private val transactions = mutableListOf<Transaction>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadSampleData()
    }
    
    private fun setupUI() {
        binding.apply {
            btnBack.setOnClickListener { finish() }
            btnRefresh.setOnClickListener { loadSampleData() }
            btnClear.setOnClickListener { clearHistory() }
        }
        
        adapter = TransactionHistoryAdapter(transactions) { transaction ->
            showTransactionDetails(transaction)
        }
        
        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
            adapter = this@TransactionHistoryActivity.adapter
        }
    }
    
    private fun loadSampleData() {
        transactions.clear()
        
        // Add sample transactions
        transactions.addAll(listOf(
            Transaction(
                id = "TXN001",
                amount = "25.50",
                type = "SALE",
                timestamp = System.currentTimeMillis() - 3600000,
                status = TransactionStatus.SUCCESS,
                cardNumber = "****1234",
                authCode = "123456"
            ),
            Transaction(
                id = "TXN002",
                amount = "100.00",
                type = "SALE",
                timestamp = System.currentTimeMillis() - 7200000,
                status = TransactionStatus.SUCCESS,
                cardNumber = "****5678",
                authCode = "789012"
            ),
            Transaction(
                id = "TXN003",
                amount = "50.25",
                type = "REFUND",
                timestamp = System.currentTimeMillis() - 10800000,
                status = TransactionStatus.SUCCESS,
                cardNumber = "****9012",
                authCode = "345678"
            ),
            Transaction(
                id = "TXN004",
                amount = "75.00",
                type = "SALE",
                timestamp = System.currentTimeMillis() - 14400000,
                status = TransactionStatus.FAILED,
                cardNumber = "****3456",
                authCode = null
            )
        ))
        
        adapter.notifyDataSetChanged()
        updateSummary()
    }
    
    private fun clearHistory() {
        transactions.clear()
        adapter.notifyDataSetChanged()
        updateSummary()
        Toast.makeText(this, "Transaction history cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateSummary() {
        val totalTransactions = transactions.size
        val successfulTransactions = transactions.count { it.status == TransactionStatus.SUCCESS }
        val totalAmount = transactions
            .filter { it.status == TransactionStatus.SUCCESS }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        
        binding.tvSummary.text = "Total: $totalTransactions | Success: $successfulTransactions | Amount: $${String.format("%.2f", totalAmount)}"
    }
    
    private fun showTransactionDetails(transaction: Transaction) {
        val details = """
            Transaction ID: ${transaction.id}
            Amount: $${transaction.amount}
            Type: ${transaction.type}
            Status: ${transaction.status}
            Card: ${transaction.cardNumber}
            Auth Code: ${transaction.authCode ?: "N/A"}
            Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp))}
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Transaction Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
}
