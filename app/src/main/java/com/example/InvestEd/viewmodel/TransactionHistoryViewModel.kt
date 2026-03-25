// viewmodel/TransactionHistoryViewModel.kt
package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.Transaction
import com.example.InvestEd.repository.TransactionRepository
import kotlinx.coroutines.launch

class TransactionHistoryViewModel(
    private val repository: TransactionRepository = TransactionRepository()
) : ViewModel() {

    private val _state = MutableLiveData<TransactionState>()
    val state: LiveData<TransactionState> = _state

    fun loadTransactions() {
        _state.value = TransactionState.Loading
        viewModelScope.launch {
            val result = repository.getTransactionHistory()
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    _state.value = TransactionState.Empty
                } else {
                    _state.value = TransactionState.Success(list)
                }
            }.onFailure {
                _state.value = TransactionState.Error(it.message ?: "Unknown error")
            }
        }
    }

    sealed class TransactionState {
        object Loading : TransactionState()
        data class Success(val transactions: List<Transaction>) : TransactionState()
        object Empty : TransactionState()
        data class Error(val message: String) : TransactionState()
    }
}