data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val results: List<T>
) 