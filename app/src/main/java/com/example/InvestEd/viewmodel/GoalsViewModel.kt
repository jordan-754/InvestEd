package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.Goal
import com.example.InvestEd.repository.BudgetRepository
import com.example.InvestEd.repository.GoalRepository
import com.example.InvestEd.repository.MainRepository
import kotlinx.coroutines.launch

class GoalsViewModel(
    private val goalRepository: GoalRepository = GoalRepository(),
    private val mainRepository: MainRepository = MainRepository()
) : ViewModel() {

    sealed class GoalsState {
        object Loading : GoalsState()
        data class Success(val goals: List<Goal>) : GoalsState()
        object Empty : GoalsState()
        data class Error(val message: String) : GoalsState()
    }

    sealed class ActionState {
        object Idle : ActionState()
        object Loading : ActionState()
        object Success : ActionState()
        data class Error(val message: String) : ActionState()
    }

    private val _goalsState    = MutableLiveData<GoalsState>(GoalsState.Loading)
    val goalsState: LiveData<GoalsState> = _goalsState

    private val _actionState   = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    private val _currentBalance = MutableLiveData(0.0)
    val currentBalance: LiveData<Double> = _currentBalance

    init {
        loadGoals()
        loadCurrentBalance()
        applyMonthlyInterestIfDue()
    }

    private fun applyMonthlyInterestIfDue() {
        viewModelScope.launch {
            goalRepository.applyMonthlyInterest()
        }
    }

    fun testMonthlyInterest() {
        viewModelScope.launch {
            val result = goalRepository.applyMonthlyInterestTest()
            if (result.isSuccess) {
                val interest = result.getOrThrow()
                if (interest > 0) {
                    _actionState.value = ActionState.Success
                    loadCurrentBalance()
                } else {
                    _actionState.value = ActionState.Error("No active goals with savings to earn interest.")
                }
            } else {
                _actionState.value = ActionState.Error("Failed to apply interest.")
            }
        }
    }

    fun loadCurrentBalance() {
        viewModelScope.launch {
            try {
                val balance = BudgetRepository().getTotalAmount()
                _currentBalance.value = balance
            } catch (e: Exception) {
                _currentBalance.value = 0.0
            }
        }
    }

    fun loadGoals() {
        viewModelScope.launch {
            _goalsState.value = GoalsState.Loading
            val result = goalRepository.getGoals()
            if (result.isSuccess) {
                val goals = result.getOrThrow()
                _goalsState.value = if (goals.isEmpty()) GoalsState.Empty
                else GoalsState.Success(goals)
            } else {
                _goalsState.value = GoalsState.Error("Failed to load goals")
            }
        }
    }

    fun addGoal(title: String, targetAmount: String, deadline: String) {
        val amount = targetAmount.toDoubleOrNull()
        if (title.isBlank())             { _actionState.value = ActionState.Error("Please enter a goal title"); return }
        if (amount == null || amount <= 0) { _actionState.value = ActionState.Error("Please enter a valid amount"); return }
        if (deadline.isBlank())          { _actionState.value = ActionState.Error("Please set a deadline"); return }

        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            val result = goalRepository.addGoal(title, amount, deadline)
            _actionState.value = if (result.isSuccess) ActionState.Success
            else ActionState.Error("Failed to add goal. Check your connection.")
            if (result.isSuccess) loadGoals()
        }
    }

    fun updateCurrentAmount(goalId: String, newAmount: Double, goalTitle: String, addedAmount: Double) {
        viewModelScope.launch {
            val result = goalRepository.updateGoalCurrentAmount(goalId, newAmount, goalTitle, addedAmount)
            if (result.isSuccess) {
                loadCurrentBalance()
                loadGoals()
            }
        }
    }

    fun completeGoal(goalId: String, goalTitle: String) {
        viewModelScope.launch {
            val result = goalRepository.completeGoal(goalId, goalTitle)
            if (result.isSuccess) {
                loadGoals()
                loadCurrentBalance()
            }
        }
    }

    fun deleteGoal(goalId: String, goalTitle: String) {
        viewModelScope.launch {
            val result = goalRepository.deleteGoal(goalId, goalTitle)
            if (result.isSuccess) {
                loadGoals()
                loadCurrentBalance()
            }
        }
    }

    fun submitGoalWithdrawal(
        goalId: String,
        goalTitle: String,
        amount: Double,
        walletType: String,
        walletNumber: String,
        fullName: String
    ) {
        viewModelScope.launch {
            val result = goalRepository.submitGoalWithdrawal(
                goalId, goalTitle, amount, walletType, walletNumber, fullName
            )
            if (result.isSuccess) {
                _actionState.value = ActionState.Success
            } else {
                _actionState.value = ActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to submit withdrawal"
                )
            }
        }
    }

    fun resetActionState() { _actionState.value = ActionState.Idle }
}