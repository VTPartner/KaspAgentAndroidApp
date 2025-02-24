package com.vtpartnertranspvtltd.vtpartneragent.models

import com.google.gson.annotations.SerializedName

data class Vehicle(
    @SerializedName("vehicle_id")
    val vehicleId: String,
    
    @SerializedName("vehicle_name")
    val vehicleName: String,
    
    @SerializedName("category_id")
    val categoryId: Int,
    
    @SerializedName("vehicle_image")
    val vehicleImage: String? = null,
    
    @SerializedName("vehicle_description")
    val vehicleDescription: String? = null,
    
    @SerializedName("status")
    val status: Int = 1
)

data class VehicleResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("results")
    val results: List<Vehicle>
) 