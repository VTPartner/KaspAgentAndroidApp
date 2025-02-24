import com.google.gson.annotations.SerializedName

data class BookingDetails(
    @SerializedName("booking_id")
    val bookingId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("pickup_location")
    val pickupLocation: String,
    
    @SerializedName("drop_location")
    val dropLocation: String,
    
    @SerializedName("customer_name")
    val customerName: String,
    
    @SerializedName("customer_phone")
    val customerPhone: String,
    
    @SerializedName("fare_amount")
    val fareAmount: Double,
    
    @SerializedName("distance")
    val distance: Double,
    
    @SerializedName("pickup_lat")
    val pickupLat: Double,
    
    @SerializedName("pickup_lng")
    val pickupLng: Double,
    
    @SerializedName("drop_lat")
    val dropLat: Double,
    
    @SerializedName("drop_lng")
    val dropLng: Double,
    
    @SerializedName("created_at")
    val createdAt: String
) 