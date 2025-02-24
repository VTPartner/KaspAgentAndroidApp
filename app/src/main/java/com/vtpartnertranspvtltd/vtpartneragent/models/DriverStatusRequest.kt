data class DriverStatusRequest(
    val driverId: String,
    val status: Int,
    val latitude: Double,
    val longitude: Double,
    val recentOnlinePic: String?
) 