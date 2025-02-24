package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.main_documents



import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vt_partner.utils.Constants

import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.owner_documents.GoodsDriverVehicleOwnerSelfieUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverVehicleOwnerDetailsBinding
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton

import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject

class GoodsDriverVehicleOwnerDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverVehicleOwnerDetailsBinding
    private lateinit var preferenceManager: PreferenceManager
    private var isLoading = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverVehicleOwnerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        setupUI()
        loadDefaultValues()
    }

    private fun setupUI() {
        binding.apply {
            backButton.setOnClickListener { finish() }
            submitButton.setOnClickListener { saveOwnerDetails() }
            ownerPhotoCard.setOnClickListener {
                startActivity(Intent(this@GoodsDriverVehicleOwnerDetailsActivity,
                    GoodsDriverVehicleOwnerSelfieUploadActivity::class.java))
            }
        }
    }

    private fun loadDefaultValues() {
        with(preferenceManager) {
            binding.apply {
                ownerNameInput.setText(getStringValue("owner_name"))
                ownerAddressInput.setText(getStringValue("owner_address"))
                ownerCityInput.setText(getStringValue("owner_city_name"))
                ownerPhoneInput.setText(getStringValue("owner_mobile_no"))
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionDialog()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun showLocationPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permission to get your current location for registration.")
            .setPositiveButton("Grant") { _, _ ->
                requestLocationPermission()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showToast("Location permission is required for registration")
            }
            .show()
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                } else {
                    requestNewLocation()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting location", e)
                showToast("Failed to get location")
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    currentLatitude = location.latitude
                }
                if (location != null) {
                    currentLongitude = location.longitude
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation()
                } else {
                    showToast("Location permission denied")
                }
            }
        }
    }

    private fun saveOwnerDetails() {
        val ownerName = binding.ownerNameInput.text.toString().trim()
        val ownerAddress = binding.ownerAddressInput.text.toString().trim()
        val ownerCity = binding.ownerCityInput.text.toString().trim()
        val ownerPhone = binding.ownerPhoneInput.text.toString().trim()
        val ownerPhotoUrl = preferenceManager.getStringValue("owner_selfie_photo_url")

        when {
            ownerName.isEmpty() -> {
                showToast("Please provide owner full name")
                return
            }
            ownerAddress.isEmpty() -> {
                showToast("Please provide owner full address")
                return
            }
            ownerCity.isEmpty() -> {
                showToast("Please provide owner City Name")
                return
            }
            ownerPhone.isEmpty() -> {
                showToast("Please provide owner mobile number")
                return
            }
            ownerPhone.length != 10 -> {
                showToast("Please provide valid 10 digits owner mobile number without country code")
                return
            }
            ownerPhotoUrl.isEmpty() -> {
                showToast("Please upload Owner Photo")
                return
            }
        }

        // Save owner details
        with(preferenceManager) {
            saveStringValue("owner_name", ownerName)
            saveStringValue("owner_address", ownerAddress)
            saveStringValue("owner_city_name", ownerCity)
            saveStringValue("owner_mobile_no", ownerPhone)
        }

        registerDriver()
    }

    private fun registerDriver() {
        isLoading = true
        updateLoadingState()

        val requestData = JSONObject().apply {

        }

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}goods_driver_registration",
            requestData,
            { response ->
                isLoading = false
                updateLoadingState()

                if (response.has("message")) {
                    with(preferenceManager) {
                        // Driver Basic Info
                        saveStringValue("goods_driver_id", getStringValue("goods_driver_id"))
                        saveStringValue("goods_driver_name", getStringValue("driver_name"))
                        saveStringValue("profile_pic", getStringValue("selfie_photo_url"))
                        saveStringValue("mobile_no", getStringValue("goods_driver_mobno"))

                        // Location Info
                        saveStringValue("r_lat", currentLatitude.toString())
                        saveStringValue("r_lng", currentLongitude.toString())
                        saveStringValue("current_lat", currentLatitude.toString())
                        saveStringValue("current_lng", currentLongitude.toString())

                        // Recent Online Info
                        saveStringValue("recent_online_pic", getStringValue("selfie_photo_url"))

                        // Vehicle and City Info
                        saveStringValue("vehicle_id", getStringValue("driver_vehicle_id"))
                        saveStringValue("city_id", getStringValue("driver_city_id"))

                        // Personal Documents
                        saveStringValue("aadhar_no", getStringValue("aadhar_no"))
                        saveStringValue("pan_card_no", getStringValue("pan_no"))
                        saveStringValue("full_address", getStringValue("driver_address"))
                        saveStringValue("gender", getStringValue("driver_gender"))

                        // Document Images
                        saveStringValue("aadhar_card_front", getStringValue("aadhar_front_photo_url"))
                        saveStringValue("aadhar_card_back", getStringValue("aadhar_back_photo_url"))
                        saveStringValue("pan_card_front", getStringValue("pan_front_photo_url"))
                        saveStringValue("pan_card_back", getStringValue("pan_back_photo_url"))
                        saveStringValue("license_front", getStringValue("license_front_photo_url"))
                        saveStringValue("license_back", getStringValue("license_back_photo_url"))

                        // Vehicle Documents
                        saveStringValue("insurance_image", getStringValue("insurance_photo_url"))
                        saveStringValue("noc_image", getStringValue("noc_photo_url"))
                        saveStringValue("pollution_certificate_image", getStringValue("puc_photo_url"))
                        saveStringValue("rc_image", getStringValue("rc_photo_url"))
                        saveStringValue("vehicle_image", getStringValue("vehicle_front_photo_url"))
                        saveStringValue("vehicle_plate_image", getStringValue("vehicle_plate_front_photo_url"))

                        // Document Numbers
                        saveStringValue("driving_license_no", getStringValue("license_no"))
                        saveStringValue("vehicle_plate_no", getStringValue("driver_vehicle_no"))
                        saveStringValue("rc_no", getStringValue("rc_no"))
                        saveStringValue("insurance_no", getStringValue("insurance_no"))
                        saveStringValue("noc_no", getStringValue("noc_no"))
                        saveStringValue("vehicle_fuel_type", getStringValue("driver_vehicle_fuel_type"))

                        // Owner Details
                        saveStringValue("owner_name", getStringValue("owner_name"))
                        saveStringValue("owner_mobile_no", getStringValue("owner_mobile_no"))
                        saveStringValue("owner_photo_url", getStringValue("owner_selfie_photo_url"))
                        saveStringValue("owner_address", getStringValue("owner_address"))
                        saveStringValue("owner_city_name", getStringValue("owner_city_name"))
                    }
                    startActivity(Intent(this, GoodsDriverHomeActivity::class.java))
                    finishAffinity()
                }
            },
            { error ->
                Log.e(TAG, "Registration failed", error)
                isLoading = false
                updateLoadingState()
                showToast("Registration failed")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return HashMap<String, String>().apply {
                    put("Content-Type", "application/json")
                }
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun updateLoadingState() {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            submitButton.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "GoodsDriverOwnerDetails"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}