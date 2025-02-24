package com.vtpartnertranspvtltd.vtpartneragent.adapters

import OrderRidesModel
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemRideBinding


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class RidesAdapter(
    private val rides: List<OrderRidesModel>,
    private val onItemClick: (OrderRidesModel) -> Unit
) : RecyclerView.Adapter<RidesAdapter.RideViewHolder>() {

    inner class RideViewHolder(private val binding:ItemRideBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: OrderRidesModel) {
            binding.apply {
                // Load customer image
                Glide.with(customerImage)
                    .load(order.customerImage)
                    .placeholder(R.drawable.demo_user)
                    .error(R.drawable.demo_user)
                    .circleCrop()
                    .into(customerImage)

                // Set texts
                dateTime.text = formatDateTime(order.bookingTiming)
                customerName.text = order.customerName
                amount.text = "₹${order.totalPrice.roundToInt()}/-"
                pickupAddress.text = order.pickupAddress
                dropAddress.text = order.dropAddress

                // Handle click
                root.setOnClickListener { onItemClick(order) }
            }
        }

        private fun formatDateTime(timestamp: Double): String {
            return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date((timestamp * 1000).toLong()))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        return RideViewHolder(
            ItemRideBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, 
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        holder.bind(rides[position])
    }

    override fun getItemCount() = rides.size
}