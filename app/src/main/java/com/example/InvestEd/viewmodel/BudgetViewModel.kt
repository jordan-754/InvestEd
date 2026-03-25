package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.BudgetEntry
import com.example.InvestEd.repository.BudgetRepository
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val repository: BudgetRepository = BudgetRepository()
) : ViewModel() {

    enum class FilterType { ALL, SAVINGS, SPENDING }

    data class BudgetSummary(
        val totalAmount: Double,
        val savingsPercent: Int,
        val spendingPercent: Int,
        val entries: List<BudgetEntry>
    )

    sealed class BudgetState {
        object Loading : BudgetState()
        data class Success(val summary: BudgetSummary) : BudgetState()
        data class Error(val message: String) : BudgetState()
    }

    sealed class ActionState {
        object Idle : ActionState()
        object Loading : ActionState()
        object Success : ActionState()
        data class Error(val message: String) : ActionState()
    }

    private val _budgetState  = MutableLiveData<BudgetState>(BudgetState.Loading)
    val budgetState: LiveData<BudgetState> = _budgetState

    private val _actionState  = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    private val _activeFilter = MutableLiveData<FilterType>(FilterType.ALL)
    val activeFilter: LiveData<FilterType> = _activeFilter

    // Full list kept in memory so filtering doesn't need a network call
    private var allEntries: List<BudgetEntry> = emptyList()

    init { loadBudget() }

    fun loadBudget() {
        viewModelScope.launch {
            _budgetState.value = BudgetState.Loading
            val result = repository.getBudget()
            if (result.isSuccess) {
                allEntries = result.getOrThrow()
                // Keep same calculation as before
                _budgetState.value = BudgetState.Success(
                    BudgetSummary(
                        totalAmount     = repository.getTotalAmount(),
                        savingsPercent  = repository.getSavingsPercent(),
                        spendingPercent = repository.getSpendingPercent(),
                        entries         = allEntries
                    )
                )
            } else {
                _budgetState.value = BudgetState.Error("Failed to load budget")
            }
        }
    }

    // ✅ Only this is new — filters the already-loaded list, no network call
    fun setFilter(filter: FilterType) {
        // Tap same filter again → reset to ALL
        val newFilter = if (_activeFilter.value == filter) FilterType.ALL else filter
        _activeFilter.value = newFilter

        val filtered = when (newFilter) {
            FilterType.SAVINGS  -> allEntries.filter { it.type == BudgetEntry.Type.SAVINGS }
            FilterType.SPENDING -> allEntries.filter { it.type == BudgetEntry.Type.SPENDING }
            FilterType.ALL      -> allEntries
        }

        // Reuse existing Success state, just swap the entries list
        val current = _budgetState.value
        if (current is BudgetState.Success) {
            _budgetState.value = current.copy(
                summary = current.summary.copy(entries = filtered)
            )
        }
    }

    fun addEntry(label: String, amountStr: String, isSavings: Boolean) {
        val amount = amountStr.toDoubleOrNull()
        if (label.isBlank() || amount == null || amount <= 0) {
            _actionState.value = ActionState.Error("Please fill in all fields correctly")
            return
        }
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            val type = if (isSavings) BudgetEntry.Type.SAVINGS else BudgetEntry.Type.SPENDING
            val result = repository.addBudgetEntry(label, amount, type)
            _actionState.value = if (result.isSuccess) ActionState.Success
            else ActionState.Error("Failed to add entry")
            if (result.isSuccess) loadBudget()
        }
    }

    fun resetActionState() { _actionState.value = ActionState.Idle }
}