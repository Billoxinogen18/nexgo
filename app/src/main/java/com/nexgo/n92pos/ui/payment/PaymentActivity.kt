package com.nexgo.n92pos.ui.payment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.nexgo.n92pos.utils.UIUtils
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.nexgo.common.ByteUtils
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityPaymentBinding
import com.nexgo.n92pos.model.Transaction
import com.nexgo.n92pos.model.TransactionStatus
import com.nexgo.n92pos.service.CardReaderService
import com.nexgo.n92pos.service.EMVService
import com.nexgo.n92pos.service.PinPadService
import com.nexgo.n92pos.service.PrinterService
import com.nexgo.n92pos.service.PaymentProcessingService
import com.nexgo.n92pos.service.CryptoService
import com.nexgo.n92pos.service.BankService
import com.nexgo.n92pos.service.RealPaymentProcessor
import com.nexgo.n92pos.viewmodel.PaymentViewModel
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var viewModel: PaymentViewModel
    private lateinit var cardReaderService: CardReaderService
    private lateinit var emvService: EMVService
    private lateinit var pinPadService: PinPadService
    private lateinit var printerService: PrinterService
    private lateinit var paymentProcessingService: PaymentProcessingService
    private lateinit var cryptoService: CryptoService
    private lateinit var bankService: BankService
    private lateinit var realPaymentProcessor: RealPaymentProcessor
    
    private var currentAmount: String = "0.00"
    private var currentTransaction: Transaction? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[PaymentViewModel::class.java]
        
        initializeServices()
        setupUI()
        observeViewModel()
        handleIntent()
    }
    
    private fun handleIntent() {
        val isManualCard = intent.getBooleanExtra("MANUAL_CARD", false)
        if (isManualCard) {
            val cardNumber = intent.getStringExtra("CARD_NUMBER") ?: ""
            val expiryDate = intent.getStringExtra("EXPIRY_DATE") ?: ""
            val cvv = intent.getStringExtra("CVV") ?: ""
            val cardholderName = intent.getStringExtra("CARDHOLDER_NAME") ?: ""
            
            processManualCard(cardNumber, expiryDate, cvv, cardholderName)
        }
    }
    
    private fun processManualCard(cardNumber: String, expiryDate: String, cvv: String, cardholderName: String) {
        binding.tvStatus.text = "Processing manual card entry..."
        
        // Create a mock card info entity for manual entry
        val mockCardInfo = object : com.nexgo.oaf.apiv3.device.reader.CardInfoEntity() {
            override fun getCardNo(): String = cardNumber
            override fun getExpiredDate(): String = expiryDate
            override fun getServiceCode(): String = "000"
            override fun getCardExistslot(): com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum = 
                com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.SWIPE
        }
        
        // Process the manual card
        processCardPayment(mockCardInfo)
    }
    
    private fun initializeServices() {
        cardReaderService = CardReaderService(this)
        emvService = EMVService(this)
        pinPadService = PinPadService(this)
        printerService = PrinterService(this)
        paymentProcessingService = PaymentProcessingService(this)
        cryptoService = CryptoService(this)
        bankService = BankService(this)
        realPaymentProcessor = RealPaymentProcessor(this)
    }
    
    private fun setupUI() {
        binding.apply {
            // Amount input field
            etAmount.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    etAmount.selectAll()
                }
            }
            
            etAmount.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val amount = s?.toString() ?: "0.00"
                    currentAmount = amount
                    tvAmount.text = "$$amount"
                    viewModel.setAmount(amount)
                }
            })
            
            // Payment method selection
            rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.rbCrypto -> {
                        // Crypto payment selected
                        UIUtils.showInfoSnackbar(binding.root, "Crypto payment selected")
                    }
                    R.id.rbBank -> {
                        // Bank payment selected
                        UIUtils.showInfoSnackbar(binding.root, "Bank transfer selected")
                    }
                }
            }
            
            // Action buttons
            btnProcessPayment.setOnClickListener { startCardPayment() }
            btnClearAmount.setOnClickListener { clearAmount() }
            btnCancel.setOnClickListener { finish() }
            btnBack.setOnClickListener { finish() }
        }
    }
    
    private fun observeViewModel() {
        viewModel.amount.observe(this) { amount ->
            currentAmount = amount
            binding.tvAmount.text = "$$amount"
        }
        
        viewModel.paymentStatus.observe(this) { status ->
            when (status) {
                "CARD_DETECTED" -> {
                    binding.tvStatus.text = "Card detected, processing..."
                }
                "PIN_REQUIRED" -> {
                    binding.tvStatus.text = "Please enter PIN"
                    showPinEntry()
                }
                "PROCESSING" -> {
                    binding.tvStatus.text = "Processing payment..."
                }
                "SUCCESS" -> {
                    binding.tvStatus.text = "Payment successful!"
                    printReceipt()
                }
                "FAILED" -> {
                    binding.tvStatus.text = "Payment failed!"
                    UIUtils.showErrorSnackbar(binding.root, "Payment failed")
                }
                "CANCELLED" -> {
                    binding.tvStatus.text = "Payment cancelled"
                }
            }
        }
    }
    
    private fun addAmount(digit: String) {
        val newAmount = if (currentAmount == "0.00") digit else currentAmount + digit
        if (isValidAmount(newAmount)) {
            currentAmount = newAmount
            viewModel.setAmount(newAmount)
        }
    }
    
    
    private fun backspaceAmount() {
        if (currentAmount.length > 1) {
            val newAmount = currentAmount.dropLast(1)
            currentAmount = newAmount
            viewModel.setAmount(newAmount)
        } else {
            currentAmount = "0.00"
            viewModel.setAmount("0.00")
        }
    }
    
    private fun isValidAmount(amount: String): Boolean {
        return try {
            val value = amount.toDouble()
            value > 0 && value <= 999999.99
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    private fun startCardPayment() {
        if (currentAmount == "0.00") {
            UIUtils.showWarningSnackbar(binding.root, "Please enter amount")
            return
        }
        
        currentTransaction = Transaction(
            id = generateTransactionId(),
            amount = currentAmount,
            type = "SALE",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.PENDING
        )
        
        viewModel.setPaymentStatus("CARD_DETECTED")
        startCardReading()
    }
    
    private fun startCardReading() {
        // Show visual feedback for card detection
        showCardDetectionUI()
        
        val slotTypes = setOf(
            CardSlotTypeEnum.ICC1,  // Chip card
            CardSlotTypeEnum.SWIPE, // Magnetic stripe
            CardSlotTypeEnum.RF     // Contactless/NFC
        )
        
        cardReaderService.searchCard(slotTypes, 60) { result, cardInfo ->
            runOnUiThread {
                if (result == 0 && cardInfo != null) {
                    val cardNumber = cardInfo.cardNo ?: "Unknown"
                    Log.d("PaymentActivity", "Card detected: $cardNumber")
                    Log.d("PaymentActivity", "Card slot type: ${cardInfo.cardExistslot}")
                    Log.d("PaymentActivity", "Card expiry: ${cardInfo.expiredDate}")
                    showCardDetectedUI(cardInfo)
                    processCardPayment(cardInfo)
                } else {
                    Log.e("PaymentActivity", "Card detection failed - result: $result, cardInfo: $cardInfo")
                    showCardDetectionFailedUI()
                    viewModel.setPaymentStatus("FAILED")
                    UIUtils.showErrorSnackbar(binding.root, "Card reading failed. Please try again.")
                }
            }
        }
    }
    
    private fun showCardDetectionUI() {
        binding.tvStatus.text = "🔍 Please insert, swipe, or tap your card..."
        binding.tvStatus.setTextColor(getColor(R.color.warning_color))
        
        // Show progress bar
        binding.progressCardDetection.visibility = View.VISIBLE
        binding.progressCardDetection.isIndeterminate = true
        
        // Add pulsing animation
        val pulse = android.animation.ObjectAnimator.ofFloat(binding.tvStatus, "alpha", 0.5f, 1.0f)
        pulse.duration = 1000
        pulse.repeatCount = android.animation.ObjectAnimator.INFINITE
        pulse.repeatMode = android.animation.ObjectAnimator.REVERSE
        pulse.start()
        
        // Store animation reference for cleanup
        binding.tvStatus.tag = pulse
    }
    
    private fun showCardDetectedUI(cardInfo: com.nexgo.oaf.apiv3.device.reader.CardInfoEntity?) {
        // Stop pulsing animation
        val animation = binding.tvStatus.tag as? android.animation.ObjectAnimator
        animation?.cancel()
        
        // Hide progress bar
        binding.progressCardDetection.visibility = View.GONE
        
        if (cardInfo != null) {
            val cardNumber = cardInfo.cardNo ?: "Unknown"
            val expiryDate = cardInfo.expiredDate ?: "Unknown"
            val cardholderName = "Cardholder" // Not available from card reader
            
            val maskedNumber = if (cardNumber.length >= 4) {
                "**** **** **** ${cardNumber.takeLast(4)}"
            } else {
                "**** **** **** $cardNumber"
            }
            
            // Show card type - debug the actual slot type
            val slotType = cardInfo.cardExistslot
            Log.d("PaymentActivity", "Card slot type: $slotType")
            Log.d("PaymentActivity", "Card slot type enum: ${slotType?.javaClass?.simpleName}")
            
            val cardType = when {
                slotType == com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.ICC1 -> "Chip Card"
                slotType == com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.SWIPE -> "Magnetic Stripe"
                slotType == com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.RF -> "Contactless/NFC"
                slotType == null -> {
                    Log.w("PaymentActivity", "Card slot type is null")
                    "Card (Slot: Unknown)"
                }
                else -> {
                    Log.w("PaymentActivity", "Unknown card slot type: $slotType (${slotType?.javaClass?.simpleName})")
                    Log.w("PaymentActivity", "Available enum values: ICC1=${com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.ICC1}, SWIPE=${com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.SWIPE}, RF=${com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.RF}")
                    "Card (Slot: $slotType)"
                }
            }
            
            // Determine card brand
            val cardBrand = when {
                cardNumber == "Unknown" || cardNumber == "0000000000000000" -> {
                    if (slotType == com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum.ICC1) {
                        "Chip Card (Number not readable - use manual entry)"
                    } else {
                        "Unknown (Card number not readable)"
                    }
                }
                cardNumber.startsWith("4") -> "Visa"
                cardNumber.startsWith("5") -> "Mastercard"
                cardNumber.startsWith("3") -> "American Express"
                cardNumber.startsWith("6") -> "Discover"
                else -> "Unknown (Starts with: ${cardNumber.take(1)})"
            }
            
            binding.tvStatus.text = "✓ Card detected: $maskedNumber"
            binding.tvStatus.setTextColor(getColor(R.color.success_color))
            
            // Show detailed card info
            val cardDetails = buildString {
                appendLine("🔍 CARD DETAILS")
                appendLine("═══════════════")
                appendLine("Brand: $cardBrand")
                appendLine("Type: $cardType")
                appendLine("Number: $maskedNumber")
                appendLine("Expiry: $expiryDate")
                appendLine("Holder: $cardholderName")
                appendLine("═══════════════")
                appendLine("Processing payment...")
            }
            
            // Log all card details for debugging
            Log.d("PaymentActivity", "=== CARD DETECTED ===")
            Log.d("PaymentActivity", "Card Number: $cardNumber")
            Log.d("PaymentActivity", "Expiry Date: $expiryDate")
            Log.d("PaymentActivity", "Cardholder: $cardholderName")
            Log.d("PaymentActivity", "Card Type: $cardType")
            Log.d("PaymentActivity", "Card Brand: $cardBrand")
            Log.d("PaymentActivity", "===================")
            
            UIUtils.showSuccessSnackbar(binding.root, "$cardBrand $cardType detected")
            
            // Process payment after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                processCardPayment(cardInfo)
            }, 3000)
        } else {
            binding.tvStatus.text = "✓ Card detected but no details available"
            binding.tvStatus.setTextColor(getColor(R.color.warning_color))
            UIUtils.showWarningSnackbar(binding.root, "Card detected but details unavailable")
        }
    }
    
    private fun showCardDetectionFailedUI() {
        // Stop pulsing animation
        val animation = binding.tvStatus.tag as? android.animation.ObjectAnimator
        animation?.cancel()
        
        // Hide progress bar
        binding.progressCardDetection.visibility = View.GONE
        
        binding.tvStatus.text = "✗ Card reading failed. Please try again."
        binding.tvStatus.setTextColor(getColor(R.color.error_color))
    }
    
    private fun showChipCardDetectedUI() {
        // Stop pulsing animation
        val animation = binding.tvStatus.tag as? android.animation.ObjectAnimator
        animation?.cancel()
        
        // Hide progress bar
        binding.progressCardDetection.visibility = View.GONE
        
        binding.tvStatus.text = "🔍 Chip card detected - manual entry required"
        binding.tvStatus.setTextColor(getColor(R.color.warning_color))
        
        UIUtils.showWarningSnackbar(binding.root, "Chip card detected but card number not readable. Please use manual entry or try swiping the card instead.")
    }
    
    private fun extractCardNumberFromTrack2(track2: String): String {
        // Track 2 format: PAN=EXPIRY... (e.g., "4111111111111111=2512...")
        return try {
            val parts = track2.split("=")
            if (parts.isNotEmpty()) {
                val cardNumber = parts[0]
                Log.d("PaymentActivity", "Extracted card number from track 2: $cardNumber")
                cardNumber
            } else {
                Log.e("PaymentActivity", "Could not parse track 2: $track2")
                "0000000000000000"
            }
        } catch (e: Exception) {
            Log.e("PaymentActivity", "Error parsing track 2: $track2", e)
            "0000000000000000"
        }
    }
    
    private fun extractCardNumberFromTrack1(track1: String): String {
        // Track 1 format: %B[PAN]^[NAME]^[EXPIRY]... (e.g., "%B4111111111111111^CARDHOLDER^2512...")
        return try {
            val startIndex = track1.indexOf('B') + 1
            val endIndex = track1.indexOf('^', startIndex)
            if (startIndex > 0 && endIndex > startIndex) {
                val cardNumber = track1.substring(startIndex, endIndex)
                Log.d("PaymentActivity", "Extracted card number from track 1: $cardNumber")
                cardNumber
            } else {
                Log.e("PaymentActivity", "Could not parse track 1: $track1")
                "0000000000000000"
            }
        } catch (e: Exception) {
            Log.e("PaymentActivity", "Error parsing track 1: $track1", e)
            "0000000000000000"
        }
    }
    
    
    private fun processCardPayment(cardInfo: com.nexgo.oaf.apiv3.device.reader.CardInfoEntity) {
        binding.tvStatus.text = "Processing payment..."
        binding.tvStatus.setTextColor(getColor(R.color.warning_color))
        
        // Try to get card number from different sources
        // For Nexgo SDK, card number is often in track data (tk1, tk2, tk3)
        val cardNumber = when {
            !cardInfo.cardNo.isNullOrBlank() -> {
                Log.d("PaymentActivity", "Card number from cardNo: ${cardInfo.cardNo}")
                cardInfo.cardNo
            }
            !cardInfo.tk2.isNullOrBlank() && cardInfo.isTk2Valid() -> {
                // Extract card number from track 2 data (format: PAN=EXPIRY...)
                val track2 = cardInfo.tk2
                Log.d("PaymentActivity", "Extracting from track 2: $track2")
                extractCardNumberFromTrack2(track2)
            }
            !cardInfo.tk1.isNullOrBlank() && cardInfo.isTk1Valid() -> {
                // Extract card number from track 1 data
                val track1 = cardInfo.tk1
                Log.d("PaymentActivity", "Extracting from track 1: $track1")
                extractCardNumberFromTrack1(track1)
            }
            else -> {
                Log.e("PaymentActivity", "No valid card data found in any track")
                Log.e("PaymentActivity", "cardNo: '${cardInfo.cardNo}'")
                Log.e("PaymentActivity", "tk1: '${cardInfo.tk1}' (valid: ${cardInfo.isTk1Valid()})")
                Log.e("PaymentActivity", "tk2: '${cardInfo.tk2}' (valid: ${cardInfo.isTk2Valid()})")
                Log.e("PaymentActivity", "tk3: '${cardInfo.tk3}' (valid: ${cardInfo.isTk3Valid()})")
                
                // No valid card data found - this is an error
                "0000000000000000" // Invalid card number to trigger error
            }
        }
        
        val expiryDate = cardInfo.expiredDate?.takeIf { it.isNotBlank() } ?: "1225" // Default expiry
        val cardholderName = "CARDHOLDER" // Name not available from card reader
        
        Log.d("PaymentActivity", "Card info from reader:")
        Log.d("PaymentActivity", "  - CardNo: '${cardInfo.cardNo}'")
        Log.d("PaymentActivity", "  - ExpiredDate: '${cardInfo.expiredDate}'")
        Log.d("PaymentActivity", "  - ServiceCode: '${cardInfo.serviceCode}'")
        Log.d("PaymentActivity", "  - CardExistslot: '${cardInfo.cardExistslot}'")
        Log.d("PaymentActivity", "  - Track1: '${cardInfo.tk1}' (valid: ${cardInfo.isTk1Valid()})")
        Log.d("PaymentActivity", "  - Track2: '${cardInfo.tk2}' (valid: ${cardInfo.isTk2Valid()})")
        Log.d("PaymentActivity", "  - Track3: '${cardInfo.tk3}' (valid: ${cardInfo.isTk3Valid()})")
        Log.d("PaymentActivity", "  - CSN: '${cardInfo.csn}'")
        Log.d("PaymentActivity", "  - isICC: ${cardInfo.isICC()}")
        Log.d("PaymentActivity", "  - Final cardNumber: '$cardNumber'")
        Log.d("PaymentActivity", "  - Final expiryDate: '$expiryDate'")
        
        // Check if we have a valid card number
        if (cardNumber == "0000000000000000" || cardNumber.length < 13) {
            Log.e("PaymentActivity", "Invalid card number detected: '$cardNumber'")
            Log.e("PaymentActivity", "Card slot type: ${cardInfo.cardExistslot}")
            
            // Card detected but no track data available - this is a problem
            Log.e("PaymentActivity", "Card detected but no track data available - this should not happen!")
            UIUtils.showErrorSnackbar(binding.root, "Card detected but no track data readable. Please use manual entry or check card reader configuration.")
            
            // Show manual entry option
            binding.btnManualEntry.visibility = View.VISIBLE
            binding.btnManualEntry.text = "Enter Card Manually"
            return
        }
        
        // If we're using test card number, show a warning
        if (cardNumber == "4111111111111111") {
            Log.w("PaymentActivity", "Using test card number - card reading not working properly")
            UIUtils.showWarningSnackbar(binding.root, "Using test card number - card reading needs to be fixed")
        }
        
        // Use REAL payment processor
        realPaymentProcessor.processPayment(
            currentAmount.toDouble(),
            cardNumber,
            expiryDate,
            cardholderName,
            object : RealPaymentProcessor.PaymentCallback {
                override fun onSuccess(transaction: RealPaymentProcessor.PaymentTransaction) {
                    runOnUiThread {
                        binding.tvStatus.text = "✓ REAL Payment successful!"
                        binding.tvStatus.setTextColor(getColor(R.color.success_color))
                        
                        val successMessage = buildString {
                            append("REAL Payment: $${transaction.amount}\n")
                            append("Auth Code: ${transaction.authCode}\n")
                            append("Transaction ID: ${transaction.transactionId}\n")
                            append("Processor: ${transaction.processor}\n")
                            if (transaction.balance != null) {
                                append("New Balance: $${String.format("%.2f", transaction.balance)}\n")
                            }
                            if (transaction.cryptoTxHash != null) {
                                append("Crypto TX: ${transaction.cryptoTxHash}\n")
                            }
                        }
                        
                        UIUtils.showSuccessSnackbar(binding.root, successMessage)
                        
                        // Print receipt
                        printRealReceipt(transaction)
                        
                        resetPaymentUI() // Don't clear amount after successful payment
                    }
                }
                
                override fun onFailure(error: String) {
                    runOnUiThread {
                        binding.tvStatus.text = "✗ REAL Payment failed: $error"
                        binding.tvStatus.setTextColor(getColor(R.color.error_color))
                        
                        UIUtils.showErrorSnackbar(binding.root, "REAL Payment failed: $error")
                        
                        resetPaymentUI() // Don't clear amount after failed payment
                    }
                }
                
                override fun onPinRequired(callback: RealPaymentProcessor.PinCallback) {
                    runOnUiThread {
                        showPinEntryDialog(callback)
                    }
                }
                
                override fun onBalanceRequired(callback: RealPaymentProcessor.BalanceCallback) {
                    // This shouldn't be called during payment processing
                    callback.onBalanceError("Balance check not required during payment")
                }
            }
        )
    }
    
    private fun showPinEntry() {
        pinPadService.inputPin { result, pinBlock ->
            if (result == 0) {
                // PIN entered successfully, continue with EMV processing
                viewModel.setPaymentStatus("PROCESSING")
            } else {
                viewModel.setPaymentStatus("CANCELLED")
            }
        }
    }
    
    private fun showPinEntryDialog(callback: RealPaymentProcessor.PinCallback) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_entry, null)
        
        val tvPin1 = dialogView.findViewById<TextView>(R.id.tvPin1)
        val tvPin2 = dialogView.findViewById<TextView>(R.id.tvPin2)
        val tvPin3 = dialogView.findViewById<TextView>(R.id.tvPin3)
        val tvPin4 = dialogView.findViewById<TextView>(R.id.tvPin4)
        
        val pinViews = listOf(tvPin1, tvPin2, tvPin3, tvPin4)
        var pinDigits = mutableListOf<String>()
        
        // Set up keypad buttons
        val keypadButtons = listOf(
            R.id.btnPin1, R.id.btnPin2, R.id.btnPin3, R.id.btnPin4, R.id.btnPin5,
            R.id.btnPin6, R.id.btnPin7, R.id.btnPin8, R.id.btnPin9, R.id.btnPin0
        )
        
        keypadButtons.forEach { buttonId ->
            dialogView.findViewById<Button>(buttonId).setOnClickListener { button ->
                val digit = (button as Button).text.toString()
                if (pinDigits.size < 4) {
                    pinDigits.add(digit)
                    pinViews[pinDigits.size - 1].text = "●"
                    pinViews[pinDigits.size - 1].setTextColor(getColor(R.color.primary_color))
                }
            }
        }
        
        dialogView.findViewById<Button>(R.id.btnPinBackspace).setOnClickListener {
            if (pinDigits.isNotEmpty()) {
                pinDigits.removeAt(pinDigits.size - 1)
                pinViews[pinDigits.size].text = "•"
                pinViews[pinDigits.size].setTextColor(getColor(R.color.text_secondary))
            }
        }
        
        dialogView.findViewById<Button>(R.id.btnPinClear).setOnClickListener {
            pinDigits.clear()
            pinViews.forEach { view -> view.text = "•" }
            pinViews.forEach { view -> view.setTextColor(getColor(R.color.text_secondary)) }
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.btnPinConfirm).setOnClickListener {
            if (pinDigits.size == 4) {
                val pin = pinDigits.joinToString("")
                callback.onPinEntered(pin)
                dialog.dismiss()
            } else {
                UIUtils.showWarningSnackbar(binding.root, "Please enter 4-digit PIN")
            }
        }
        
        dialogView.findViewById<Button>(R.id.btnPinCancel).setOnClickListener {
            callback.onPinCancelled()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun clearAmount() {
        currentAmount = "0.00"
        binding.etAmount.setText("0.00")
        binding.tvAmount.text = "$0.00"
        viewModel.setAmount("0.00")
    }
    
    private fun resetPaymentUI(clearAmount: Boolean = false) {
        binding.tvStatus.text = "Ready"
        binding.tvStatus.setTextColor(getColor(R.color.text_primary))
        binding.progressCardDetection.visibility = View.GONE
        
        if (clearAmount) {
            clearAmount()
        }
    }
    
    private fun printRealReceipt(transaction: RealPaymentProcessor.PaymentTransaction) {
        val receiptData = """
            ================================
                    RECEIPT
            ================================
            
            Transaction ID: ${transaction.transactionId}
            Amount: $${String.format("%.2f", transaction.amount)}
            Card: ${transaction.cardNumber}
            Type: ${transaction.cardType}
            Auth Code: ${transaction.authCode}
            Status: ${transaction.status}
            Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))}
            
            ${if (transaction.balance != null) "New Balance: $${String.format("%.2f", transaction.balance)}" else ""}
            
            ================================
            Thank you for your business!
            ================================
        """.trimIndent()
        
        // Create a mock transaction for printing
        val mockTransaction = Transaction(
            id = transaction.transactionId,
            amount = transaction.amount.toString(),
            cardNumber = transaction.cardNumber,
            timestamp = transaction.timestamp,
            status = TransactionStatus.SUCCESS,
            type = "CARD_PAYMENT"
        )
        printerService.printReceipt(mockTransaction) { result ->
            if (result == 0) {
                UIUtils.showSuccessSnackbar(binding.root, "Receipt printed successfully")
            } else {
                UIUtils.showErrorSnackbar(binding.root, "Receipt printing failed")
            }
        }
    }
    
    private fun printReceipt() {
        currentTransaction?.let { transaction ->
            printerService.printReceipt(transaction) { result ->
                if (result == 0) {
                    UIUtils.showSuccessSnackbar(binding.root, "Receipt printed successfully")
                } else {
                    UIUtils.showErrorSnackbar(binding.root, "Receipt printing failed")
                }
            }
        }
    }
    
    private fun startCashPayment() {
        // TODO: Implement cash payment
        UIUtils.showWarningSnackbar(binding.root, "Cash payment not implemented")
    }
    
    private fun startRefund() {
        // TODO: Implement refund
        UIUtils.showWarningSnackbar(binding.root, "Refund not implemented")
    }
    
    private fun startVoid() {
        // TODO: Implement void
        UIUtils.showWarningSnackbar(binding.root, "Void not implemented")
    }
    
    private fun generateTransactionId(): String {
        return "TXN${System.currentTimeMillis()}"
    }
    
    private fun generateTraceNumber(): String {
        return String.format("%06d", Random().nextInt(999999))
    }
    
    private fun leftPad(str: String, size: Int, padChar: Char): String {
        return str.padStart(size, padChar)
    }
    
    private fun printReceipt(transaction: Transaction, authCode: String, cryptoTxHash: String?) {
        printerService.printReceipt(transaction) { result ->
            runOnUiThread {
                if (result == 0) {
                    UIUtils.showSuccessSnackbar(binding.root, "Receipt printed successfully")
                } else {
                    UIUtils.showErrorSnackbar(binding.root, "Receipt printing failed")
                }
            }
        }
    }
}
