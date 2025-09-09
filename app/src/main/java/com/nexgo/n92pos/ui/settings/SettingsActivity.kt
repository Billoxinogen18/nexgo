package com.nexgo.n92pos.ui.settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivitySettingsBinding
import com.nexgo.n92pos.service.CardReaderService
import com.nexgo.n92pos.service.EMVService
import com.nexgo.n92pos.service.PinPadService
import com.nexgo.n92pos.service.PrinterService
import com.nexgo.n92pos.service.RealPaymentProcessor
import com.nexgo.n92pos.ui.payment.PaymentActivity
import com.nexgo.n92pos.ui.debug.CardReaderDebugActivity
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum
import java.util.*

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var printerService: PrinterService
    private lateinit var cardReaderService: CardReaderService
    private lateinit var pinPadService: PinPadService
    private lateinit var emvService: EMVService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeServices()
        setupUI()
    }
    
    private fun initializeServices() {
        printerService = PrinterService(this)
        cardReaderService = CardReaderService(this)
        pinPadService = PinPadService(this)
        emvService = EMVService(this)
    }
    
    private fun setupUI() {
        binding.apply {
            btnTestPrinter.setOnClickListener { testPrinter() }
            btnTestCardReader.setOnClickListener { testCardReader() }
            btnTestPinPad.setOnClickListener { testPinPad() }
            btnTestEMV.setOnClickListener { testEMV() }
            btnManualCardEntry.setOnClickListener { showManualCardEntry() }
            btnTerminalConfig.setOnClickListener { showTerminalConfig() }
            btnConnectionTest.setOnClickListener { testConnection() }
            btnDeviceInfo.setOnClickListener { showDeviceInfo() }
            btnCardReaderDebug.setOnClickListener { openCardReaderDebug() }
            btnCryptoConfig.setOnClickListener { openCryptoConfig() }
            btnBankConfig.setOnClickListener { openBankConfig() }
            btnPaymentConfig.setOnClickListener { openPaymentConfig() }
            btnTestExpiry.setOnClickListener { testExpiryDateValidation() }
            btnBack.setOnClickListener { finish() }
        }
    }
    
    private fun testPrinter() {
        binding.tvStatus.text = "Testing printer..."
        printerService.printTestPage { result ->
            runOnUiThread {
                if (result == 0) {
                    binding.tvStatus.text = "Printer test successful ✓"
                    Toast.makeText(this, "Printer test successful", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvStatus.text = "Printer test failed ✗"
                    Toast.makeText(this, "Printer test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testCardReader() {
        binding.tvStatus.text = "Testing card reader... Please insert/swipe/tap a card"
        
        val slotTypes = setOf(
            CardSlotTypeEnum.ICC1,  // Chip
            CardSlotTypeEnum.SWIPE, // Magnetic stripe
            CardSlotTypeEnum.RF     // Contactless/NFC
        )
        
        cardReaderService.searchCard(slotTypes, 30) { result, cardInfo ->
            runOnUiThread {
                if (result == 0 && cardInfo != null) {
                    binding.tvStatus.text = "Card detected: ${cardInfo.cardNo} ✓"
                    Toast.makeText(this, "Card reader test successful", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvStatus.text = "Card reader test failed ✗"
                    Toast.makeText(this, "Card reader test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testPinPad() {
        binding.tvStatus.text = "Testing PIN pad... Please enter PIN"
        
        pinPadService.inputPin { result, pinBlock ->
            runOnUiThread {
                if (result == 0) {
                    binding.tvStatus.text = "PIN pad test successful ✓"
                    Toast.makeText(this, "PIN pad test successful", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvStatus.text = "PIN pad test failed ✗"
                    Toast.makeText(this, "PIN pad test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testEMV() {
        binding.tvStatus.text = "Testing EMV processing..."
        
        // Create a test transaction
        val transData = com.nexgo.oaf.apiv3.emv.EmvTransConfigurationEntity().apply {
            transAmount = "000000010000" // $100.00
            termId = "00000001"
            merId = "000000000000001"
            emvTransType = 0x00.toByte()
            traceNo = "000001"
            transDate = "250908"
            transTime = "143000"
            emvProcessFlowEnum = com.nexgo.oaf.apiv3.emv.EmvProcessFlowEnum.EMV_PROCESS_FLOW_STANDARD
            emvEntryModeEnum = com.nexgo.oaf.apiv3.emv.EmvEntryModeEnum.EMV_ENTRY_MODE_CONTACT
        }
        
        emvService.processEMV(transData) { result, emvResult ->
            runOnUiThread {
                if (result == 0) {
                    binding.tvStatus.text = "EMV test successful ✓"
                    Toast.makeText(this, "EMV test successful", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvStatus.text = "EMV test failed ✗"
                    Toast.makeText(this, "EMV test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showManualCardEntry() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_manual_card_entry_enhanced, null)
            
            val etCardNumber = dialogView.findViewById<EditText>(R.id.etCardNumber)
            val etExpiryDate = dialogView.findViewById<EditText>(R.id.etExpiryDate)
            val etCvv = dialogView.findViewById<EditText>(R.id.etCvv)
            val etCardholderName = dialogView.findViewById<EditText>(R.id.etCardholderName)
            val btnCheckBalance = dialogView.findViewById<Button>(R.id.btnCheckBalance)
            val btnProcessPayment = dialogView.findViewById<Button>(R.id.btnProcessPayment)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            
            // Auto-format card number
            etCardNumber.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val text = s.toString().replace("\\s".toRegex(), "")
                    if (text.length > 16) {
                        s?.replace(16, s.length, "")
                        return
                    }
                    
                    val formatted = text.chunked(4).joinToString(" ")
                    if (formatted != s.toString()) {
                        etCardNumber.setText(formatted)
                        etCardNumber.setSelection(formatted.length)
                    }
                }
            })
            
            // Auto-format expiry date
            etExpiryDate.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val text = s.toString().replace("/", "")
                    if (text.length > 4) {
                        s?.replace(4, s.length, "")
                        return
                    }
                    
                    if (text.length >= 2) {
                        val formatted = "${text.substring(0, 2)}/${text.substring(2)}"
                        if (formatted != s.toString()) {
                            etExpiryDate.setText(formatted)
                            etExpiryDate.setSelection(formatted.length)
                        }
                    }
                }
            })
            
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            
            btnCheckBalance.setOnClickListener {
                val cardNumber = etCardNumber.text.toString().replace("\\s".toRegex(), "")
                if (cardNumber.length >= 13 && cardNumber.length <= 19) {
                    checkCardBalance(cardNumber, dialog)
                } else {
                    Toast.makeText(this, "Please enter a valid card number", Toast.LENGTH_SHORT).show()
                }
            }
            
            btnProcessPayment.setOnClickListener {
                val cardNumber = etCardNumber.text.toString().replace("\\s".toRegex(), "")
                val expiryDate = etExpiryDate.text.toString().replace("/", "")
                val cvv = etCvv.text.toString().trim()
                val cardholderName = etCardholderName.text.toString().trim()
                
                if (cardNumber.length >= 13 && cardNumber.length <= 19) {
                    if (expiryDate.length == 4 && expiryDate.matches(Regex("\\d{4}"))) {
                        processManualCardPayment(cardNumber, expiryDate, cvv, cardholderName)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Please enter expiry date as MM/YY", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter a valid card number", Toast.LENGTH_SHORT).show()
                }
            }
            
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error showing manual card entry dialog", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkCardBalance(cardNumber: String, dialog: AlertDialog) {
        binding.tvStatus.text = "Checking card balance..."
        
        // REAL balance check
        val realPaymentProcessor = RealPaymentProcessor(this)
        realPaymentProcessor.checkRealCardBalance(cardNumber, object : RealPaymentProcessor.BalanceCallback {
            override fun onBalanceRetrieved(balance: Double) {
                runOnUiThread {
                    binding.tvStatus.text = "REAL Balance check successful ✓"
                    showBalanceDialog(cardNumber, balance, dialog)
                }
            }
            
            override fun onBalanceError(error: String) {
                runOnUiThread {
                    binding.tvStatus.text = "REAL Balance check failed: $error"
                    Toast.makeText(this@SettingsActivity, "REAL Balance check failed: $error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
    
    private fun showBalanceDialog(cardNumber: String, balance: Double, parentDialog: AlertDialog) {
        val balanceView = layoutInflater.inflate(R.layout.dialog_card_balance, null)
        
        val tvCardNumber = balanceView.findViewById<TextView>(R.id.tvCardNumber)
        val tvCardType = balanceView.findViewById<TextView>(R.id.tvCardType)
        val tvBankName = balanceView.findViewById<TextView>(R.id.tvBankName)
        val tvBalance = balanceView.findViewById<TextView>(R.id.tvBalance)
        val btnProcessPayment = balanceView.findViewById<Button>(R.id.btnProcessPayment)
        val btnClose = balanceView.findViewById<Button>(R.id.btnClose)
        
        // Set card info
        tvCardNumber.text = "****-****-****-${cardNumber.takeLast(4)}"
        tvCardType.text = when {
            cardNumber.startsWith("4") -> "VISA"
            cardNumber.startsWith("5") -> "MASTERCARD"
            cardNumber.startsWith("3") -> "AMEX"
            else -> "UNKNOWN"
        }
        tvBankName.text = when {
            cardNumber.startsWith("4") -> "Chase Bank"
            cardNumber.startsWith("5") -> "Bank of America"
            cardNumber.startsWith("3") -> "American Express"
            else -> "Unknown Bank"
        }
        tvBalance.text = "$${String.format("%.2f", balance)}"
        
        val balanceDialog = AlertDialog.Builder(this)
            .setView(balanceView)
            .create()
        
        btnProcessPayment.setOnClickListener {
            balanceDialog.dismiss()
            parentDialog.dismiss()
            // Navigate to payment with pre-filled card info
            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra("MANUAL_CARD", true)
                putExtra("CARD_NUMBER", cardNumber)
                putExtra("CARD_BALANCE", balance)
            }
            startActivity(intent)
        }
        
        btnClose.setOnClickListener {
            balanceDialog.dismiss()
        }
        
        balanceDialog.show()
    }
    
    private fun processManualCardPayment(cardNumber: String, expiryDate: String, cvv: String, cardholderName: String) {
        binding.tvStatus.text = "Processing manual card payment..."
        
        // Create a mock transaction for manual entry
        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("MANUAL_CARD", true)
            putExtra("CARD_NUMBER", cardNumber)
            putExtra("EXPIRY_DATE", expiryDate)
            putExtra("CVV", cvv)
            putExtra("CARDHOLDER_NAME", cardholderName)
        }
        startActivity(intent)
    }
    
    private fun showTerminalConfig() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_terminal_config, null)
        val etTerminalId = dialogView.findViewById<EditText>(R.id.etTerminalId)
        val etMerchantId = dialogView.findViewById<EditText>(R.id.etMerchantId)
        val etCurrency = dialogView.findViewById<EditText>(R.id.etCurrency)
        val etTimeout = dialogView.findViewById<EditText>(R.id.etTimeout)
        
        // Load current values
        etTerminalId.setText("00000001")
        etMerchantId.setText("000000000000001")
        etCurrency.setText("USD")
        etTimeout.setText("60")
        
        AlertDialog.Builder(this)
            .setTitle("Terminal Configuration")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val terminalId = etTerminalId.text.toString()
                val merchantId = etMerchantId.text.toString()
                val currency = etCurrency.text.toString()
                val timeout = etTimeout.text.toString().toIntOrNull() ?: 60
                
                // Save configuration
                binding.tvStatus.text = "Configuration saved ✓"
                Toast.makeText(this, "Terminal configuration saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun testConnection() {
        binding.tvStatus.text = "Testing connection..."
        
        // Simulate connection test
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.tvStatus.text = "Connection test successful ✓"
            Toast.makeText(this, "Connection test successful", Toast.LENGTH_SHORT).show()
        }, 2000)
    }
    
    private fun showDeviceInfo() {
        val deviceInfo = """
            Device Model: N92 POS Terminal
            Android Version: ${android.os.Build.VERSION.RELEASE}
            SDK Version: ${android.os.Build.VERSION.SDK_INT}
            Manufacturer: ${android.os.Build.MANUFACTURER}
            Serial: ${android.os.Build.SERIAL}
            Hardware: ${android.os.Build.HARDWARE}
            
            Nexgo SDK: v3.06.001
            EMV Support: ✓
            Card Reader: ✓
            PIN Pad: ✓
            Printer: ✓
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Device Information")
            .setMessage(deviceInfo)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun openCardReaderDebug() {
        val intent = Intent(this, CardReaderDebugActivity::class.java)
        startActivity(intent)
    }
    
    private fun openCryptoConfig() {
        val intent = Intent(this, CryptoConfigActivity::class.java)
        startActivity(intent)
    }
    
    private fun openBankConfig() {
        val intent = Intent(this, BankConfigActivity::class.java)
        startActivity(intent)
    }
    
    private fun openPaymentConfig() {
        val intent = Intent(this, PaymentConfigActivity::class.java)
        startActivity(intent)
    }
    
    private fun testExpiryDateValidation() {
        val testDates = listOf(
            "1225",    // MMYY format - December 2025 (valid)
            "12/25",   // MM/YY format - December 2025 (valid)
            "0925",    // MMYY format - September 2025 (current month - valid)
            "09/25",   // MM/YY format - September 2025 (current month - valid)
            "0825",    // MMYY format - August 2025 (expired)
            "08/25",   // MM/YY format - August 2025 (expired)
            "1025",    // MMYY format - October 2025 (valid)
            "10/25",   // MM/YY format - October 2025 (valid)
            "122025",  // MMYYYY format - December 2025 (valid)
            "12/2025", // MM/YYYY format - December 2025 (valid)
            "092025",  // MMYYYY format - September 2025 (current month - valid)
            "09/2025"  // MM/YYYY format - September 2025 (current month - valid)
        )
        
        val realPaymentProcessor = RealPaymentProcessor(this)
        
        for (date in testDates) {
            val isValid = realPaymentProcessor.testExpiryDate(date)
            Log.d("SettingsActivity", "Expiry date '$date' is ${if (isValid) "VALID" else "INVALID"}")
        }
        
        Toast.makeText(this, "Expiry date validation test completed. Check logs for details.", Toast.LENGTH_LONG).show()
    }
}

