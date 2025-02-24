import com.google.gson.annotations.SerializedName

data class EarningsResponse(
    @SerializedName("todays_earnings")
    val todaysEarnings: Double,
    
    @SerializedName("todays_rides")
    val todaysRides: Int,
    
    @SerializedName("total_earnings")
    val totalEarnings: Double,
    
    @SerializedName("total_rides")
    val totalRides: Int
) 