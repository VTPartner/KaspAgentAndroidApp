package com.vtpartnertranspvtltd.vtpartneragent.data.models

import com.google.gson.annotations.SerializedName

data class City(
    @SerializedName("city_id")
    val cityId: String,
    @SerializedName("city_name")
    val cityName: String
)

data class CitiesResponse(
    @SerializedName("results")
    val results: List<City>?
) 