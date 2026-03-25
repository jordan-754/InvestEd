// viewmodel/MicroInvestmentViewModel.kt
package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.Investment
import com.example.InvestEd.repository.InvestmentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MicroInvestmentViewModel(
    private val repository: InvestmentRepository = InvestmentRepository()
) : ViewModel() {

    sealed class InvestState {
        object Idle : InvestState()
        object Loading : InvestState()
        object PendingSubmitted : InvestState()
        object WithdrawalSubmitted : InvestState()
        data class Success(val investment: Investment) : InvestState()
        data class Error(val message: String) : InvestState()
    }

    private val _totalInvested = MutableLiveData(0.0)
    val totalInvested: LiveData<Double> = _totalInvested

    private val _investState = MutableLiveData<InvestState>(InvestState.Idle)
    val investState: LiveData<InvestState> = _investState

    private val _latestCashIn = MutableLiveData<InvestmentRepository.LatestCashIn?>(null)
    val latestCashIn: LiveData<InvestmentRepository.LatestCashIn?> = _latestCashIn

    private val _currentValue = MutableLiveData(0.0)
    val currentValue: LiveData<Double> = _currentValue

    private val _totalInterest = MutableLiveData(0.0)
    val totalInterest: LiveData<Double> = _totalInterest

    init {
        loadLatestCashIn()
        loadCurrentValue()
        applyInterestIfDue()
        cleanupOrphanBudgetEntries() // ✅ one-time fix
    }

    private fun cleanupOrphanBudgetEntries() {
        viewModelScope.launch {
            repository.cleanupBudgetEntriesWithoutSourceId()
        }
    }

    // Old direct invest — kept for compatibility
    fun invest(amount: Double) {
        if (amount < 20) { _investState.value = InvestState.Error("Minimum investment is ₱20"); return }
        if (amount > 10000) { _investState.value = InvestState.Error("Maximum investment is ₱10,000"); return }

        _investState.value = InvestState.Loading
        viewModelScope.launch {
            val result = repository.invest(amount)
            if (result.isSuccess) {
                _investState.value = InvestState.Success(result.getOrThrow())
                loadLatestCashIn()
            } else {
                _investState.value = InvestState.Error(
                    result.exceptionOrNull()?.message ?: "Investment failed"
                )
            }
        }
    }
    // New — submits a PENDING transaction for admin approval
    fun submitPendingInvestment(amount: Double) {
        if (amount < 20) { _investState.value = InvestState.Error("Minimum investment is ₱20"); return }
        if (amount > 10000) { _investState.value = InvestState.Error("Maximum investment is ₱10,000"); return }

        _investState.value = InvestState.Loading
        viewModelScope.launch {
            val result = repository.submitPendingInvestment(amount)
            if (result.isSuccess) {
                _investState.value = InvestState.PendingSubmitted
                loadLatestCashIn()
            } else {
                _investState.value = InvestState.Error(
                    result.exceptionOrNull()?.message ?: "Submission failed"
                )
            }
        }
    }

    fun loadLatestCashIn() {
        viewModelScope.launch {
            _latestCashIn.value = repository.getLatestCashIn()
        }
    }

    fun resetState() { _investState.value = InvestState.Idle }

    fun loadCurrentValue() {
        viewModelScope.launch {
            _currentValue.value = repository.getTotalCurrentValue()
            _totalInterest.value = repository.getTotalInterestEarned()
            _totalInvested.value = repository.getTotalInvested()
        }
    }

    fun applyInterestIfDue() {
        viewModelScope.launch {
            repository.applyDailyInterest()
            loadCurrentValue()
        }
    }
    fun submitWithdrawal(
        amount: Double,
        source: String,
        walletType: String   = "",
        walletNumber: String = "",
        fullName: String     = ""
    ) {
        _investState.value = InvestState.Loading
        viewModelScope.launch {
            val result = repository.submitWithdrawal(
                amount, source, walletType, walletNumber, fullName
            )
            if (result.isSuccess) {
                _investState.value = InvestState.WithdrawalSubmitted
            } else {
                _investState.value = InvestState.Error(
                    result.exceptionOrNull()?.message ?: "Withdrawal failed"
                )
            }
        }
    }
}