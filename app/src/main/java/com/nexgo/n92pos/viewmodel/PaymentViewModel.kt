package com.nexgo.n92pos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nexgo.n92pos.model.Transaction

class PaymentViewModel : ViewModel() {
    
    private val _amount = MutableLiveData<String>()
    val amount: LiveData<String> = _amount
    
    private val _paymentStatus = MutableLiveData<String>()
    val paymentStatus: LiveData<String> = _paymentStatus
    
    private val _currentTransaction = MutableLiveData<Transaction?>()
    val currentTransaction: LiveData<Transaction?> = _currentTransaction
    
    init {
        _amount.value = "0.00"
        _paymentStatus.value = "READY"
    }
    
    fun setAmount(amount: String) {
        _amount.value = amount
    }
    
    fun setPaymentStatus(status: String) {
        _paymentStatus.value = status
    }
    
    fun setCurrentTransaction(transaction: Transaction) {
        _currentTransaction.value = transaction
    }
    
    fun clearTransaction() {
        _currentTransaction.value = null
        _paymentStatus.value = "READY"
    }
}
