package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.User
import com.example.InvestEd.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OtpViewModel(
    private val repository: AuthRepository = AuthRepository.getInstance()
) : ViewModel() {

    sealed class OtpState {
        object Idle : OtpState()
        object Loading : OtpState()
        object OtpSent : OtpState()
        data class VerifySuccess(val user: User) : OtpState()
        data class Error(val message: String) : OtpState()
    }

    private val _otpState = MutableLiveData<OtpState>(OtpState.Idle)
    val otpState: LiveData<OtpState> = _otpState

    private val _countdown = MutableLiveData(0)
    val countdown: LiveData<Int> = _countdown

    fun sendOtp(email: String) {
        if (email.isBlank()) {
            _otpState.value = OtpState.Error("Please enter your email")
            return
        }
        _otpState.value = OtpState.Loading
        viewModelScope.launch {
            val result = repository.sendOtp(email)
            if (result.isSuccess) {
                _otpState.value = OtpState.OtpSent
                startCountdown()
            } else {
                _otpState.value = OtpState.Error("Failed to send OTP")
            }
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.length < 6) {
            _otpState.value = OtpState.Error("Please enter all 6 digits")
            return
        }
        _otpState.value = OtpState.Loading
        viewModelScope.launch {
            val result = repository.verifyOtp(otp)
            _otpState.value = if (result.isSuccess) {
                val user = repository.getRegisteredUser()
                if (user != null) OtpState.VerifySuccess(user)
                else OtpState.Error("User not found")
            } else {
                OtpState.Error("Invalid OTP. Try: 123456")
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            for (i in 60 downTo 0) {
                _countdown.value = i
                delay(1000)
            }
        }
    }

    fun resetState() { _otpState.value = OtpState.Idle }
}