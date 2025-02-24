package com.vtpartnertranspvtltd.vtpartneragent.data.models

import com.google.gson.annotations.SerializedName

data class UserData(
    @SerializedName("goods_driver_id")
    val driverId: String,
    @SerializedName("driver_first_name")
    val name: String?,
    @SerializedName("profile_pic")
    val profilePic: String?,
    @SerializedName("mobile_no")
    val mobileNo: String,
    @SerializedName("full_address")
    val address: String?,
    @SerializedName("is_profile_complete")
    val isProfileComplete: Boolean
) 