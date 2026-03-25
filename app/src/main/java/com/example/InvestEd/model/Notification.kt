// model/Notification.kt
package com.example.InvestEd.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val timestamp: Long = 0L
) : Parcelable {
    val timeAgo: String
        get() {
            val diff = System.currentTimeMillis() - timestamp
            val minutes = diff / 60000
            val hours = minutes / 60
            val days = hours / 24
            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                else -> "${days}d ago"
            }
        }

    val icon: String
        get() = when (type) {
            "INVEST" -> "📈"
            "GOAL_ADD" -> "🎯"
            "GOAL_AMOUNT" -> "💰"
            "GOAL_DELETE" -> "🗑️"
            "BUDGET" -> "📊"
            "LESSON" -> "📚"
            else -> "🔔"
        }
}