// model/ChatMessage.kt
package com.example.InvestEd.model

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Sender { USER, AI }
}