package com.vtpartnertranspvtltd.vtpartneragent.models

import org.json.JSONObject

data class OrderRatingModel(
    val customerName: String,
    val bookingTiming: Long,
    val ratings: String,
    val ratingDescription: String?
) {
    companion object {
        fun fromJson(json: JSONObject): OrderRatingModel {
            return OrderRatingModel(
                customerName = json.getString("customer_name"),
                bookingTiming = json.getString("booking_timing").toDouble().toLong(),
                ratings = json.getString("ratings"),
                ratingDescription = json.getString("rating_description").takeIf { it != "NA" }
            )
        }
    }
}