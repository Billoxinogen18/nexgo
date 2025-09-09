package com.nexgo.n92pos

import android.app.Application
import com.nexgo.oaf.apiv3.APIProxy
import com.nexgo.oaf.apiv3.DeviceEngine

class N92POSApplication : Application() {
    
    companion object {
        lateinit var deviceEngine: DeviceEngine
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Nexgo SDK
        deviceEngine = APIProxy.getDeviceEngine(this)
        
        // Initialize device
        initializeDevice()
    }
    
    private fun initializeDevice() {
        try {
            // Initialize printer
            val printer = deviceEngine.printer
            printer.initPrinter()
            
            // Initialize card reader
            val cardReader = deviceEngine.cardReader
            
            // Initialize PIN pad
            val pinPad = deviceEngine.pinPad
            
            // Initialize EMV handler
            val emvHandler = deviceEngine.getEmvHandler2("n92pos")
            emvHandler.emvDebugLog(true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
