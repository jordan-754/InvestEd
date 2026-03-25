package com.example.InvestEd.ui.goals

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.databinding.ItemGoalBinding
import com.example.InvestEd.model.Goal

class GoalAdapter(
    private val onDelete: (Goal) -> Unit,
    private val onAddAmount: (Goal) -> Unit,
    private val onEdit: (Goal) -> Unit          // ← NEW callback
) : ListAdapter<Goal, GoalAdapter.GoalViewHolder>(DiffCallback()) {

    inner class GoalViewHolder(private val binding: ItemGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            binding.tvGoalTitle.text     = goal.title
            binding.tvTargetAmount.text  = "Target: ₱${String.format("%,.2f", goal.targetAmount)}"
            binding.tvDeadline.text      = "📅 By: ${goal.deadline}"
            binding.tvCurrentAmount.text = "Saved: ${goal.savedVsTarget}"

            binding.progressBar.max      = 10000
            binding.progressBar.progress = (goal.progressPercentFloat * 100).toInt()

            if (goal.progressPercent >= 100) {
                binding.tvProgress.text = "✅ Complete!"
                binding.tvProgress.setTextColor(Color.parseColor("#4CAF50"))
                binding.btnAddAmount.text = "Withdraw"
                binding.btnAddAmount.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#2196F3"))
            } else {
                binding.tvProgress.text = if (
                    goal.progressPercentFloat < 1f && goal.progressPercentFloat > 0f
                ) {
                    String.format("%.1f%%", goal.progressPercentFloat)
                } else {
                    "${goal.progressPercent}%"
                }
                binding.tvProgress.setTextColor(Color.parseColor("#3F51B5"))
                binding.btnAddAmount.text = "+ ADD"
                binding.btnAddAmount.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#3F51B5"))
            }

            binding.btnAddAmount.setOnClickListener { onAddAmount(goal) }

            // ── Edit button ──────────────────────────────────────────────
            binding.btnEdit.setOnClickListener { onEdit(goal) }

            // ── Delete button ────────────────────────────────────────────
            binding.btnDelete.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                    .setTitle("Delete Goal")
                    .setMessage("Are you sure you want to delete \"${goal.title}\"?")
                    .setPositiveButton("Delete") { _, _ -> onDelete(goal) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal) = oldItem == newItem
    }
}