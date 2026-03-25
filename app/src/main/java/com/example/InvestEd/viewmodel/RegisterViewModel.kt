package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.repository.AuthRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class RegisterViewModel(
    private val repository: AuthRepository = AuthRepository.getInstance()
) : ViewModel() {

    sealed class RegisterState {
        object Idle    : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val registerState: LiveData<RegisterState> = _registerState

    fun register(
        firstName: String, lastName: String, email: String,
        birthdate: String, school: String, place: String,
        password: String, confirmPassword: String
    ) {
        // ─── FIRST NAME ───────────────────────────────────────────────────────
        if (firstName.trim().isBlank()) {
            _registerState.value = RegisterState.Error("First name is required")
            return
        }
        if (firstName.trim().length < 2) {
            _registerState.value = RegisterState.Error("First name must be at least 2 characters")
            return
        }
        if (!firstName.trim().matches(Regex("^[a-zA-Z ]+$"))) {
            _registerState.value = RegisterState.Error("First name must contain letters only")
            return
        }

        // ─── LAST NAME ────────────────────────────────────────────────────────
        if (lastName.trim().isBlank()) {
            _registerState.value = RegisterState.Error("Last name is required")
            return
        }
        if (lastName.trim().length < 2) {
            _registerState.value = RegisterState.Error("Last name must be at least 2 characters")
            return
        }
        if (!lastName.trim().matches(Regex("^[a-zA-Z ]+$"))) {
            _registerState.value = RegisterState.Error("Last name must contain letters only")
            return
        }

        // ─── EMAIL ────────────────────────────────────────────────────────────
        if (email.trim().isBlank()) {
            _registerState.value = RegisterState.Error("Email is required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _registerState.value = RegisterState.Error("Please enter a valid email address")
            return
        }
        if (!email.trim().endsWith("@gmail.com")) {
            _registerState.value = RegisterState.Error("Only @gmail.com addresses are allowed")
            return
        }

        // ─── BIRTHDATE ────────────────────────────────────────────────────────
        if (birthdate.isBlank()) {
            _registerState.value = RegisterState.Error("Birthdate is required")
            return
        }
        val dateError = validateBirthdate(birthdate)
        if (dateError != null) {
            _registerState.value = RegisterState.Error(dateError)
            return
        }

        // ─── SCHOOL ───────────────────────────────────────────────────────────
        if (school.trim().isBlank()) {
            _registerState.value = RegisterState.Error("School is required")
            return
        }
        if (school.trim().length < 3) {
            _registerState.value = RegisterState.Error("Please enter a valid school name")
            return
        }

        // ─── PLACE ────────────────────────────────────────────────────────────
        if (place.trim().isBlank()) {
            _registerState.value = RegisterState.Error("Place is required")
            return
        }
        if (place.trim().length < 3) {
            _registerState.value = RegisterState.Error("Please enter a valid place")
            return
        }

        // ─── PASSWORD ─────────────────────────────────────────────────────────
        if (password.isBlank()) {
            _registerState.value = RegisterState.Error("Password is required")
            return
        }
        if (password.length < 8) {
            _registerState.value = RegisterState.Error("Password must be at least 8 characters")
            return
        }
        if (!password.any { it.isUpperCase() }) {
            _registerState.value = RegisterState.Error("Password must contain at least one uppercase letter")
            return
        }
        if (!password.any { it.isLowerCase() }) {
            _registerState.value = RegisterState.Error("Password must contain at least one lowercase letter")
            return
        }
        if (!password.any { it.isDigit() }) {
            _registerState.value = RegisterState.Error("Password must contain at least one number")
            return
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            _registerState.value = RegisterState.Error("Password must contain at least one special character (!@#\$%^&*)")
            return
        }

        // ─── CONFIRM PASSWORD ─────────────────────────────────────────────────
        if (confirmPassword.isBlank()) {
            _registerState.value = RegisterState.Error("Please confirm your password")
            return
        }
        if (password != confirmPassword) {
            _registerState.value = RegisterState.Error("Passwords do not match")
            return
        }

        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            val result = repository.register(
                firstName.trim(), lastName.trim(), email.trim(),
                birthdate, school.trim(), place.trim(), password
            )
            _registerState.value = if (result.isSuccess) RegisterState.Success
            else RegisterState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
        }
    }

    fun completeGoogleProfile(birthdate: String, school: String, place: String) {
        if (birthdate.isBlank()) {
            _registerState.value = RegisterState.Error("Birthdate is required")
            return
        }
        val dateError = validateBirthdate(birthdate)
        if (dateError != null) {
            _registerState.value = RegisterState.Error(dateError)
            return
        }
        if (school.trim().isBlank()) {
            _registerState.value = RegisterState.Error("School is required")
            return
        }
        if (school.trim().length < 3) {
            _registerState.value = RegisterState.Error("Please enter a valid school name")
            return
        }
        if (place.trim().isBlank()) {
            _registerState.value = RegisterState.Error("Place is required")
            return
        }
        if (place.trim().length < 3) {
            _registerState.value = RegisterState.Error("Please enter a valid place")
            return
        }
        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            val result = repository.completeProfile(birthdate, school.trim(), place.trim())
            _registerState.value = if (result.isSuccess) RegisterState.Success
            else RegisterState.Error(result.exceptionOrNull()?.message ?: "Failed to save profile")
        }
    }

    private fun validateBirthdate(birthdate: String): String? {
        return try {
            val parts = birthdate.split("/")
            if (parts.size != 3) return "Invalid date format (MM/DD/YYYY)"
            val month = parts[0].toInt()
            val day   = parts[1].toInt()
            val year  = parts[2].toInt()

            if (month < 1 || month > 12) return "Invalid month"
            if (day < 1 || day > 31)     return "Invalid day"
            if (year < 1900)             return "Invalid birth year"
            if (year > 2026)             return "Birth year cannot be after 2026"

            val today = Calendar.getInstance()
            val birthday = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }

            // ✅ Check if date is in the future
            if (birthday.after(today)) return "Birthdate cannot be in the future"

            // ✅ Check minimum age — must be 13 years or older
            var age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthday.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age < 13) return "You must be at least 13 years old to register"

            null
        } catch (e: NumberFormatException) {
            "Invalid date format"
        }
    }

    fun resetState() { _registerState.value = RegisterState.Idle }
}