import com.google.gson.annotations.SerializedName

data class ImageUploadResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("image_url")
    val imageUrl: String
) 