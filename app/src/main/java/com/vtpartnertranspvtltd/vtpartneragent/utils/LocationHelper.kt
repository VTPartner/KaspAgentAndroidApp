package com.vtpartnertranspvtltd.vt_partner.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LocationDetails(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val pincode:String?
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    suspend fun getCurrentLocation(): Result<LocationDetails> = suspendCancellableCoroutine { continuation ->
        try {
            if (!hasLocationPermission()) {
                continuation.resume(Result.failure(Exception("Location permission not granted")))
                return@suspendCancellableCoroutine
            }

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(listener: OnTokenCanceledListener) = CancellationTokenSource().token
                    override fun isCancellationRequested() = false
                }
            ).addOnSuccessListener { location: Location? ->
                if (location == null) {
                    continuation.resume(Result.failure(Exception("Unable to get location")))
                    return@addOnSuccessListener
                }

                val address = getAddressFromLocation(location.latitude, location.longitude)
                continuation.resume(
                    Result.success(
                        LocationDetails(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = address ?: "Address not found",
pincode = ""
                        )
                    )
                )
            }.addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildString {
                    // Get the most detailed address possible
                    if (address.maxAddressLineIndex >= 0) {
                        append(address.getAddressLine(0))
                    } else {
                        // Fallback to building address from components
                        address.subLocality?.let { append(it).append(", ") }
                        address.locality?.let { append(it).append(", ") }
                        address.subAdminArea?.let { append(it).append(", ") }
                        address.adminArea?.let { append(it) }
                    }
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 