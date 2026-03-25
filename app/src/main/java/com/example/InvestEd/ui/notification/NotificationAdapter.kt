// ui/notification/NotificationAdapter.kt
package com.example.InvestEd.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.R
import com.example.InvestEd.databinding.ItemNotificationBinding
import com.example.InvestEd.model.Notification

class NotificationAdapter : ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            val context = binding.root.context
            
            // Map types to icons and colors
            val (iconRes, iconTint) = when (notification.type) {
                "INVEST" -> R.drawable.ic_invest to R.color.green
                "GOAL_ADD" -> R.drawable.ic_goals to R.color.blue
                "GOAL_AMOUNT" -> R.drawable.ic_invest to R.color.green
                "GOAL_DELETE" -> R.drawable.ic_delete to R.color.red
                "BUDGET" -> R.drawable.ic_budget to R.color.orange
                "LESSON" -> R.drawable.ic_learning to R.color.purple
                else -> R.drawable.ic_notification to R.color.blue
            }

            binding.ivNotifIcon.setImageResource(iconRes)
            binding.ivNotifIcon.setColorFilter(ContextCompat.getColor(context, iconTint))
            
            binding.tvNotifTitle.text = notification.title
            binding.tvNotifMessage.text = notification.message
            binding.tvNotifTime.text = notification.timeAgo
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(a: Notification, b: Notification) = a.id == b.id
        override fun areContentsTheSame(a: Notification, b: Notification) = a == b
    }
}