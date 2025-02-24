package com.vtpartnertranspvtltd.vtpartneragent.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemOrdersBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.OrderModel
import de.hdodenhof.circleimageview.CircleImageView

class OrdersAdapter(private val orders: List<OrderModel>) :
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(private val binding: ItemOrdersBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: OrderModel) {
            binding.apply {
                // Set customer name
                customerName.text = order.customerName

                // Set date
                orderDate.text = "${order.getDayFromDate()}, ${order.getFormattedDate()}"

                // Set price
                orderAmount.apply {
                    text = order.getFormattedPrice()
                    setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
                }

                // Load customer image
//                order.customerImage?.let { imageUrl ->
//                    Glide.with(customerImage.context)
//                        .load(imageUrl)
//                        .placeholder(R.drawable.ic_person)
//                        .error(R.drawable.ic_person)
//                        .circleCrop()
//                        .into(customerImage)
//                } ?: run {
//                    // Set default image if no image URL
////                    customerImage.setImageResource(R.drawable.ic_person)
//                }

                // Add divider except for last item
                divider.isVisible = adapterPosition < orders.size - 1
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrdersBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size
}