package com.vtpartnertranspvtltd.vtpartneragent.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.RecyclerView
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemRechargePlanBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.RechargeModel
import com.vtpartnertranspvtltd.vtpartneragent.utils.RechargeDiffCallback

class RechargeAdapter(
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<RechargeAdapter.RechargeViewHolder>() {

    private val recharges = mutableListOf<RechargeModel>()

    inner class RechargeViewHolder(private val binding: ItemRechargePlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClick(adapterPosition)
            }
        }

        fun bind(recharge: RechargeModel) {
            binding.apply {
                // Set amount
                amountText.text = "₹ ${recharge.amount}/-"

                // Set description
                descriptionText.text = recharge.description

                // Set validity
                validityText.text = "${recharge.validDays} Days"

                // Show/hide divider based on position
                divider.isVisible = adapterPosition < recharges.size - 1
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RechargeViewHolder {
        val binding = ItemRechargePlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RechargeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RechargeViewHolder, position: Int) {
        holder.bind(recharges[position])
    }

    override fun getItemCount() = recharges.size

    fun updateData(newRecharges: List<RechargeModel>) {
        val diffCallback = RechargeDiffCallback(recharges, newRecharges)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        recharges.clear()
        recharges.addAll(newRecharges)

        diffResult.dispatchUpdatesTo(this)
    }
}

// DiffUtil Callback
