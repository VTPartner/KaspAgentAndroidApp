package com.vtpartnertranspvtltd.vtpartneragent.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemRatingBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.OrderRatingModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RatingsAdapter(private val ratings: List<OrderRatingModel>) :
    RecyclerView.Adapter<RatingsAdapter.RatingViewHolder>() {

    inner class RatingViewHolder(private val binding: ItemRatingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(rating: OrderRatingModel) {
            binding.apply {
                customerName.text = rating.customerName
                bookingTime.text = formatDateTime(rating.bookingTiming)
                ratingValue.text = rating.ratings
                
                rating.ratingDescription?.let {
                    ratingDescription.apply {
                        text = it
                        visibility = View.VISIBLE
                    }
                } ?: run {
                    ratingDescription.visibility = View.GONE
                }

                divider.isVisible = adapterPosition < ratings.size - 1
            }
        }

        private fun formatDateTime(timestamp: Long): String {
            return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(timestamp * 1000))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
        return RatingViewHolder(
            ItemRatingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, 
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
        holder.bind(ratings[position])
    }

    override fun getItemCount() = ratings.size
}