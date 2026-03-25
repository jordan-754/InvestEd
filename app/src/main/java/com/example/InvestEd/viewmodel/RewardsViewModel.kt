package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.RedemptionItem
import com.example.InvestEd.model.RewardItem
import com.example.InvestEd.repository.MainRepository
import kotlinx.coroutines.launch

class RewardsViewModel(
    private val mainRepository: MainRepository = MainRepository()
) : ViewModel() {

    sealed class RewardsState {
        object Loading : RewardsState()
        data class Success(val points: Long) : RewardsState()
        data class Error(val message: String) : RewardsState()
    }

    sealed class ExchangeState {
        object Idle    : ExchangeState()
        object Loading : ExchangeState()
        object Pending : ExchangeState()
        data class Success(val pesosAdded: Double, val remainingPoints: Long) : ExchangeState()
        data class Error(val message: String) : ExchangeState()
    }

    private val _state = MutableLiveData<RewardsState>(RewardsState.Loading)
    val state: LiveData<RewardsState> = _state

    private val _exchangeState = MutableLiveData<ExchangeState>(ExchangeState.Idle)
    val exchangeState: LiveData<ExchangeState> = _exchangeState

    private val _rewards = MutableLiveData<List<RewardItem>>()
    val rewards: LiveData<List<RewardItem>> = _rewards

    private val _redemptions = MutableLiveData<List<RedemptionItem>>()
    val redemptions: LiveData<List<RedemptionItem>> = _redemptions

    init {
        loadRewards()
        loadRewardsFromFirestore()
        loadMyRedemptions()
    }

    fun loadRewards() {
        viewModelScope.launch {
            _state.value = RewardsState.Loading
            val result = mainRepository.getRewardPoints()
            if (result.isSuccess) {
                _state.value = RewardsState.Success(result.getOrThrow())
            } else {
                _state.value = RewardsState.Error("Failed to load rewards")
            }
        }
    }

    fun loadRewardsFromFirestore() {
        viewModelScope.launch {
            val result = mainRepository.getRewardItems()
            if (result.isSuccess) {
                _rewards.value = result.getOrThrow()
            } else {
                _rewards.value = emptyList()
            }
        }
    }

    fun loadMyRedemptions() {
        viewModelScope.launch {
            val result = mainRepository.getMyRedemptions()
            if (result.isSuccess) {
                _redemptions.value = result.getOrThrow()
            } else {
                _redemptions.value = emptyList()
            }
        }
    }

    fun exchangePoints(pointsCost: Long, pesosReward: Double) {
        val current = _state.value
        if (current !is RewardsState.Success) return
        if (current.points < pointsCost) {
            _exchangeState.value = ExchangeState.Error("Not enough points")
            return
        }
        _exchangeState.value = ExchangeState.Loading
        viewModelScope.launch {
            val result = mainRepository.requestRedemption(pointsCost, pesosReward)
            if (result.isSuccess) {
                _exchangeState.value = ExchangeState.Success(pesosReward, current.points - pointsCost)
                loadRewards()
                loadRewardsFromFirestore()
                loadMyRedemptions()
            } else {
                _exchangeState.value = ExchangeState.Error(
                    result.exceptionOrNull()?.message ?: "Exchange failed. Try again."
                )
            }
        }
    }

    fun resetExchangeState() { _exchangeState.value = ExchangeState.Idle }
}