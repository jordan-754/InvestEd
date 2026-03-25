package com.example.InvestEd.viewmodel

import androidx.lifecycle.*
import com.example.InvestEd.model.User
import com.example.InvestEd.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository.getInstance()
) : ViewModel() {

    sealed class LoginState {
        object Idle    : LoginState()
        object Loading : LoginState()
        data class Success(val user: User)        : LoginState()
        data class GoogleSuccess(val user: User, val isProfileComplete: Boolean) : LoginState()
        data class Error(val message: String)     : LoginState()
    }

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Please enter email and password")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            _loginState.value = if (result.isSuccess) {
                LoginState.Success(result.getOrThrow())
            } else {
                LoginState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(account)
            if (result.isSuccess) {
                // ✅ Check if birthdate/school/place are filled
                val isComplete = repository.isProfileComplete()
                _loginState.value = LoginState.GoogleSuccess(
                    user              = result.getOrThrow(),
                    isProfileComplete = isComplete
                )
            } else {
                _loginState.value = LoginState.Error(
                    result.exceptionOrNull()?.message ?: "Google sign-in failed"
                )
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}