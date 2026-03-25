// ui/budget/BudgetAdapter.kt
package com.example.InvestEd.ui.budget

import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.databinding.ItemBudgetBinding
import com.example.InvestEd.model.BudgetEntry

class BudgetAdapter : ListAdapter<BudgetEntry, BudgetAdapter.BudgetViewHolder>(DiffCallback()) {

    inner class BudgetViewHolder(private val binding: ItemBudgetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: BudgetEntry) {
            binding.tvLabel.text = entry.label
            binding.tvAmount.text = "₱${String.format("%.2f", entry.amount)}"
            binding.tvDate.text = entry.date
            binding.tvType.text = if (entry.type == BudgetEntry.Type.SAVINGS) "Savings" else "Spending"
            binding.tvType.setTextColor(
                if (entry.type == BudgetEntry.Type.SAVINGS) Color.parseColor("#4CAF50")
                else Color.parseColor("#F44336")
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<BudgetEntry>() {
        override fun areItemsTheSame(oldItem: BudgetEntry, newItem: BudgetEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BudgetEntry, newItem: BudgetEntry) = oldItem == newItem
    }
}