package com.nexgo.n92pos.service

import android.content.Context
import android.util.Log
import com.nexgo.n92pos.N92POSApplication
import com.nexgo.oaf.apiv3.emv.*
import com.nexgo.oaf.apiv3.device.reader.CardSlotTypeEnum
import com.nexgo.oaf.apiv3.SdkResult
import com.nexgo.common.ByteUtils
import java.util.*

class EMVService(private val context: Context) {
    
    private val deviceEngine = N92POSApplication.deviceEngine
    private val emvHandler = deviceEngine.getEmvHandler2("n92pos")
    
    init {
        emvHandler.emvDebugLog(true)
        loadEMVConfiguration()
    }
    
    fun processEMV(
        transData: EmvTransConfigurationEntity,
        callback: (Int, EmvProcessResultEntity?) -> Unit
    ) {
        try {
            emvHandler.emvProcess(transData, object : com.nexgo.oaf.apiv3.emv.OnEmvProcessListener2 {
                override fun onTransInitBeforeGPO() {
                    Log.d("EMVService", "EMV transaction init before GPO")
                    emvHandler.onSetTransInitBeforeGPOResponse(true)
                }
                
                override fun onSelApp(
                    appList: MutableList<String>?,
                    candidateAppList: MutableList<com.nexgo.oaf.apiv3.emv.CandidateAppInfoEntity>?,
                    isFirstSelect: Boolean
                ) {
                    Log.d("EMVService", "EMV app selection")
                    candidateAppList?.forEach { app ->
                        Log.d("EMVService", "Candidate app: $app")
                    }
                }
                
                override fun onPrompt(prompt: com.nexgo.oaf.apiv3.emv.PromptEnum?) {
                    Log.d("EMVService", "EMV prompt: $prompt")
                    emvHandler.onSetPromptResponse(true)
                }
                
                override fun onConfirmCardNo(cardInfo: com.nexgo.oaf.apiv3.device.reader.CardInfoEntity?) {
                    Log.d("EMVService", "EMV confirm card number")
                    if (cardInfo != null) {
                        Log.d("EMVService", "Card number: ${cardInfo.cardNo}")
                        Log.d("EMVService", "Expiry date: ${cardInfo.expiredDate}")
                    }
                    emvHandler.onSetConfirmCardNoResponse(true)
                }
                
                override fun onCardHolderInputPin(isOnlinePin: Boolean, leftTimes: Int) {
                    Log.d("EMVService", "EMV PIN input required - Online: $isOnlinePin, Left times: $leftTimes")
                    // PIN input will be handled by PinPadService
                    emvHandler.onSetPinInputResponse(true, false)
                }
                
                override fun onOnlineProc() {
                    Log.d("EMVService", "EMV online processing")
                    
                    // Simulate online processing
                    val onlineResult = com.nexgo.oaf.apiv3.emv.EmvOnlineResultEntity().apply {
                        authCode = "123456"
                        rejCode = "00"
                        recvField55 = null // Host response data
                    }
                    
                    emvHandler.onSetOnlineProcResponse(com.nexgo.oaf.apiv3.SdkResult.Success, onlineResult)
                }
                
                override fun onRemoveCard() {
                    Log.d("EMVService", "EMV remove card")
                    emvHandler.onSetRemoveCardResponse()
                }
                
                override fun onFinish(retCode: Int, emvResult: com.nexgo.oaf.apiv3.emv.EmvProcessResultEntity?) {
                    Log.d("EMVService", "EMV process finished with code: $retCode")
                    
                    if (emvResult != null) {
                        Log.d("EMVService", "EMV result: $emvResult")
                    }
                    
                    callback(retCode, emvResult)
                    emvHandler.emvProcessCancel()
                }
                
                override fun onContactlessTapCardAgain() {
                    Log.d("EMVService", "Contactless tap card again")
                }
            })
        } catch (e: Exception) {
            Log.e("EMVService", "Error processing EMV", e)
            callback(-1, null)
        }
    }
    
    private fun loadEMVConfiguration() {
        try {
            // Load AID configuration
            loadAIDConfiguration()
            
            // Load CAPK configuration
            loadCAPKConfiguration()
            
            // Set terminal configuration
            setTerminalConfiguration()
            
        } catch (e: Exception) {
            Log.e("EMVService", "Error loading EMV configuration", e)
        }
    }
    
    private fun loadAIDConfiguration() {
        // Load AID data from assets
        try {
            val aidData = context.assets.open("emv_aid.json").bufferedReader().use { it.readText() }
            // Parse and set AID data
            Log.d("EMVService", "AID configuration loaded")
        } catch (e: Exception) {
            Log.e("EMVService", "Error loading AID configuration", e)
        }
    }
    
    private fun loadCAPKConfiguration() {
        // Load CAPK data from assets
        try {
            val capkData = context.assets.open("emv_capk.json").bufferedReader().use { it.readText() }
            // Parse and set CAPK data
            Log.d("EMVService", "CAPK configuration loaded")
        } catch (e: Exception) {
            Log.e("EMVService", "Error loading CAPK configuration", e)
        }
    }
    
    private fun setTerminalConfiguration() {
        try {
            // Set terminal capabilities
            val terminalCapabilities = byteArrayOf(0xE0.toByte(), 0xF8.toByte(), 0xC8.toByte())
            emvHandler.setTlv(byteArrayOf(0x9F.toByte(), 0x33.toByte()), terminalCapabilities)
            
            // Set terminal type
            val terminalType = byteArrayOf(0x22.toByte())
            emvHandler.setTlv(byteArrayOf(0x9F.toByte(), 0x35.toByte()), terminalType)
            
            // Set country code (US = 0840)
            val countryCode = byteArrayOf(0x08.toByte(), 0x40.toByte())
            emvHandler.setTlv(byteArrayOf(0x9F.toByte(), 0x1A.toByte()), countryCode)
            
            // Set currency code (USD = 0840)
            val currencyCode = byteArrayOf(0x08.toByte(), 0x40.toByte())
            emvHandler.setTlv(byteArrayOf(0x5F.toByte(), 0x2A.toByte()), currencyCode)
            
            Log.d("EMVService", "Terminal configuration set")
        } catch (e: Exception) {
            Log.e("EMVService", "Error setting terminal configuration", e)
        }
    }
}
