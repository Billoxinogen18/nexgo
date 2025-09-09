package com.nexgo.n92pos.service

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.nexgo.n92pos.N92POSApplication
import com.nexgo.n92pos.model.Transaction
import com.nexgo.oaf.apiv3.device.printer.*
import java.text.SimpleDateFormat
import java.util.*

class PrinterService(private val context: Context) {
    
    private val deviceEngine = N92POSApplication.deviceEngine
    private val printer = deviceEngine.printer
    
    fun printReceipt(transaction: Transaction, callback: (Int) -> Unit) {
        try {
            printer.initPrinter()
            printer.setTypeface(Typeface.DEFAULT)
            printer.setLetterSpacing(5)
            
            // Print header
            printer.appendPrnStr("NEXGO N92 POS TERMINAL", 24, AlignEnum.CENTER, true)
            printer.appendPrnStr("================================", 24, AlignEnum.CENTER, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            
            // Print transaction details
            printer.appendPrnStr("Transaction ID: ${transaction.id}", 20, AlignEnum.LEFT, false)
            printer.appendPrnStr("Date: ${formatDate(transaction.timestamp)}", 20, AlignEnum.LEFT, false)
            printer.appendPrnStr("Time: ${formatTime(transaction.timestamp)}", 20, AlignEnum.LEFT, false)
            printer.appendPrnStr("Type: ${transaction.type}", 20, AlignEnum.LEFT, false)
            printer.appendPrnStr("Amount: $${transaction.amount}", 20, AlignEnum.LEFT, true)
            printer.appendPrnStr("Status: ${transaction.status}", 20, AlignEnum.LEFT, false)
            
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("================================", 24, AlignEnum.CENTER, false)
            
            // Print card details if available
            transaction.cardNumber?.let { cardNumber ->
                val maskedCardNumber = maskCardNumber(cardNumber)
                printer.appendPrnStr("Card: $maskedCardNumber", 20, AlignEnum.LEFT, false)
            }
            
            transaction.authCode?.let { authCode ->
                printer.appendPrnStr("Auth Code: $authCode", 20, AlignEnum.LEFT, false)
            }
            
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("Thank you for your business!", 20, AlignEnum.CENTER, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            
            // Start printing
            printer.startPrint(false, object : OnPrintListener {
                override fun onPrintResult(retCode: Int) {
                    Log.d("PrinterService", "Print result: $retCode")
                    callback(retCode)
                }
            })
        } catch (e: Exception) {
            Log.e("PrinterService", "Error printing receipt", e)
            callback(-1)
        }
    }
    
    fun printTestPage(callback: (Int) -> Unit) {
        try {
            printer.initPrinter()
            printer.setTypeface(Typeface.DEFAULT)
            printer.setLetterSpacing(5)
            
            printer.appendPrnStr("NEXGO N92 POS TERMINAL", 24, AlignEnum.CENTER, true)
            printer.appendPrnStr("TEST PAGE", 24, AlignEnum.CENTER, true)
            printer.appendPrnStr("================================", 24, AlignEnum.CENTER, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            
            printer.appendPrnStr("Device Model: N92", 20, AlignEnum.LEFT, false)
            printer.appendPrnStr("Date: ${formatDate(System.currentTimeMillis())}", 20, AlignEnum.LEFT, false)
            printer.appendPrnStr("Time: ${formatTime(System.currentTimeMillis())}", 20, AlignEnum.LEFT, false)
            
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("Printer test successful!", 20, AlignEnum.CENTER, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            
            printer.startPrint(false, object : OnPrintListener {
                override fun onPrintResult(retCode: Int) {
                    Log.d("PrinterService", "Test print result: $retCode")
                    callback(retCode)
                }
            })
        } catch (e: Exception) {
            Log.e("PrinterService", "Error printing test page", e)
            callback(-1)
        }
    }
    
    fun printQRCode(data: String, callback: (Int) -> Unit) {
        try {
            printer.initPrinter()
            printer.appendQRcode(data, 200, AlignEnum.CENTER)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            
            printer.startPrint(false, object : OnPrintListener {
                override fun onPrintResult(retCode: Int) {
                    Log.d("PrinterService", "QR code print result: $retCode")
                    callback(retCode)
                }
            })
        } catch (e: Exception) {
            Log.e("PrinterService", "Error printing QR code", e)
            callback(-1)
        }
    }
    
    fun printBarcode(data: String, callback: (Int) -> Unit) {
        try {
            printer.initPrinter()
            printer.appendBarcode(data, 50, 0, 2, BarcodeFormatEnum.CODE_128, AlignEnum.CENTER)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            printer.appendPrnStr("", 24, AlignEnum.LEFT, false)
            
            printer.startPrint(false, object : OnPrintListener {
                override fun onPrintResult(retCode: Int) {
                    Log.d("PrinterService", "Barcode print result: $retCode")
                    callback(retCode)
                }
            })
        } catch (e: Exception) {
            Log.e("PrinterService", "Error printing barcode", e)
            callback(-1)
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun maskCardNumber(cardNumber: String): String {
        return if (cardNumber.length >= 4) {
            "**** **** **** ${cardNumber.takeLast(4)}"
        } else {
            "****"
        }
    }
}
