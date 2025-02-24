package com.vtpartnertranspvtltd.vtpartneragent.data.auth

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("mobile_no")
    val phoneNumber: String,
    @SerializedName("country_code")
    val countryCode: String,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null
)

data class SendOTPRequest(
    @SerializedName("mobile_no")
    val phoneNumber: String
)

data class VerifyOTPRequest(
    @SerializedName("mobile_no")
    val phoneNumber: String,
    @SerializedName("otp")
    val otp: String
) 