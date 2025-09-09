package com.nexgo.n92pos.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityMainBinding
import com.nexgo.n92pos.ui.payment.PaymentActivity
import com.nexgo.n92pos.ui.settings.SettingsActivity
import com.nexgo.n92pos.ui.history.TransactionHistoryActivity
import com.nexgo.n92pos.ui.crypto.CryptoWalletActivity
import com.nexgo.n92pos.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Set up click listeners for the card views
        // Payment card (first card in the grid)
        val paymentCard = findViewById<androidx.cardview.widget.CardView>(R.id.card_payment)
        paymentCard?.setOnClickListener {
            startActivity(Intent(this@MainActivity, PaymentActivity::class.java))
        }
        
        // Settings card (second card in the grid)
        val settingsCard = findViewById<androidx.cardview.widget.CardView>(R.id.card_settings)
        settingsCard?.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        
        // History card (third card in the grid)
        val historyCard = findViewById<androidx.cardview.widget.CardView>(R.id.card_history)
        historyCard?.setOnClickListener {
            startActivity(Intent(this@MainActivity, TransactionHistoryActivity::class.java))
        }
        
        // Reports card (fourth card in the grid)
        val reportsCard = findViewById<androidx.cardview.widget.CardView>(R.id.card_reports)
        reportsCard?.setOnClickListener {
            showReportsMenu()
        }
        
        // Crypto wallet card (fifth card in the grid)
        val cryptoCard = findViewById<androidx.cardview.widget.CardView>(R.id.card_crypto)
        cryptoCard?.setOnClickListener {
            startActivity(Intent(this@MainActivity, CryptoWalletActivity::class.java))
        }
    }
    
    private fun showReportsMenu() {
        val options = arrayOf("Daily Report", "Weekly Report", "Monthly Report", "Transaction Summary")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reports")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> generateReport("Daily")
                    1 -> generateReport("Weekly")
                    2 -> generateReport("Monthly")
                    3 -> generateReport("Summary")
                }
            }
            .show()
    }
    
    private fun generateReport(type: String) {
        Toast.makeText(this, "Generating $type report...", Toast.LENGTH_SHORT).show()
        // TODO: Implement actual report generation
    }
    
    private fun observeViewModel() {
        viewModel.deviceStatus.observe(this) { status ->
            binding.tvDeviceStatus.text = status
        }
        
        viewModel.lastTransaction.observe(this) { transaction ->
            if (transaction != null) {
                binding.tvLastTransaction.text = "Last: ${transaction.amount} - ${transaction.status}"
            }
        }
    }
}
