import com.google.gson.annotations.SerializedName

data class DriverStatus(
    @SerializedName("is_online")
    val isOnline: Int,
    
    @SerializedName("profile_pic")
    val profilePic: String,
    
    @SerializedName("mobile_no")
    val mobileNo: String,
    
    @SerializedName("driver_first_name")
    val driverFirstName: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("recent_online_pic")
    val recentOnlinePic: String?
) 