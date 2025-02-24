package com.vtpartnertranspvtltd.vtpartneragent.network

import ActiveDriverRequest
import ApiResponse
import BookingDetails
import DriverStatus
import DriverStatusRequest
import EarningsResponse
import ImageUploadResponse
import RechargeDetails
import TripDetails
import com.vtpartnertranspvtltd.vtpartneragent.data.auth.LoginRequest
import com.vtpartnertranspvtltd.vtpartneragent.data.auth.LoginResponse
import com.vtpartnertranspvtltd.vtpartneragent.data.auth.OtpResponse

import com.vtpartnertranspvtltd.vtpartneragent.data.auth.VerifyOTPRequest
import com.vtpartnertranspvtltd.vtpartneragent.data.auth.VerifyOTPResponse
import com.vtpartnertranspvtltd.vtpartneragent.data.models.CitiesResponse
import com.vtpartnertranspvtltd.vtpartneragent.models.VehicleResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @POST("goods_driver_login")
    suspend fun login(@Body params: Map<String, String>): Response<LoginResponse>

    @POST("send_otp")
    suspend fun sendOtp(@Body params: Map<String, String>): Response<OtpResponse>

    @POST("verify_otp")
    suspend fun verifyOTP(@Body request: VerifyOTPRequest): Response<VerifyOTPResponse>

    @POST("all_cities")
    suspend fun getCities(): Response<CitiesResponse>

    @POST("goods_driver_update_online_status")
    suspend fun updateDriverStatus(
        @Body request: DriverStatusRequest
    ): Response<ApiResponse<Any>>

    @POST("goods_driver_online_status")
    suspend fun getDriverOnlineStatus(
        @Query("goods_driver_id") driverId: String
    ): Response<ApiResponse<DriverStatus>>

    @POST("goods_driver_todays_earnings")
    suspend fun getDriverTodaysEarnings(
        @Query("driver_id") driverId: String
    ): Response<ApiResponse<EarningsResponse>>

    @POST("update_goods_drivers_current_location")
    suspend fun updateDriverLocation(
        @Query("goods_driver_id") driverId: String,
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): Response<ApiResponse<Any>>

    @POST("add_new_active_goods_driver")
    suspend fun addToActiveDrivers(
        @Body request: ActiveDriverRequest
    ): Response<ApiResponse<Any>>

    @POST("delete_active_goods_driver")
    suspend fun deleteFromActiveDrivers(
        @Query("goods_driver_id") driverId: String
    ): Response<ApiResponse<Any>>

    @POST("get_goods_driver_current_recharge_details")
    suspend fun getRechargeDetails(
        @Query("driver_id") driverId: String
    ): Response<ApiResponse<RechargeDetails>>

    @Multipart
    @POST("upload_image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody
    ): Response<ImageUploadResponse>

    @POST("update_firebase_token")
    suspend fun updateFirebaseToken(
        @Query("goods_driver_id") driverId: String,
        @Query("authToken") authToken: String
    ): Response<ApiResponse<Any>>

    @POST("update_firebase_goods_driver_token")
    suspend fun updateFirebaseGoodsDriverToken(
        @Query("goods_driver_id") driverId: String,
        @Query("authToken") authToken: String
    ): Response<ApiResponse<Any>>

    @POST("get_current_booking_details")
    suspend fun getCurrentBookingDetails(
        @Query("booking_id") bookingId: String
    ): Response<ApiResponse<BookingDetails>>

    @POST("all_vehicles")
    suspend fun getVehicles(
        @Query("category_id") categoryId: Int
    ): Response<VehicleResponse>

    @GET("trips/{tripId}")
    suspend fun getTripDetails(
        @Path("tripId") tripId: String
    ): Response<TripDetails>

    @POST("trips/{tripId}/status")
    suspend fun updateTripStatus(
        @Path("tripId") tripId: String,
        @Query("driver_id") driverId: String,
        @Query("status") status: String
    ): Response<Any>

    @POST("trips/{tripId}/verify-otp")
    suspend fun verifyTripOtp(
        @Path("tripId") tripId: String,
        @Query("driver_id") driverId: String,
        @Query("otp") otp: String
    ): Response<Any>

    @POST("trips/{tripId}/payment")
    suspend fun updateTripPayment(
        @Path("tripId") tripId: String,
        @Query("driver_id") driverId: String,
        @Query("payment_method") paymentMethod: String
    ): Response<Any>
}
