package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.Notification
import com.example.InvestEd.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    sealed class NotifState {
        object Loading : NotifState()
        data class Success(val notifications: List<Notification>) : NotifState()
        object Empty : NotifState()
        data class Error(val message: String) : NotifState()
    }

    private val _state = MutableLiveData<NotifState>(NotifState.Loading)
    val state: LiveData<NotifState> = _state

    init { loadNotifications() }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.value = NotifState.Loading
            val result = repository.getNotifications()
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _state.value = if (list.isEmpty()) NotifState.Empty
                else NotifState.Success(list)
            } else {
                _state.value = NotifState.Error("Failed to load notifications")
            }
        }
    }
}
