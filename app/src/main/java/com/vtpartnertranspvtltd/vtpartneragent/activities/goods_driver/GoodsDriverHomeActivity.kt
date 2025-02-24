package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver


import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.BuildConfig
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NoConnectionError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.activities.LoginActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings.GoodsDriverEarningsActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings.GoodsDriverRatingsActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings.GoodsDriverRechargeHistoryActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings.GoodsDriverRechargeHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings.GoodsDriverRidesActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings.NewLiveRideActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverHomeBinding

import com.vtpartnertranspvtltd.vtpartneragent.network.VolleyFileUploadRequest
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleyMultipartRequest
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.services.AccessToken
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class GoodsDriverHomeActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityGoodsDriverHomeBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocationMarker: Marker? = null
    private var isMapReady = false
    private var backPressedTime: Long = 0
    private lateinit var drawerLayout: DrawerLayout
    private var isOnline = false
    private var selfieUri: Uri? = null
    private val cameraPermissionRequest = 1005
    private var isVerified = false
    private var verifiedStatus = ""
    private var isLoading = false
    private lateinit var preferenceManager:PreferenceManager
    private var limitExceededBalance = 0.0
    private var isExpired = false
    private var showLiveRideDetails = false
    private var currentBalance = "0"
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val handler = Handler(Looper.getMainLooper())
    private var previousSelfie: String? = null

    private val locationRequest = LocationRequest.create().apply {
        priority = Priority.PRIORITY_HIGH_ACCURACY
        interval = 5000 // Update location every 5 seconds
        fastestInterval = 3000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager.getInstance(this)
        setupToolbar()
        checkDriverStatus()
        updateDriverAuthToken()

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        setupNavigationDrawer()

        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupNavigationView() {
        // Get the header view
        val headerView = binding.navigationView.getHeaderView(0)

        // Initialize views from header
        val profileImage: CircleImageView = headerView.findViewById(R.id.profileImageNav)
        val nameText: TextView = headerView.findViewById(R.id.nameText)
        val emailText: TextView = headerView.findViewById(R.id.emailText)
        val editProfileBtn: TextView = headerView.findViewById(R.id.edit_profile)

        editProfileBtn.setOnClickListener {
            startActivity(Intent(this,GoodsDriverEditProfileActivity::class.java))
        }


        // Set user data
        with(preferenceManager) {
            // Set name
            nameText.text = getStringValue("goods_driver_name")

            // Set mobile number as email/contact
            emailText.text = getStringValue("goods_driver_mobile_no")

            // Load profile image
            val profilePicUrl = getStringValue("profile_pic")
            println("profilePicUrl::$profilePicUrl")
            if (profilePicUrl.isNotEmpty()) {
                Glide.with(this@GoodsDriverHomeActivity)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.demo_user)
                    .error(R.drawable.demo_user)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(profileImage)
            }
        }

        // Optional: Add click listener to header
        headerView.setOnClickListener {
            // Navigate to profile or handle click
            // For example:
            // startActivity(Intent(this, ProfileActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun checkDriverStatus() {
        val driverId = preferenceManager.getStringValue("goods_driver_id")
        if (driverId.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }

        val url = "${Constants.BASE_URL}goods_driver_online_status"

        // Create JSON body
        val jsonBody = JSONObject().apply {
            put("goods_driver_id", driverId)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    val results = response.getJSONArray("results")
                    if (results.length() > 0) {
                        val ret = results.getJSONObject(0)

                        val isOnlineStr = ret.getString("is_online")
                        val profilePic = ret.getString("profile_pic")
                        val mobileNo = ret.getString("mobile_no")
                        val status = ret.getString("status")
                        val recentOnlinePic = ret.getString("recent_online_pic")
                        val driverName = ret.getString("driver_first_name")

                        preferenceManager.saveStringValue("recent_online_pic", recentOnlinePic)

                        runOnUiThread {
                            isOnline = isOnlineStr == "1"
                            updateOnlineStatus(isOnline)
                            if (isOnline) {
                                checkLocationPermissionAndStartUpdates()
                            } else {
                                stopLocationUpdates()
                            }

                            when (status) {
                                "0" -> {
                                    isVerified = false
                                    verifiedStatus = "You are not yet verified"
                                    showExpiryAlert(true)
                                }
                                "2" -> {
                                    isVerified = false
                                    verifiedStatus = "You are blocked"
                                    showExpiryAlert(true)
                                }
                                "3" -> {
                                    isVerified = false
                                    verifiedStatus = "You are rejected"
                                    showExpiryAlert(true)
                                }
                                else -> {
                                    isVerified = true
                                    showExpiryAlert(false)
                                }
                            }

                            binding.toolbar.onlineSwitch.isChecked = isOnline
                            updateDriverInfo(profilePic, 0, 0.0, 0.0)
                        }

                        getDriverEarnings()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Error processing response")
                }
            },
            { error ->
                error.printStackTrace()
                when (error) {
                    is NoConnectionError -> showError("No internet connection")
                    else -> showError("Server error occurred")
                }
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

    private fun getDriverEarnings() {
        val driverId = preferenceManager.getStringValue("goods_driver_id")
        val url = "${Constants.BASE_URL}goods_driver_todays_earnings"

        val jsonBody = JSONObject().apply {
            put("driver_id", driverId)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    val results = response.getJSONArray("results")
                    if (results.length() > 0) {
                        val ret = results.getJSONObject(0)
                        val todaysEarnings = ret.getDouble("todays_earnings")
                        val todaysRides = ret.getInt("todays_rides")


                        runOnUiThread {
                            updateDriverInfo(
                                binding.driverInfo.profileImage.tag as? String ?: "",
                                todaysRides,
                                todaysEarnings,
                                0.0
                            )
                        }

                        fetchCurrentBalance()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Error processing earnings data")
                }
            },
            { error ->
                error.printStackTrace()
                showError("Error fetching earnings")
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

    private fun fetchCurrentBalance() {
        val driverId = preferenceManager.getStringValue("goods_driver_id")
        val url = "${Constants.BASE_URL}get_goods_driver_current_recharge_details"

        val jsonBody = JSONObject().apply {
            put("driver_id", driverId)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    val results = response.getJSONArray("results")
                    if (results.length() > 0) {
                        val ret = results.getJSONObject(0)
                        var hasNegativePoints = false

                        currentBalance = ret.getString("remaining_points")
                        val negativePoints = ret.getInt("negative_points")

                        if (negativePoints > 500) {
                            currentBalance = negativePoints.toString()
                            limitExceededBalance = currentBalance.toDouble()
                            hasNegativePoints = true
                            currentBalance = "-$currentBalance"

                            if (!showLiveRideDetails) {
                                showExpiryAlert(true)
                                showError("To continue receiving ride requests, please ensure your account is recharged.\nThank you for your prompt attention.")
                            }
                        }

                        // Handle validity date
                        val validTillDateStr = ret.getString("valid_till_date")
                        val validTillDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(validTillDateStr)

                        val currentDate = Calendar.getInstance().time
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        val formattedValidTillDate = sdf.format(validTillDate)
                        val formattedCurrentDate = sdf.format(currentDate)

                        val parsedValidTillDate = sdf.parse(formattedValidTillDate)
                        val parsedCurrentDate = sdf.parse(formattedCurrentDate)

                        if (parsedValidTillDate?.before(parsedCurrentDate) == true ||
                            parsedValidTillDate?.equals(parsedCurrentDate) == true) {
                            Log.d("Balance", "Valid till date is valid, no action needed.")
                        } else {
                            if (hasNegativePoints) {
                                currentBalance = "-$limitExceededBalance"
                                isExpired = true
                                showError("Your previous plan has expired. Please recharge promptly to continue receiving ride requests.")
                                showExpiryAlert(true)
                            }
                        }

                        runOnUiThread {
                            updateBalanceUI()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    handleError(e)
                }
            },
            { error ->
                error.printStackTrace()
                handleError(error)
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

    private fun updateDriverAuthToken() {
        Log.d("FCMAuthTokenUpdating", "updating goods driver authToken")



        // Get device token
        val deviceToken = preferenceManager.getStringValue("goods_drive_device_token")
        val driverId = preferenceManager.getStringValue("goods_driver_id")

        if (deviceToken.isNullOrEmpty()) {
            // Launch coroutine to get new token
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val newToken = FirebaseMessaging.getInstance().token.await()
                    println("newTokenFCM::$newToken")
                    preferenceManager.saveStringValue("goods_drive_device_token", newToken)
                    // Retry with new token
                    updateDriverAuthToken()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        // Get server key token
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverToken = AccessToken.getAccessToken()
                println("serverToken::$serverToken")
                val url = "${Constants.BASE_URL}update_firebase_goods_driver_token"
                val newToken = FirebaseMessaging.getInstance().token.await()
                val jsonBody = JSONObject().apply {
                    put("goods_driver_id", driverId)
                    put("authToken", newToken)
                }

                val request = object : JsonObjectRequest(
                    Method.POST,
                    url,
                    jsonBody,
                    { response ->
                        val message = response.optString("message")
                        Log.d("Auth", "Token update response: $message")
                    },
                    { error ->
                        error.printStackTrace()
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        return HashMap<String, String>().apply {
                            put("Content-Type", "application/json")
                            put("Authorization", "Bearer $serverToken")
                        }
                    }
                }

                VolleySingleton.getInstance(this@GoodsDriverHomeActivity)
                    .addToRequestQueue(request)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun driverIsOnlineNow() {
        try {
            val position = withContext(Dispatchers.IO) {
                if (ActivityCompat.checkSelfPermission(
                        this@GoodsDriverHomeActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@GoodsDriverHomeActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    //return
                }
                LocationServices.getFusedLocationProviderClient(this@GoodsDriverHomeActivity)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .await()
            }


            val latitude = position.latitude
            val longitude = position.longitude

            Log.d("Driver", "driver lat: $latitude")
            Log.d("Driver", "driver lng: $longitude")

            updateDriverStatus(latitude, longitude)
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Could not get current location")
        }
    }

    private fun updateDriverStatus(latitude: Double, longitude: Double) {
        val driverId = preferenceManager.getStringValue("goods_driver_id")
        val recentOnlinePic = preferenceManager.getStringValue("recent_online_pic")
        val status = if (isOnline) 1 else 0

        val url = "${Constants.BASE_URL}goods_driver_update_online_status"

        val jsonBody = JSONObject().apply {
            put("goods_driver_id", driverId)
            put("status", status)
            put("lat", latitude)
            put("lng", longitude)
            put("recent_online_pic", recentOnlinePic)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { response ->
                if (status == 1) {
                    addToActiveDriverTable(latitude, longitude)
                }
            },
            { error ->
                error.printStackTrace()
                showError("Something went wrong")
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

    private fun addToActiveDriverTable(latitude: Double, longitude: Double) {
        val driverId = preferenceManager.getStringValue("goods_driver_id")
        val status = if (isOnline) 1 else 0

        val url = "${Constants.BASE_URL}add_new_active_goods_driver"

        val jsonBody = JSONObject().apply {
            put("goods_driver_id", driverId)
            put("status", status)
            put("current_lat", latitude)
            put("current_lng", longitude)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { _ ->
                showError("You are Online now")
                updateOnlineStatus(true)
            },
            { error ->
                error.printStackTrace()
                showError("Something went wrong")
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

    private fun deleteFromActiveDriverTable() {
        val driverId = preferenceManager.getStringValue("goods_driver_id")

        val url = "${Constants.BASE_URL}delete_active_goods_driver"

        val jsonBody = JSONObject().apply {
            put("goods_driver_id", driverId)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { _ ->
                showError("You are offline now")
                isOnline = false
                updateOnlineStatus(false)
                Handler(Looper.getMainLooper()).postDelayed({
                    stopLocationUpdates()
                }, 1000)
            },
            { error ->
                error.printStackTrace()
                showError("Something went wrong")
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

    private fun driverIsOfflineNow() {
        val currentBookingId = preferenceManager.getStringValue("current_booking_id_assigned")

//        if (!currentBookingId.isNullOrEmpty()) {
//            showError("You can't go Offline during live order")
//            isOnline = true
//            return
//        }

        preferenceManager.saveStringValue("recent_online_pic", "")
        previousSelfie = null
        deleteFromActiveDriverTable()
    }


    private fun updateBalanceUI() {
        // Update your UI elements with the current balance
        binding.driverInfo.currentBalance.text = "Current Balance: ₹$currentBalance"

        if (isExpired || currentBalance.startsWith("-")) {
            showExpiryAlert(true)
        } else {
            showExpiryAlert(false)
        }
    }

    private fun handleError(error: Exception) {
        when {
            error.message?.contains("No Data Found") == true -> {
                showError("Not Yet Subscribed to any Top-Up Recharge plan.")
            }
            error is NoConnectionError -> {
                showError("No internet connection")
            }
            else -> {
                Log.e("Balance", "Error fetching balance", error)
            }
        }
    }

    // Replace updateDriverInfo method
    private fun updateDriverInfo(
        profilePic: String,
        todaysRides: Int,
        todaysEarnings: Double,
        currentBalance: Double
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            // Load image in background
            val glideRequest = Glide.with(this@GoodsDriverHomeActivity)
                .load(profilePic)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)

            // Update UI on main thread
            withContext(Dispatchers.Main) {
                binding.driverInfo.profileImage.tag = profilePic
                glideRequest.into(binding.driverInfo.profileImage)
                binding.driverInfo.ridesAndEarnings.text =
                    "$todaysRides Rides | ₹${todaysEarnings.roundToInt()}"
                binding.driverInfo.currentBalance.text =
                    "Current Balance: ₹${currentBalance.roundToInt()}"
            }
        }
    }

    // Add cleanup in onDestroy
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun showExpiryAlert(show: Boolean) {
        binding.expiryLayout.root.visibility = if (show) View.VISIBLE else View.GONE

        if (show) {
            binding.expiryLayout.title.text = verifiedStatus
            binding.expiryLayout.root.setOnClickListener {
//                startActivity(Intent(this, GoodsDriverRechargeActivity::class.java))
            }
        }
    }



    private fun setupToolbar() {
        // Setup click listeners
        binding.toolbar.onlineStatusCard.setOnClickListener {
            if (isOnline) {
                showGoOfflineDialog()
            } else {
                showGoOnlineDialog()
            }
        }

        binding.toolbar.onlineSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isOnline) {
                if (isChecked) {
                    showGoOnlineDialog()
                } else {
                    showGoOfflineDialog()
                }
            }
        }
    }


    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selfieUri?.let { uri ->
                uploadSelfie(uri)
            }
        } else {
            binding.toolbar.onlineSwitch.isChecked = false
            showError("Selfie is required to go online")
        }
    }

    private fun showGoOnlineDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Go Online")
            .setMessage("A selfie is required before going online. Would you like to take a selfie now?")
            .setPositiveButton("Take Selfie") { dialog, _ ->
                dialog.dismiss()
                checkCameraPermission()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                binding.toolbar.onlineSwitch.isChecked = false
            }
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraPermissionRationaleDialog()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun showCameraPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Camera Permission Required")
            .setMessage("A selfie is required to verify your identity before going online. Please allow camera access.")
            .setPositiveButton("Grant Permission") { dialog, _ ->
                dialog.dismiss()
                requestCameraPermission()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                binding.toolbar.onlineSwitch.isChecked = false
            }
            .show()
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            cameraPermissionRequest
        )
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            selfieUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            cameraLauncher.launch(selfieUri)
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Could not open camera")
            binding.toolbar.onlineSwitch.isChecked = false
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "RECENT_ONLINE_SELFIE_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun uploadSelfie(uri: Uri) {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Uploading Selfie")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        println("selfie_uri:$uri")
        val url = "${Constants.IMAGE_SERVER_URL}/upload"  // Match your Django URL
        println("image_endpoint::$url")
        try {
            val request = object : VolleyFileUploadRequest(
                Method.POST,
                url,
                { response ->
                    try {
                        val jsonResponse = JSONObject(String(response.data))
                        val imageUrl = jsonResponse.getString("image_url")
                        Log.d("Upload", "Image URL: $imageUrl")

                        // Save URL
                        preferenceManager.saveStringValue("recent_online_pic", imageUrl)
                        previousSelfie = imageUrl

                        runOnUiThread {
                            loadingDialog.dismiss()
                            showError("Selfie image uploaded successfully")

                            // Update driver status
                            CoroutineScope(Dispatchers.Main).launch {
                                driverIsOnlineNow()
                            }
                            checkLocationPermissionAndStartUpdates()

                            isOnline = true
                            binding.toolbar.onlineSwitch.isChecked = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread {
                            loadingDialog.dismiss()
                            showError("Failed to process response")
                            isOnline = false
                            binding.toolbar.onlineSwitch.isChecked = false
                        }
                    }
                },
                { error ->
                    error.printStackTrace()
                    runOnUiThread {
                        loadingDialog.dismiss()
                        println("FailedSelfieUpload::${error.message}")
                        showError("Failed to upload image")
                        isOnline = false
                        binding.toolbar.onlineSwitch.isChecked = false
                    }
                }
            ) {
                override fun getByteData(): Map<String, DataPart> {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: ByteArray(0)
                    inputStream?.close()
val goods_driver_name = preferenceManager.getStringValue("goods_driver_name")
                    val goods_driver_id = preferenceManager.getStringValue("goods_driver_id")
                    return mapOf(
                        "image" to DataPart(
                            "goods_driver_id_${goods_driver_id}_${goods_driver_name}_selfie.jpg",
                            bytes,
                            "image/jpeg"
                        )
                    )
                }
            }

            // Add to request queue
            VolleySingleton.getInstance(this).addToRequestQueue(request)

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                loadingDialog.dismiss()
                showError("Error preparing image")
                isOnline = false
                binding.toolbar.onlineSwitch.isChecked = false
            }
        }
    }




    private fun showGoOfflineDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Go Offline")
            .setMessage("Are you sure you want to go offline? You won't receive any new trips.")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                driverIsOfflineNow()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateOnlineStatus(online: Boolean) {
        isOnline = online

        // Animate status change
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                binding.toolbar.onlineStatusCard.alpha = 1f - value
            }
            doOnEnd {
                // Update UI elements
                binding.toolbar.statusDot.setBackgroundResource(
                    if (online) R.drawable.status_dot_online
                    else R.drawable.status_dot_offline
                )
                binding.toolbar.statusText.text = if (online) "Online" else "Offline"
                binding.toolbar.onlineSwitch.isChecked = online

                // Animate back in
                binding.toolbar.onlineStatusCard.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        }
        animator.start()

        // Update your backend/database here
        updateDriverStatus(online)
    }

    private fun updateDriverStatus(online: Boolean) {
        // Call your API to update driver's status
        // Example:
        // viewModel.updateDriverStatus(online)
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationOnMap(location)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        isMapReady = true
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        checkLocationPermissionAndStartUpdates()
    }

    private fun setupNavigationDrawer() {
        drawerLayout = binding.drawerLayout

        // Setup menu button click
        binding.toolbar.menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Setup navigation view
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, GoodsDriverHomeActivity::class.java))
                    finishAffinity()
                }
                R.id.nav_trips -> {
                    startActivity(Intent(this, NewLiveRideActivity::class.java))
                }
                R.id.nav_earnings -> {
                    startActivity(Intent(this, GoodsDriverEarningsActivity::class.java))
                }
                R.id.nav_my_ratings -> {
                    startActivity(Intent(this, GoodsDriverRatingsActivity::class.java))
                }
                R.id.nav_my_rides -> {
                    startActivity(Intent(this, GoodsDriverRidesActivity::class.java))
                }
                R.id.nav_recharge_home -> {
                    startActivity(Intent(this, GoodsDriverRechargeHomeActivity::class.java))
                }
                R.id.nav_recharge_history -> {
                    startActivity(Intent(this, GoodsDriverRechargeHistoryActivity::class.java))
                }
                R.id.nav_account_delete -> {
                    handleAccountDeletionRequest()
                }
                R.id.nav_logout -> {
                    if(isOnline) {
                        showSnackbar("You can't log out without going Offline")
                    }else {
                        showLogoutConfirmationDialog()
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Update header with user info
        val headerView = binding.navigationView.getHeaderView(0)

        val profilePicImage = headerView.findViewById<ImageView>(R.id.profileImageNav)
        with(preferenceManager){
            val profilePicUrl = getStringValue("profile_pic")
            println("profilePicUrl::$profilePicUrl")
            if (profilePicUrl.isNotEmpty()) {
                Glide.with(this@GoodsDriverHomeActivity)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.demo_user)
                    .error(R.drawable.demo_user)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(profilePicImage)
            }

            val driverName = getStringValue("goods_driver_name")
            val driverMobileNo = getStringValue("goods_driver_mobile_no")

            headerView.findViewById<TextView>(R.id.nameText).text = driverName
            headerView.findViewById<TextView>(R.id.emailText).text = driverMobileNo

        }

        val editProfileBtn: ImageView = headerView.findViewById(R.id.edit_profile)

        editProfileBtn.setOnClickListener {
            startActivity(Intent(this,GoodsDriverEditProfileActivity::class.java))
        }
    }

    private fun handleAccountDeletionRequest() {
        // Check if request already sent
        if (preferenceManager.getBooleanValue("account_deletion_requested") == true) {
            showAlreadyRequestedDialog()
            return
        }

        // Show confirmation dialog
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Delete Account")
            .setView(createCustomDialogView())
            .setPositiveButton("Yes") { dialog, _ ->
                preferenceManager.saveBooleanValue("account_deletion_requested", true)
                showSuccessSnackbar("Account deletion request sent successfully")
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun createCustomDialogView(): View {
        return LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null).apply {
            // You can customize the view here if needed
        }
    }

    private fun showAlreadyRequestedDialog() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Request Already Sent")
            .setMessage("Your account deletion request has already been submitted. Our team will process it soon.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccessSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.green))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                preferenceManager.saveStringValue("goods_driver_id","")
                preferenceManager.saveStringValue("goods_driver_name","")
                startActivity(Intent(this@GoodsDriverHomeActivity,LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    protected fun showSnackbar(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

//    override fun onBackPressed() {
//        if (backPressedTime + 2000 > System.currentTimeMillis()) {
//            super.onBackPressed()
//        } else {
//            showSnackbar("Press back once again to exit")
//            backPressedTime = System.currentTimeMillis()
//        }
//    }


    private fun shouldShowRequestPermissionRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationUpdates() {
        try {
            if (hasLocationPermission() && isMapReady) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                map.isMyLocationEnabled = true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateLocationOnMap(location: Location) {
        if (!isMapReady) return  // Add early return if map isn't ready
        val currentLatLng = LatLng(location.latitude, location.longitude)

        // Update or create marker
        if (currentLocationMarker == null) {
            val markerOptions = MarkerOptions()
                .position(currentLatLng)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_location))
            currentLocationMarker = map.addMarker(markerOptions)

            // Move camera to current location first time
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        } else {
            currentLocationMarker?.position = currentLatLng
        }
    }



    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()  && isMapReady) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }



    private fun checkLocationPermissionAndStartUpdates() {
        when {
            hasAllLocationPermissions() -> startLocationUpdates()
            shouldShowAnyPermissionRationale() -> showPermissionRationaleDialog()
            else -> requestLocationPermissions()
        }
    }

    private fun hasAllLocationPermissions(): Boolean {
        return hasLocationPermission() && hasBackgroundLocationPermission()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun shouldShowAnyPermissionRationale(): Boolean {
        return shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }

    private fun requestLocationPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permission to track your position while delivering. " +
                    "Background location access is required to track your location even when the app is in background.")
            .setPositiveButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                requestLocationPermissions()
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionRequiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Location Permission Required")
            .setMessage("Location permissions are required for this app to function. Please enable them in settings.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showBackgroundLocationRequiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Background Location Required")
            .setMessage("This app needs background location access to track your position while delivering. " +
                    "Please select 'Allow all the time' in the next screen.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                ) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        requestBackgroundLocationPermission()
                    } else {
                        startLocationUpdates()
                    }
                } else {
                    showPermissionRequiredDialog()
                }
            }
            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    startLocationUpdates()
                } else {
                    showBackgroundLocationRequiredDialog()
                }
            }
            cameraPermissionRequest -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        // Permission denied permanently
                        showPermissionDeniedPermanentlyDialog()
                    } else {
                        binding.toolbar.onlineSwitch.isChecked = false
                        showError("Camera permission is required to go online")
                    }
                }
            }
        }
    }

    private fun showPermissionDeniedPermanentlyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage("Camera permission is required to take selfie verification. Please enable it in settings.")
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                binding.toolbar.onlineSwitch.isChecked = false
            }
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestBackgroundLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
    }
}