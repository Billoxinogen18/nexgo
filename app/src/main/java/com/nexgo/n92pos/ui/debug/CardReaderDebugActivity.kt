package com.nexgo.n92pos.ui.debug

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexgo.n92pos.R
import com.nexgo.n92pos.databinding.ActivityCardReaderDebugBinding
import com.nexgo.n92pos.service.CardReaderService
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum

class CardReaderDebugActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCardReaderDebugBinding
    private lateinit var cardReaderService: CardReaderService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardReaderDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cardReaderService = CardReaderService(this)
        setupUI()
    }
    
    private fun setupUI() {
        binding.apply {
            btnTestChip.setOnClickListener { testCardSlot(CardSlotTypeEnum.ICC1, "Chip") }
            btnTestSwipe.setOnClickListener { testCardSlot(CardSlotTypeEnum.SWIPE, "Swipe") }
            btnTestContactless.setOnClickListener { testCardSlot(CardSlotTypeEnum.RF, "Contactless") }
            btnTestAll.setOnClickListener { testAllSlots() }
            btnBack.setOnClickListener { finish() }
        }
    }
    
    private fun testCardSlot(slotType: CardSlotTypeEnum, slotName: String) {
        binding.tvDebugLog.text = "Testing $slotName card reader...\n"
        binding.tvDebugLog.append("Please insert/swipe/tap a card\n")
        
        cardReaderService.searchCard(setOf(slotType), 30) { result, cardInfo ->
            runOnUiThread {
                val logMessage = when (result) {
                    0 -> {
                        "✓ SUCCESS: $slotName card detected!\n"
                        "Card Number: ${cardInfo?.cardNo}\n"
                        "Card Slot: ${cardInfo?.cardExistslot}\n"
                    }
                    -1 -> "✗ FAILED: Swipe incorrect\n"
                    -2 -> "✗ FAILED: Multiple cards detected\n"
                    -3 -> "✗ FAILED: Timeout - No card detected\n"
                    -999 -> "✗ FAILED: Service error\n"
                    else -> "✗ FAILED: Error code $result\n"
                }
                
                binding.tvDebugLog.append(logMessage)
                binding.tvDebugLog.append("---\n")
                
                if (result == 0) {
                    Toast.makeText(this, "$slotName card detected successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "$slotName card test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun testAllSlots() {
        binding.tvDebugLog.text = "Testing all card readers...\n"
        binding.tvDebugLog.append("Please try different card types\n")
        
        val allSlots = setOf(
            CardSlotTypeEnum.ICC1,
            CardSlotTypeEnum.SWIPE,
            CardSlotTypeEnum.RF
        )
        
        cardReaderService.searchCard(allSlots, 30) { result, cardInfo ->
            runOnUiThread {
                val logMessage = when (result) {
                    0 -> {
                        "✓ SUCCESS: Card detected!\n"
                        "Card Number: ${cardInfo?.cardNo}\n"
                        "Card Slot: ${cardInfo?.cardExistslot}\n"
                        "Card Type: ${getCardTypeName(cardInfo?.cardExistslot)}\n"
                    }
                    -1 -> "✗ FAILED: Swipe incorrect\n"
                    -2 -> "✗ FAILED: Multiple cards detected\n"
                    -3 -> "✗ FAILED: Timeout - No card detected\n"
                    -999 -> "✗ FAILED: Service error\n"
                    else -> "✗ FAILED: Error code $result\n"
                }
                
                binding.tvDebugLog.append(logMessage)
                binding.tvDebugLog.append("---\n")
                
                if (result == 0) {
                    Toast.makeText(this, "Card detected successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Card test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun getCardTypeName(slotType: CardSlotTypeEnum?): String {
        return when (slotType) {
            CardSlotTypeEnum.ICC1 -> "Chip Card"
            CardSlotTypeEnum.SWIPE -> "Magnetic Stripe"
            CardSlotTypeEnum.RF -> "Contactless/NFC"
            else -> "Unknown"
        }
    }
}
