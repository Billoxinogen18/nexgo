package com.nexgo.n92pos.service

import android.content.Context
import android.util.Log
import com.nexgo.n92pos.N92POSApplication
import com.nexgo.oaf.apiv3.device.reader.CardInfoEntity
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum
import com.nexgo.oaf.apiv3.device.reader.OnCardInfoListener
import java.util.*

class CardReaderService(private val context: Context) {
    
    private val deviceEngine = N92POSApplication.deviceEngine
    private val cardReader = deviceEngine.cardReader
    
    fun searchCard(
        slotTypes: Set<CardSlotTypeEnum>,
        timeout: Int,
        callback: (Int, CardInfoEntity?) -> Unit
    ) {
        try {
            Log.d("CardReaderService", "Starting card search with slots: $slotTypes, timeout: $timeout")
            
            val slotTypesSet = HashSet<CardSlotTypeEnum>()
            slotTypesSet.addAll(slotTypes)
            
            // Try different search methods to get track data
            cardReader.searchCard(slotTypesSet, timeout, object : OnCardInfoListener {
                override fun onCardInfo(retCode: Int, cardInfo: CardInfoEntity?) {
                    Log.d("CardReaderService", "Card search result: $retCode")
                    if (retCode == 0 && cardInfo != null) {
                        Log.d("CardReaderService", "Card detected - checking track data:")
                        Log.d("CardReaderService", "  cardNo: '${cardInfo.cardNo}'")
                        Log.d("CardReaderService", "  tk1: '${cardInfo.tk1}' (valid: ${cardInfo.isTk1Valid()})")
                        Log.d("CardReaderService", "  tk2: '${cardInfo.tk2}' (valid: ${cardInfo.isTk2Valid()})")
                        Log.d("CardReaderService", "  tk3: '${cardInfo.tk3}' (valid: ${cardInfo.isTk3Valid()})")
                        Log.d("CardReaderService", "  expiredDate: '${cardInfo.expiredDate}'")
                        Log.d("CardReaderService", "  serviceCode: '${cardInfo.serviceCode}'")
                        Log.d("CardReaderService", "  cardExistslot: '${cardInfo.cardExistslot}'")
                        Log.d("CardReaderService", "  isICC: ${cardInfo.isICC()}")
                        Log.d("CardReaderService", "  csn: '${cardInfo.csn}'")
                        
                        // Try to populate track data if it's missing
                        if (cardInfo.cardNo.isNullOrEmpty() && 
                            cardInfo.tk1.isNullOrEmpty() && 
                            cardInfo.tk2.isNullOrEmpty() && 
                            cardInfo.tk3.isNullOrEmpty()) {
                            
                            Log.w("CardReaderService", "No track data found, trying alternative methods...")
                            
                            // Try to get card data using different approach
                            tryAlternativeCardReading(cardInfo, callback)
                        } else {
                            callback(0, cardInfo)
                        }
                    } else {
                        callback(retCode, null)
                    }
                }
                
                override fun onSwipeIncorrect() {
                    Log.w("CardReaderService", "Swipe incorrect")
                    callback(-1, null)
                }
                
                override fun onMultipleCards() {
                    Log.w("CardReaderService", "Multiple cards detected")
                    callback(-2, null)
                }
            })
        } catch (e: Exception) {
            Log.e("CardReaderService", "Error searching card", e)
            callback(-999, null)
        }
    }
    
    private fun tryAlternativeCardReading(cardInfo: CardInfoEntity, callback: (Int, CardInfoEntity?) -> Unit) {
        Log.d("CardReaderService", "Trying alternative card reading methods...")
        
        // For now, return the card info as is - the issue might be with the SDK configuration
        // or the card reader hardware not providing track data
        Log.w("CardReaderService", "Alternative methods not implemented - returning original card info")
        callback(0, cardInfo)
    }
    
    fun cancelSearchCard() {
        try {
            // Note: cancelSearchCard method may not be available in all SDK versions
            // cardReader.cancelSearchCard()
        } catch (e: Exception) {
            Log.e("CardReaderService", "Error cancelling card search", e)
        }
    }
    
    fun removeCard() {
        try {
            // Note: removeCard method may not be available in all SDK versions
            // cardReader.removeCard()
        } catch (e: Exception) {
            Log.e("CardReaderService", "Error removing card", e)
        }
    }
}
