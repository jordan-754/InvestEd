package com.example.InvestEd.ui.rewards

import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.R
import com.example.InvestEd.model.RedemptionItem

class RedemptionAdapter : RecyclerView.Adapter<RedemptionAdapter.VH>() {

    private var items = listOf<RedemptionItem>()
    fun submitList(list: List<RedemptionItem>) { items = list; notifyDataSetChanged() }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvCost:   TextView = view.findViewById(R.id.tvRedemptionCost)
        val tvStatus: TextView = view.findViewById(R.id.tvRedemptionStatus)
        val tvDate:   TextView = view.findViewById(R.id.tvRedemptionDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_redemption, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvCost.text = "${String.format("%,d", item.cost)} pts = ₱${String.format("%.2f", item.pesosReward)}"
        holder.tvStatus.text = item.status.replaceFirstChar { it.uppercase() }
        holder.tvStatus.setTextColor(when (item.status) {
            "approved" -> Color.parseColor("#16A34A")
            "rejected" -> Color.parseColor("#DC2626")
            else       -> Color.parseColor("#D97706")
        })
        holder.tvDate.text = item.requestedAt?.toDate()?.let {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)
        } ?: ""
    }

    override fun getItemCount() = items.size
}