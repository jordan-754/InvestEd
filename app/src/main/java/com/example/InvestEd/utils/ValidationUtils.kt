// utils/ValidationUtils.kt
package com.example.InvestEd.utils

import android.util.Patterns

/**
 * Reusable validation helpers.
 * Call these from ViewModels (never from Fragments directly).
 */
object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun doPasswordsMatch(password: String, confirm: String): Boolean {
        return password == confirm
    }

    fun isNotBlank(vararg fields: String): Boolean {
        return fields.all { it.isNotBlank() }
    }

    fun isValidOtp(otp: String): Boolean {
        return otp.length == 6 && otp.all { it.isDigit() }
    }
}