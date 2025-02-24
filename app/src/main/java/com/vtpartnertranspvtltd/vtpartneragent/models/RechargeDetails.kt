import com.google.gson.annotations.SerializedName

data class RechargeDetails(
    @SerializedName("remaining_points")
    val remainingPoints: Double,
    
    @SerializedName("negative_points")
    val negativePoints: Double,
    
    @SerializedName("valid_till_date")
    val validTillDate: String
) 