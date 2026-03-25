// model/User.kt
package com.example.InvestEd.model

data class User(
    val firstName: String,
    val lastName: String,
    val email: String,
    val birthdate: String,
    val username: String,     // Auto-generated or from firstName
    val accountId: String     // Auto-generated account ID
)