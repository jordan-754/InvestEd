// viewmodel/AiChatViewModel.kt
package com.example.InvestEd.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.InvestEd.model.ChatMessage
import com.example.InvestEd.repository.ChatRepository
import kotlinx.coroutines.launch

class AiChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "0",
                text = "Hi! I'm your InvestEd AI Assistant. 👋\nYou can tap the suggestions below like \"Saving tips\" or \"How to invest\"—no need to type a message.",
                sender = ChatMessage.Sender.AI
            )
        )
    )
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isTyping = MutableLiveData(false)
    val isTyping: LiveData<Boolean> = _isTyping

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = text,
            sender = ChatMessage.Sender.USER
        )

        val current = _messages.value.orEmpty().toMutableList()
        current.add(userMsg)
        _messages.value = current
        _isTyping.value = true

        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            val aiResponse = repository.sendMessage(text)
            _isTyping.value = false

            val updated = _messages.value.orEmpty().toMutableList()
            updated.add(aiResponse)
            _messages.value = updated
        }
    }
}