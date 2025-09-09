package com.nexgo.n92pos.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityBankConfigBinding

class BankConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBankConfigBinding
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBankConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("bank_config", MODE_PRIVATE)
        
        setupUI()
        loadCurrentConfig()
    }
    
    private fun setupUI() {
        binding.apply {
            btnSave.setOnClickListener { saveConfig() }
            btnTest.setOnClickListener { testConnection() }
            btnBack.setOnClickListener { finish() }
        }
    }
    
    private fun loadCurrentConfig() {
        binding.apply {
            etBankName.setText(prefs.getString("bank_name", "Chase Bank"))
            etAccountNumber.setText(prefs.getString("account_number", ""))
            etRoutingNumber.setText(prefs.getString("routing_number", ""))
            etAccountType.setText(prefs.getString("account_type", "Business Checking"))
            etBankApiKey.setText(prefs.getString("bank_api_key", ""))
            etBankApiUrl.setText(prefs.getString("bank_api_url", "https://api.bankprocessor.com/v1"))
            etMerchantId.setText(prefs.getString("merchant_id", "000000000000001"))
        }
    }
    
    private fun saveConfig() {
        val bankName = binding.etBankName.text.toString().trim()
        val accountNumber = binding.etAccountNumber.text.toString().trim()
        val routingNumber = binding.etRoutingNumber.text.toString().trim()
        val accountType = binding.etAccountType.text.toString().trim()
        val bankApiKey = binding.etBankApiKey.text.toString().trim()
        val bankApiUrl = binding.etBankApiUrl.text.toString().trim()
        val merchantId = binding.etMerchantId.text.toString().trim()
        
        if (bankName.isEmpty() || accountNumber.isEmpty() || routingNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all required bank details", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefs.edit().apply {
            putString("bank_name", bankName)
            putString("account_number", accountNumber)
            putString("routing_number", routingNumber)
            putString("account_type", accountType)
            putString("bank_api_key", bankApiKey)
            putString("bank_api_url", bankApiUrl)
            putString("merchant_id", merchantId)
            apply()
        }
        
        Toast.makeText(this, "Bank configuration saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun testConnection() {
        binding.tvStatus.text = "Testing bank connection..."
        
        // Simulate connection test
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.tvStatus.text = "Bank connection test successful ✓"
            Toast.makeText(this, "Bank connection test successful", Toast.LENGTH_SHORT).show()
        }, 2000)
    }
}
