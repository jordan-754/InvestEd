package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.Badge
import com.example.InvestEd.repository.BadgeRepository
import kotlinx.coroutines.launch

class BadgeViewModel(
    private val repository: BadgeRepository = BadgeRepository()
) : ViewModel() {

    sealed class BadgeState {
        object Loading : BadgeState()
        data class Success(val badges: List<Badge>) : BadgeState()
        data class Error(val message: String) : BadgeState()
    }

    private val _badgeState = MutableLiveData<BadgeState>(BadgeState.Loading)
    val badgeState: LiveData<BadgeState> = _badgeState

    fun loadBadges() {
        viewModelScope.launch {
            _badgeState.value = BadgeState.Loading
            val result = repository.getBadges()
            if (result.isSuccess) {
                _badgeState.value = BadgeState.Success(result.getOrThrow())
            } else {
                _badgeState.value = BadgeState.Error("Failed to load badges")
            }
        }
    }
}