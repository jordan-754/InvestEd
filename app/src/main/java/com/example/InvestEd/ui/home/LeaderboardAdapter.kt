package com.example.InvestEd.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.databinding.ItemLeaderboardBinding
import com.example.InvestEd.model.LeaderboardEntry

class LeaderboardAdapter(private val currentUserId: String?) :
    ListAdapter<LeaderboardEntry, LeaderboardAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: LeaderboardEntry) {
            // Emojis replaced with rank text for aesthetics
            binding.tvRank.text = when (entry.rank) {
                1 -> "1st"
                2 -> "2nd"
                3 -> "3rd"
                else -> "#${entry.rank}"
            }
            
            // Apply distinctive colors for top 3
            val rankColor = when (entry.rank) {
                1 -> "#FFD700" // Gold
                2 -> "#C0C0C0" // Silver
                3 -> "#CD7F32" // Bronze
                else -> "#888888" // Default
            }
            binding.tvRank.setTextColor(Color.parseColor(rankColor))

            binding.tvName.text = entry.name
            binding.tvScore.text = "${entry.score.toInt()} pts"
            binding.tvDetails.text = "Reward Points"
            // Highlight current user
            if (entry.userId == currentUserId) {
                binding.root.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            } else {
                binding.root.setCardBackgroundColor(Color.WHITE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<LeaderboardEntry>() {
        override fun areItemsTheSame(oldItem: LeaderboardEntry, newItem: LeaderboardEntry) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: LeaderboardEntry, newItem: LeaderboardEntry) = oldItem == newItem
    }
}
