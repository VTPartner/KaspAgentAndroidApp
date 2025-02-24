package com.vtpartnertranspvtltd.vtpartneragent.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemRechargeHistoryBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.RechargeHistoryModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class RechargeHistoryAdapter(private val history: List<RechargeHistoryModel>) :
    RecyclerView.Adapter<RechargeHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRechargeHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recharge: RechargeHistoryModel) {
            binding.apply {
                paymentId.text = recharge.paymentId
                date.text = formatDate(recharge.rechargeDate)
                points.text = "${recharge.points.roundToInt()} Points Allotted"
                amount.text = "₹${recharge.amount.roundToInt()}/-"

                if (recharge.lastRechargeNegativePoints > 0) {
                    negativeAmount.apply {
                        text = "- ₹${recharge.lastRechargeNegativePoints}/-"
                        visibility = View.VISIBLE
                    }
                } else {
                    negativeAmount.visibility = View.GONE
                }

                divider.isVisible = adapterPosition < history.size - 1
            }
        }

        private fun formatDate(date: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault())
                val parsedDate = inputFormat.parse(date)
                outputFormat.format(parsedDate!!)
            } catch (e: Exception) {
                date
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemRechargeHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(history[position])
    }

    override fun getItemCount() = history.size
}