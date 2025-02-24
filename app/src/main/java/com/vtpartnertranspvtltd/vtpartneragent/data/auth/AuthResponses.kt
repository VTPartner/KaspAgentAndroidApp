package com.vtpartnertranspvtltd.vtpartneragent.data.auth

import com.google.gson.annotations.SerializedName
import com.vtpartnertranspvtltd.vtpartneragent.data.models.UserData

data class LoginResponse(
    @SerializedName("results")
    val results: List<UserDetails>? = null,

    @SerializedName("result")
    val result: List<UserDetails>? = null
)

data class UserDetails(
    @SerializedName("driver_first_name")
    val goodsDriverName: String,

    @SerializedName("goods_driver_id")
    val goodsDriverID: Int,

    @SerializedName("profile_pic")
    val profilePic: String,

    @SerializedName("mobile_no")
    val mobileNo: String,

    @SerializedName("full_address")
    val fullAddress: String,

    )

data class OtpResponse(
    @SerializedName("message")
    val message: String?,

    @SerializedName("otp")
    val otp: String
)

data class VerifyOTPResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: UserData?
) 