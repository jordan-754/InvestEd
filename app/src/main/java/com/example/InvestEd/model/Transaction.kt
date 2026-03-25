// model/Transaction.kt
package com.example.InvestEd.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "investment", // investment, reward, savings, spending
    val status: String = "pending",  // pending, approved, rejected
    val createdAt: Timestamp? = null,
    val description: String? = null
) : Parcelable {
    val formattedDate: String
        get() {
            if (createdAt == null) return "Unknown date"
            val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
            return sdf.format(createdAt.toDate())
        }

    val displayStatus: String
        get() = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}