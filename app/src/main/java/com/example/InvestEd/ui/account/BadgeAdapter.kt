package com.example.InvestEd.ui.account

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.databinding.ItemBadgeBinding
import com.example.InvestEd.model.Badge

class BadgeAdapter : ListAdapter<Badge, BadgeAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemBadgeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Badge) {
            // ✅ Use emoji icon from Firestore instead of drawable
            binding.tvBadgeIcon.text  = item.icon
            binding.tvBadgeName.text  = item.name
            binding.tvBadgeDescription.text = item.description
            binding.tvBadgePoints.text = "+${item.points} pts"

            if (item.isUnlocked) {
                binding.tvBadgeIcon.alpha  = 1.0f
                binding.tvBadgeName.setTextColor(android.graphics.Color.parseColor("#212121"))
                binding.tvBadgePoints.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                binding.cardBadge.setCardBackgroundColor(android.graphics.Color.WHITE)
                binding.tvStatus.text      = "✅ Unlocked"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                binding.tvBadgeIcon.alpha  = 0.3f
                binding.tvBadgeName.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
                binding.tvBadgePoints.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
                binding.cardBadge.setCardBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                binding.tvStatus.text      = "🔒 Locked"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            }

            binding.root.setOnClickListener {
                val status = if (item.isUnlocked) "Unlocked! (+${item.points} pts)" else "Locked"
                Toast.makeText(
                    binding.root.context,
                    "${item.name}: ${item.description} ($status)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Badge>() {
        override fun areItemsTheSame(a: Badge, b: Badge) = a.id == b.id
        override fun areContentsTheSame(a: Badge, b: Badge) = a == b
    }
}