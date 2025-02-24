import com.google.gson.annotations.SerializedName

data class ActiveDriverRequest(
    @SerializedName("goods_driver_id")
    val driverId: String,
    
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("current_lat")
    val currentLat: Double,
    
    @SerializedName("current_lng")
    val currentLng: Double
) 