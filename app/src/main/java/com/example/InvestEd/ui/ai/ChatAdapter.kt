// ui/ai/ChatAdapter.kt
package com.example.InvestEd.ui.ai

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.databinding.ItemChatAiBinding
import com.example.InvestEd.databinding.ItemChatUserBinding
import com.example.InvestEd.model.ChatMessage

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI   = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).sender == ChatMessage.Sender.USER) VIEW_TYPE_USER
        else VIEW_TYPE_AI
    }

    inner class UserViewHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) { binding.tvMessage.text = message.text }
    }

    inner class AiViewHolder(private val binding: ItemChatAiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) { binding.tvMessage.text = message.text }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            UserViewHolder(ItemChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            AiViewHolder(ItemChatAiBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserViewHolder -> holder.bind(getItem(position))
            is AiViewHolder  -> holder.bind(getItem(position))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
    }
}