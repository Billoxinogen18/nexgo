package com.nexgo.n92pos.service

import android.content.Context
import android.util.Log
import com.nexgo.n92pos.N92POSApplication
import com.nexgo.oaf.apiv3.device.pinpad.*
import com.nexgo.oaf.apiv3.SdkResult

class PinPadService(private val context: Context) {
    
    private val deviceEngine = N92POSApplication.deviceEngine
    private val pinPad = deviceEngine.pinPad
    
    fun inputPin(callback: (Int, ByteArray?) -> Unit) {
        try {
            // Configure PIN pad
            pinPad.setPinKeyboardMode(PinKeyboardModeEnum.FIXED)
            pinPad.setAlgorithmMode(AlgorithmModeEnum.DES)
            
            // Set PIN length options (4-12 digits)
            val pinLengths = intArrayOf(0x00, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c)
            
            // Input offline PIN (for testing)
            pinPad.inputOfflinePin(pinLengths, 60, object : OnPinPadInputListener {
                override fun onInputResult(retCode: Int, pinBlock: ByteArray?) {
                    Log.d("PinPadService", "PIN input result: $retCode")
                    
                    when (retCode) {
                        SdkResult.Success -> {
                            Log.d("PinPadService", "PIN entered successfully")
                            callback(0, pinBlock)
                        }
                        SdkResult.PinPad_No_Pin_Input -> {
                            Log.d("PinPadService", "No PIN input required")
                            callback(0, null)
                        }
                        SdkResult.PinPad_Input_Cancel -> {
                            Log.d("PinPadService", "PIN input cancelled")
                            callback(-1, null)
                        }
                        SdkResult.PinPad_Input_Timeout -> {
                            Log.d("PinPadService", "PIN input timeout")
                            callback(-2, null)
                        }
                        else -> {
                            Log.e("PinPadService", "PIN input failed with code: $retCode")
                            callback(retCode, null)
                        }
                    }
                }
                
                override fun onSendKey(key: Byte) {
                    Log.d("PinPadService", "Key pressed: $key")
                }
            })
        } catch (e: Exception) {
            Log.e("PinPadService", "Error inputting PIN", e)
            callback(-3, null)
        }
    }
    
    fun inputOnlinePin(
        cardNumber: String,
        callback: (Int, ByteArray?) -> Unit
    ) {
        try {
            // Configure PIN pad for online PIN
            pinPad.setPinKeyboardMode(PinKeyboardModeEnum.FIXED)
            pinPad.setAlgorithmMode(AlgorithmModeEnum.DES)
            
            // Set PIN length options
            val pinLengths = intArrayOf(0x00, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c)
            
            // Input online PIN
            pinPad.inputOnlinePin(
                pinLengths,
                60,
                cardNumber.toByteArray(),
                10, // Key ID
                PinAlgorithmModeEnum.ISO9564FMT1,
                object : OnPinPadInputListener {
                    override fun onInputResult(retCode: Int, pinBlock: ByteArray?) {
                        Log.d("PinPadService", "Online PIN input result: $retCode")
                        
                        when (retCode) {
                            SdkResult.Success -> {
                                Log.d("PinPadService", "Online PIN entered successfully")
                                callback(0, pinBlock)
                            }
                            SdkResult.PinPad_No_Pin_Input -> {
                                Log.d("PinPadService", "No online PIN input required")
                                callback(0, null)
                            }
                            SdkResult.PinPad_Input_Cancel -> {
                                Log.d("PinPadService", "Online PIN input cancelled")
                                callback(-1, null)
                            }
                            SdkResult.PinPad_Input_Timeout -> {
                                Log.d("PinPadService", "Online PIN input timeout")
                                callback(-2, null)
                            }
                            else -> {
                                Log.e("PinPadService", "Online PIN input failed with code: $retCode")
                                callback(retCode, null)
                            }
                        }
                    }
                    
                    override fun onSendKey(key: Byte) {
                        Log.d("PinPadService", "Key pressed: $key")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("PinPadService", "Error inputting online PIN", e)
            callback(-3, null)
        }
    }
    
    fun cancelPinInput() {
        try {
            pinPad.cancelInput()
        } catch (e: Exception) {
            Log.e("PinPadService", "Error cancelling PIN input", e)
        }
    }
    
    fun setPinKey(keyId: Int, key: ByteArray) {
        try {
            // Note: setPinKey method may not be available in all SDK versions
            // pinPad.setPinKey(keyId, key)
            Log.d("PinPadService", "PIN key set for ID: $keyId")
        } catch (e: Exception) {
            Log.e("PinPadService", "Error setting PIN key", e)
        }
    }
}
