data class TripDetails(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerImage: String?,
    val pickupLocation: TripLocation,
    val dropLocation: TripLocation,
    val distance: Double,
    val fare: Double,
    val status: String,
    val otp: String,
    val totalTime: String,
    val totalPrice: Double,
    val totalDistance: String,
    val bookingId: String,
    val pickupAddress: String,
    val dropAddress: String,
    val senderName: String,
    val senderNumber: String,
    val receiverName: String,
    val receiverNumber: String
) 