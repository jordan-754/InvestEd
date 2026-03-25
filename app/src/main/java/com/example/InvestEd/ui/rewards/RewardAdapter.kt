package com.example.InvestEd.ui.rewards

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.R
import com.example.InvestEd.model.RewardItem

class RewardAdapter(
    private val onExchange: (RewardItem) -> Unit
) : RecyclerView.Adapter<RewardAdapter.VH>() {

    private var items = listOf<RewardItem>()
    private var userPoints = 0

    fun submitList(list: List<RewardItem>) { items = list; notifyDataSetChanged() }
    fun setUserPoints(pts: Int) { userPoints = pts; notifyDataSetChanged() }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvCost: TextView    = view.findViewById(R.id.tvRewardCost)
        val tvPesos: TextView   = view.findViewById(R.id.tvRewardPesos)
        val btnExchange: Button = view.findViewById(R.id.btnExchangeReward)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val reward = items[position]
        holder.tvCost.text  = "${String.format("%,d", reward.cost)} pts"
        holder.tvPesos.text = "= ₱${String.format("%.2f", reward.pesosEquivalent)}"
        holder.btnExchange.setOnClickListener { onExchange(reward) }
    }

    override fun getItemCount() = items.size
}