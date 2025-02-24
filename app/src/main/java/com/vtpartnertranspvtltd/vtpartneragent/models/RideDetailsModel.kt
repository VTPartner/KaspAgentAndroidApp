package com.vtpartnertranspvtltd.vtpartneragent.models

data class RideDetailsModel(
    val customerName: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val distance: String = "",
    val totalTime: String = "",
    val senderName: String = "",
    val senderNumber: String = "",
    val receiverName: String = "",
    val receiverNumber: String = "",
    val totalPrice: Double = 0.0,
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val customerNumber: String = "",
    val bookingStatus: String = "",
    val otp: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0
)