package com.nexgo.n92pos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nexgo.n92pos.model.Transaction

class MainViewModel : ViewModel() {
    
    private val _deviceStatus = MutableLiveData<String>()
    val deviceStatus: LiveData<String> = _deviceStatus
    
    private val _lastTransaction = MutableLiveData<Transaction?>()
    val lastTransaction: LiveData<Transaction?> = _lastTransaction
    
    init {
        _deviceStatus.value = "Initializing..."
        checkDeviceStatus()
    }
    
    private fun checkDeviceStatus() {
        // Simulate device status check
        _deviceStatus.value = "Ready"
    }
    
    fun updateLastTransaction(transaction: Transaction) {
        _lastTransaction.value = transaction
    }
}
