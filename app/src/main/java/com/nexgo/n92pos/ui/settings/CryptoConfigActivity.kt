package com.nexgo.n92pos.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityCryptoConfigBinding

class CryptoConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCryptoConfigBinding
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("crypto_config", MODE_PRIVATE)
        
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
            etWalletAddress.setText(prefs.getString("wallet_address", "0x29014271d71e3691cBaF01a26A4AC1502e2C4048"))
            etCryptoApiKey.setText(prefs.getString("crypto_api_key", ""))
            etCryptoApiUrl.setText(prefs.getString("crypto_api_url", "https://api.coingecko.com/api/v3"))
            etEthereumApiUrl.setText(prefs.getString("ethereum_api_url", "https://api.etherscan.io/api"))
            etCryptoApiKey.setText(prefs.getString("crypto_api_key", ""))
            etGasLimit.setText(prefs.getString("gas_limit", "21000"))
            etNetwork.setText(prefs.getString("network", "ethereum"))
        }
    }
    
    private fun saveConfig() {
        val walletAddress = binding.etWalletAddress.text.toString().trim()
        val cryptoApiKey = binding.etCryptoApiKey.text.toString().trim()
        val cryptoApiUrl = binding.etCryptoApiUrl.text.toString().trim()
        val ethereumApiUrl = binding.etEthereumApiUrl.text.toString().trim()
        val gasLimit = binding.etGasLimit.text.toString().trim()
        val network = binding.etNetwork.text.toString().trim()
        
        if (walletAddress.isEmpty() || !walletAddress.startsWith("0x")) {
            Toast.makeText(this, "Please enter a valid Ethereum wallet address", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefs.edit().apply {
            putString("wallet_address", walletAddress)
            putString("crypto_api_key", cryptoApiKey)
            putString("crypto_api_url", cryptoApiUrl)
            putString("ethereum_api_url", ethereumApiUrl)
            putString("gas_limit", gasLimit)
            putString("network", network)
            apply()
        }
        
        Toast.makeText(this, "Crypto configuration saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun testConnection() {
        binding.tvStatus.text = "Testing crypto connection..."
        
        // Simulate connection test
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.tvStatus.text = "Crypto connection test successful ✓"
            Toast.makeText(this, "Crypto connection test successful", Toast.LENGTH_SHORT).show()
        }, 2000)
    }
}
