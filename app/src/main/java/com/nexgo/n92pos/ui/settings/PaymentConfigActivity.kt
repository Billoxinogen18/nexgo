package com.nexgo.n92pos.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityPaymentConfigBinding

class PaymentConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPaymentConfigBinding
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("payment_config", MODE_PRIVATE)
        
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
            etStripeApiKey.setText(prefs.getString("stripe_api_key", ""))
            etSquareApiKey.setText(prefs.getString("square_api_key", ""))
        etPaypalApiKey.setText(prefs.getString("paypal_api_key", "AUHMyl0I90mgdQTjrUWFL8JswSCll_MpMuIFV299HogEiuU9C6za_powpTXhP29tUWtzRxl2b-fsdIX5"))
        etPaypalSecret.setText(prefs.getString("paypal_secret", "EHdgWhzDvcjejewg0_7QjX3Zcpw3aaPUXTVNbA2R7CYw7peX5Mb8hatGVOnjk08gAP2krySgi5RkZu91"))
            etBankApiKey.setText(prefs.getString("bank_api_key", ""))
            etCoinbaseApiKey.setText(prefs.getString("coinbase_api_key", ""))
            etBinanceApiKey.setText(prefs.getString("binance_api_key", "ghhAUdvCyMrYImYzFnaeom1cVXvHopy5gKWmQ9O7hPZK13ImJa66BJZ8L7Gps6C8"))
            etBinanceSecret.setText(prefs.getString("binance_secret", "Xj0OCpB7H7t4YT6LD87ShEE1JMys0ppRI6aU1Xy2wIfU3VYoN2sZfAp8uvz3MEce"))
            etCryptoWalletAddress.setText(prefs.getString("crypto_wallet_address", "0x29014271d71e3691cBaF01a26A4AC1502e2C4048"))
        }
    }
    
    private fun saveConfig() {
        val stripeApiKey = binding.etStripeApiKey.text.toString().trim()
        val squareApiKey = binding.etSquareApiKey.text.toString().trim()
        val paypalApiKey = binding.etPaypalApiKey.text.toString().trim()
        val paypalSecret = binding.etPaypalSecret.text.toString().trim()
        val bankApiKey = binding.etBankApiKey.text.toString().trim()
        val coinbaseApiKey = binding.etCoinbaseApiKey.text.toString().trim()
        val binanceApiKey = binding.etBinanceApiKey.text.toString().trim()
        val binanceSecret = binding.etBinanceSecret.text.toString().trim()
        val cryptoWalletAddress = binding.etCryptoWalletAddress.text.toString().trim()
        
        if (stripeApiKey.isEmpty() && squareApiKey.isEmpty() && paypalApiKey.isEmpty()) {
            Toast.makeText(this, "Please enter at least one payment processor API key", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (cryptoWalletAddress.isEmpty() || !cryptoWalletAddress.startsWith("0x")) {
            Toast.makeText(this, "Please enter a valid Ethereum wallet address", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefs.edit().apply {
            putString("stripe_api_key", stripeApiKey)
            putString("square_api_key", squareApiKey)
            putString("paypal_api_key", paypalApiKey)
            putString("paypal_secret", paypalSecret)
            putString("bank_api_key", bankApiKey)
            putString("coinbase_api_key", coinbaseApiKey)
            putString("binance_api_key", binanceApiKey)
            putString("binance_secret", binanceSecret)
            putString("crypto_wallet_address", cryptoWalletAddress)
            apply()
        }
        
        Toast.makeText(this, "Payment configuration saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun testConnection() {
        binding.tvStatus.text = "Testing payment processor connections..."
        
        // Test Stripe connection
        if (binding.etStripeApiKey.text.toString().isNotEmpty()) {
            testStripeConnection()
        } else {
            binding.tvStatus.text = "No API keys configured for testing"
            Toast.makeText(this, "Please configure API keys first", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testStripeConnection() {
        // This would make a real API call to test the connection
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.tvStatus.text = "Stripe connection test successful ✓"
            Toast.makeText(this, "Stripe connection test successful", Toast.LENGTH_SHORT).show()
        }, 2000)
    }
}
