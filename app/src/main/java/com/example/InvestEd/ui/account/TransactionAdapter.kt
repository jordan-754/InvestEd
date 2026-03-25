// ui/account/TransactionAdapter.kt
package com.example.InvestEd.ui.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.R
import com.example.InvestEd.databinding.ItemTransactionBinding
import com.example.InvestEd.model.Transaction

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Transaction) {
            val (iconRes, bgTint) = when (item.type) {
                "investment" -> R.drawable.ic_invest to "#E8F5E9"
                "reward"     -> R.drawable.ic_rewards to "#F3E5F5"
                "savings"    -> R.drawable.ic_goals to "#E3F2FD"
                "spending"   -> R.drawable.ic_budget to "#FFF3E0"
                "interest"   -> R.drawable.ic_invest to "#E8F5E9"
                "withdrawal"      -> R.drawable.ic_budget to "#FFEBEE"
                "goal_withdrawal" -> R.drawable.ic_budget to "#FFEBEE"
                else         -> R.drawable.ic_transaction to "#F5F5F5"
            }

            binding.ivIcon.setImageResource(iconRes)
            binding.layoutIcon.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(bgTint)
                )

            binding.tvType.text = when (item.type) {
                "investment" -> "Cash In Investment"
                "interest"   -> "Daily Interest"
                "reward"     -> "Reward Exchange"
                "savings"    -> "Savings"
                "spending"   -> "Spending"
                "withdrawal"    -> "Withdrawal"
                "goal_withdrawal" -> "Goal Withdrawal"
                else         -> item.type.replaceFirstChar { it.uppercase() }
            }

            binding.tvStatus.text = when (item.type) {
                "interest"   -> "Interest"
                "investment" -> item.displayStatus
                "reward"     -> "Reward"
                else         -> item.displayStatus
            }

            binding.tvDate.text = item.formattedDate

            val prefix = when (item.type) {
                "spending", "withdrawal", "goal_withdrawal" -> "-"
                else -> "+"
            }
            binding.tvAmount.text = "$prefix₱${String.format("%.2f", item.amount)}"

            val amountColor = when (prefix) {
                "+" -> android.graphics.Color.parseColor("#4CAF50")
                else -> android.graphics.Color.parseColor("#F44336")
            }
            binding.tvAmount.setTextColor(amountColor)
        }
    } // ← closing brace for ViewHolder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
        override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
    }
}