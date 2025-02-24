package com.vtpartnertranspvtltd.vtpartneragent.models

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

data class OrderModel(
    val customerName: String,
    val bookingDate: String,
    val totalPrice: String,
    val customerImage: String? = null // Optional, can be null if no image
) {
    fun getFormattedPrice(): String {
        return "₹${totalPrice.toDoubleOrNull()?.roundToInt() ?: 0} /-"
    }

    fun getFormattedDate(): String {
        // You can add more sophisticated date formatting here
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(bookingDate)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            bookingDate
        }
    }

    fun getDayFromDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val date = inputFormat.parse(bookingDate)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }
}