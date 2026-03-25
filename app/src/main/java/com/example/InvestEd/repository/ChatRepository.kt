package com.example.InvestEd.repository

import com.example.InvestEd.model.ChatMessage

class ChatRepository {
    fun sendMessage(userMessage: String): ChatMessage {
        val reply = generateAiReply(userMessage)
        return ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = reply,
            sender = ChatMessage.Sender.AI
        )
    }

    private fun generateAiReply(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("save") || lower.contains("saving") ->
                "Great question! Save at least 20% of your income. Even ₱50/day adds up to ₱18,000 a year!"
            lower.contains("invest") ->
                "Micro-investing is perfect for beginners. Start with as little as ₱20. Consistency matters more than amount!"
            lower.contains("budget") ->
                "Try the 50/30/20 rule: 50% needs, 30% wants, 20% savings. Track every peso in the Budget Tracker tab."
            lower.contains("goal") ->
                "Set SMART goals — Specific, Measurable, Achievable, Relevant, Time-bound. Use the Goals tab!"
            lower.contains("emergency") ->
                "Aim for 3-6 months of expenses in an emergency fund in a separate, accessible account."
            lower.contains("hello") || lower.contains("hi") ->
                "Hi there! I'm your InvestEd AI Assistant. Ask me about saving, investing, budgeting, or financial goals!"
            else ->
                "Great question! Check the Learning Hub for lessons, or ask me about saving, investing, or budgeting."
        }
    }
}
