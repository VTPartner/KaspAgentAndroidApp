package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.maps.android.PolyUtil
import com.ncorti.slidetoact.SlideToActView
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityNewLiveRideBinding
import com.vtpartnertranspvtltd.vtpartneragent.databinding.DailogPaymentDetailsBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.RideDetailsModel
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.services.AccessToken
import com.vtpartnertranspvtltd.vtpartneragent.services.LocationUpdateService
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Math.abs
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

class NewLiveRideActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNewLiveRideBinding
    private lateinit var map: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationReceiver: BroadcastReceiver? = null
    private var currentBookingId = 0
    private var isLoading = false
    private lateinit var progressDialog: MaterialAlertDialogBuilder
    private var rideDetails = RideDetailsModel()
    private lateinit var preferenceManager: PreferenceManager

    // Add these properties to your class
    private var currentPolyline: Polyline? = null
    private var currentMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var dropMarker: Marker? = null

    private var locationUpdateService: LocationUpdateService? = null
    private var bound = false
    // Add service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationUpdateService.LocalBinder
            locationUpdateService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationUpdateService = null
            bound = false
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                updateDriverLocation(location)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewLiveRideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.bottomSheet.isVisible = false
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        preferenceManager = PreferenceManager.getInstance(this)
        initializeLocationReceiver()
        setupNavigationButton()
        setupBottomSheet()
        setupToggleButton()
        setupSlideButton()
        setupProgressDialog()
        getCurrentBookingIdDetails()
//        startLocationService()
    }

    private fun initializeLocationReceiver() {
        locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "LOCATION_UPDATE") {
                    var latitude = intent.getDoubleExtra("latitude", 0.0)
                    var longitude = intent.getDoubleExtra("longitude", 0.0)

                    // Move heavy work off the main thread
                    lifecycleScope.launch(Dispatchers.Default) {
                        val currentLocation = Location("").apply {
                            this.latitude = latitude
                            this.longitude = longitude
                        }

                        // Update driver's position in backend
                        updateDriversCurrentPosition(latitude, longitude)

                        // Get destination based on status
                        val destination = if (rideDetails.bookingStatus == "Start Trip") {
                            Location("").apply {
                                latitude = rideDetails.destinationLat
                                longitude = rideDetails.destinationLng
                            }
                        } else {
                            Location("").apply {
                                latitude = rideDetails.pickupLat
                                longitude = rideDetails.pickupLng
                            }
                        }

                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            updateDriverLocation(currentLocation)
                        }
                    }
                }
            }
        }
    }

    private fun setupNavigationButton() {
        binding.navigationFab.setOnClickListener {
            navigateToDestination()
        }
    }

    private fun navigateToDestination() {
        val destination = if (rideDetails.bookingStatus == "Start Trip") {
            LatLng(rideDetails.destinationLat, rideDetails.destinationLng)
        } else {
            LatLng(rideDetails.pickupLat, rideDetails.pickupLng)
        }

        // Open Google Maps navigation
        val uri = "google.navigation:q=${destination.latitude},${destination.longitude}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }

    private fun setupProgressDialog() {
        progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Loading")
            .setMessage("Please wait...")
            .setCancelable(false)
    }

    private fun showLoading() {
        isLoading = true
        progressDialog.show()
    }

    private fun hideLoading() {
        isLoading = false
        progressDialog.create().dismiss()
    }

    private fun getCurrentBookingIdDetails() {
        //showLoading()

        val driverId = preferenceManager.getStringValue("goods_driver_id")
        if (driverId.isNullOrEmpty()) {
            showError("No Live Ride Found")
            finish()
            return
        }

        val url = "${Constants.BASE_URL}get_goods_driver_current_booking_detail"

        val jsonBody = JSONObject().apply {
            put("goods_driver_id", driverId)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    val bookingId = response.optInt("current_booking_id")
                    if (bookingId != 0) {
                        currentBookingId = bookingId
                        binding.bottomSheet.isVisible = true
                        getBookingDetails()
                    } else {
                        showError("No Live Ride Found")
                        preferenceManager.saveStringValue("current_booking_id_assigned","")
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Error processing response")
                    preferenceManager.saveStringValue("current_booking_id_assigned","")
                    finish()
                }
            },
            { error ->
                error.printStackTrace()
                if (error.networkResponse?.statusCode == 404) {
                    showError("No Booking Details Found")
                    preferenceManager.saveStringValue("current_booking_id_assigned","")
                } else {
                    showError("Error fetching booking details")
                    preferenceManager.saveStringValue("current_booking_id_assigned","")
                }
                finish()
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

    private fun getBookingDetails() {
        Log.d("BookingDetailsAsync", "Getting booking details")

        if (currentBookingId == 0) {
            showError("No Live Ride Found")
            finish()
            return
        }

        val url = "${Constants.BASE_URL}booking_details_live_track"

        val jsonBody = JSONObject().apply {
            put("booking_id", currentBookingId)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            url,
            jsonBody,
            { response ->
                try {
                    val results = response.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val result = results.getJSONObject(0)

                        // Parse booking details
                        rideDetails = RideDetailsModel(
                            customerName = result.getString("customer_name"),
                            bookingId = result.getString("booking_id"),
                            customerId = result.getString("customer_id"),
                            pickupAddress = result.getString("pickup_address"),
                            dropAddress = result.getString("drop_address"),
                            distance = result.getString("distance"),
                            totalTime = result.getString("total_time"),
                            senderName = result.getString("sender_name"),
                            senderNumber = result.getString("sender_number"),
                            receiverName = result.getString("receiver_name"),
                            receiverNumber = result.getString("receiver_number"),
                            totalPrice = result.getDouble("total_price"),
                            pickupLat = result.getDouble("pickup_lat"),
                            pickupLng = result.getDouble("pickup_lng"),
                            customerNumber = result.getString("customer_mobile_no"),
                            bookingStatus = result.getString("booking_status"),
                            otp = result.getString("otp"),
                            destinationLat = result.getDouble("destination_lat"),
                            destinationLng = result.getDouble("destination_lng")
                        )
                        println("rideDetails::$rideDetails")
                        // Handle booking cancellation
                        if (rideDetails.bookingStatus == "Cancelled") {
                            preferenceManager.saveStringValue("current_booking_id_assigned", "")
                            showError("This Booking has been Cancelled")
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopLocationUpdates()
                                // Navigate to home
                                startActivity(Intent(this, GoodsDriverHomeActivity::class.java))
                                finish()
                            }, 3000)

                        }

                        // Update UI
                        updateUIWithBookingDetails()
                        startLocationUpdates()
                        hideLoading()

                    } else {
                        showError("No booking details found")
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Error processing booking details")
                    finish()
                }
            },
            { error ->
                error.printStackTrace()
                if (error.networkResponse?.statusCode == 404) {
                    showError("No Live Ride Found")
                } else {
                    showError("Error fetching booking details")
                }
                finish()
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

    private fun updateUIWithBookingDetails() {
        binding.apply {
            customerNameText.text = rideDetails.customerName
            bookingIdText.text = "Booking #${rideDetails.bookingId}"
            statusChip.text = rideDetails.bookingStatus
            pickupLocationText.text = rideDetails.pickupAddress
            dropLocationText.text = rideDetails.dropAddress
            distanceText.text = "${rideDetails.distance}"
            timeText.text = "${rideDetails.totalTime}"
            senderNameText.text = rideDetails.senderName
            senderPhoneText.text = rideDetails.senderNumber
            receiverNameText.text = rideDetails.receiverName
            receiverPhoneText.text = rideDetails.receiverNumber
            priceText.text = "₹${rideDetails.totalPrice.toDouble().roundToInt()}"

            // Update next status button text
            slideToActView.text = when (rideDetails.bookingStatus) {
                "Driver Accepted" -> "Update to Arrived Location"
                "Driver Arrived" -> "Verify OTP"
                "OTP Verified" -> "Start Trip"
                "Start Trip" -> "Send Payment Details"
                else -> "End Trip"
            }
            println("updated values here")
        }

        // Update map markers
        updateMapMarkers()
    }

    private fun updateMapMarkers() {
        if (!::map.isInitialized) return

        // Clear existing markers and polyline
        pickupMarker?.remove()
        dropMarker?.remove()
        currentPolyline?.remove()

        val pickupLatLng = LatLng(rideDetails.pickupLat, rideDetails.pickupLng)
        val dropLatLng = LatLng(rideDetails.destinationLat, rideDetails.destinationLng)

        // Add pickup marker
        pickupMarker = map.addMarker(
            MarkerOptions()
                .position(pickupLatLng)
                .title("Pickup")
                .snippet(rideDetails.pickupAddress)
                .icon(getBitmapDescriptor(R.drawable.ic_pickup_location))
        )

        // Add drop marker
        dropMarker = map.addMarker(
            MarkerOptions()
                .position(dropLatLng)
                .title("Drop")
                .snippet(rideDetails.dropAddress)
                .icon(getBitmapDescriptor(R.drawable.ic_drop_location))
        )

        // Get current driver location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
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
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let { driverLocation ->
                    val driverLatLng = LatLng(driverLocation.latitude, driverLocation.longitude)

                    // Determine destination based on booking status
                    val destination = if (rideDetails.bookingStatus == "Start Trip") {
                        dropLatLng
                    } else {
                        pickupLatLng
                    }

                    // Draw route from driver to destination
                    val url = getDirectionsUrl(driverLatLng, destination)

                    val directionsRequest = JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,
                        { response ->
                            try {
                                val routes = response.getJSONArray("routes")
                                if (routes.length() > 0) {
                                    val points = routes.getJSONObject(0)
                                        .getJSONObject("overview_polyline")
                                        .getString("points")

                                    val decodedPath = PolyUtil.decode(points)

                                    // Draw polyline
                                    currentPolyline = map.addPolyline(PolylineOptions().apply {
                                        addAll(decodedPath)
                                        width(10f)
                                        color(
                                            ContextCompat.getColor(
                                                this@NewLiveRideActivity,
                                                if (rideDetails.bookingStatus == "Start Trip")
                                                    R.color.route_to_drop_color
                                                else
                                                    R.color.route_to_pickup_color
                                            )
                                        )
                                        geodesic(true)
                                    })

                                    // Update camera to show entire route
                                    val bounds = LatLngBounds.builder().apply {
                                        include(driverLatLng)
                                        include(destination)
                                        // Include both markers for better view
                                        include(pickupLatLng)
                                        include(dropLatLng)
                                        decodedPath.forEach { include(it) }
                                    }.build()

                                    map.animateCamera(
                                        CameraUpdateFactory.newLatLngBounds(
                                            bounds,
                                            100 // padding in pixels
                                        )
                                    )

                                    // Update distance and duration
                                    val leg = routes.getJSONObject(0)
                                        .getJSONArray("legs")
                                        .getJSONObject(0)
                                    val distance = leg.getJSONObject("distance").getString("text")
                                    val duration = leg.getJSONObject("duration").getString("text")

                                    updateRouteInfo(distance, duration, destination)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showError("Error drawing route")
                            }
                        },
                        { error ->
                            error.printStackTrace()
                            showError("Error fetching route")
                        }
                    )

                    VolleySingleton.getInstance(this).addToRequestQueue(directionsRequest)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                showError("Could not get current location")
            }
    }

    private fun showRoute(driverLocation: LatLng) {
        if (!::map.isInitialized) return

        // Clear previous polylines
        currentPolyline?.remove()

        // Determine destination based on status
        val destination = when {
            rideDetails.bookingStatus == "Start Trip" -> {
                // Route to drop location
                LatLng(rideDetails.destinationLat, rideDetails.destinationLng)
            }

            else -> {
                // Route to pickup location
                LatLng(rideDetails.pickupLat, rideDetails.pickupLng)
            }
        }

        // Draw route
        val url = getDirectionsUrl(driverLocation, destination)

        val directionsRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    val routes = response.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val points = routes.getJSONObject(0)
                            .getJSONObject("overview_polyline")
                            .getString("points")

                        val decodedPath = PolyUtil.decode(points)

                        // Draw polyline
                        currentPolyline = map.addPolyline(PolylineOptions().apply {
                            addAll(decodedPath)
                            width(10f)
                            color(
                                ContextCompat.getColor(
                                    this@NewLiveRideActivity,
                                    R.color.route_color
                                )
                            )
                            geodesic(true)
                        })

                        // Update camera to show entire route
                        val bounds = LatLngBounds.builder().apply {
                            include(driverLocation)
                            include(destination)
                            decodedPath.forEach { include(it) }
                        }.build()

                        map.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                bounds,
                                100 // padding in pixels
                            )
                        )

                        // Update distance and duration
                        val leg = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0)
                        val distance = leg.getJSONObject("distance").getString("text")
                        val duration = leg.getJSONObject("duration").getString("text")

                        updateRouteInfo(distance, duration, driverLocation)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Error drawing route")
                }
            },
            { error ->
                error.printStackTrace()
                showError("Error fetching route")
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(directionsRequest)
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&mode=driving" +
                "&key=${getString(R.string.google_maps_key)}"
    }

    private fun updateRouteInfo(distance: String, duration: String, destination: LatLng) {
        binding.apply {
            val destinationType =
                if (destination == LatLng(rideDetails.destinationLat, rideDetails.destinationLng)) {
                    "to Drop"
                } else {
                    "to Pickup"
                }
            distanceTitle.text = "Distance $destinationType"
            timingTitle.text = "Time $destinationType"
            distanceText.text = "$distance"
            timeText.text = "$duration"
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                updateToggleButtonIcon(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun setupToggleButton() {
        binding.toggleBottomSheetButton.setOnClickListener {
            val currentState = bottomSheetBehavior.state
            bottomSheetBehavior.state =
                if (currentState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    BottomSheetBehavior.STATE_HALF_EXPANDED
                }
        }

        binding.callSenderButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${rideDetails.senderNumber}")
            }
            startActivity(intent)
        }

        binding.callReceiverButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${rideDetails.receiverNumber}")
            }
            startActivity(intent)
        }
    }

    private fun updateToggleButtonIcon(state: Int) {
        binding.toggleBottomSheetButton.setImageResource(
            if (state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                R.drawable.ic_expand_more
            } else {
                R.drawable.ic_expand_less
            }
        )
    }

    private fun setupSlideButton() {
        binding.slideToActView.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                when (rideDetails.bookingStatus) {
                    "Driver Arrived" -> showOtpVerificationDialog()
                    "Make Payment" -> showPaymentDialog(rideDetails.totalPrice.toString())
                    else -> updateDriverStatus(getNextStatus(rideDetails.bookingStatus), null)
                }
                view.resetSlider()
            }
        }
    }

    private fun showOtpVerificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_otp_verification, null)
        val otpInput = dialogView.findViewById<TextInputEditText>(R.id.otpInput)

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Verify") { dialog, _ ->
                val enteredOtp = otpInput.text.toString()
                if (enteredOtp == rideDetails.otp) {
                    updateDriverStatus("OTP Verified", null)
                } else {
                    showError("Invalid OTP")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

//    private fun setupSlideButton() {
//        binding.slideToActView.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
//            override fun onSlideComplete(view: SlideToActView) {
//                // Show loading dialog
//                val loadingDialog = MaterialAlertDialogBuilder(this@NewLiveRideActivity)
//                    .setTitle("Updating Status")
//                    .setMessage("Please wait...")
//                    .setCancelable(false)
//                    .create()
//                loadingDialog.show()
//
//                // Get next status based on current status
//                val nextStatus = when(rideDetails.bookingStatus) {
//                    "Driver Accepted" -> "Driver Arrived"
//                    "Driver Arrived" -> "OTP Verified"
//                    "OTP Verified" -> "Start Trip"
//                    "Start Trip" -> "Send Payment Details"
//                    else -> "End Trip"
//                }
//
//                // Update status
//                updateDriverStatus(nextStatus, loadingDialog)
//                view.resetSlider()
//            }
//        }
//    }

    private fun updateDriverStatus(status: String, dialog: AlertDialog?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val driverId = preferenceManager.getStringValue("goods_driver_id")

                val accessToken = AccessToken.getAccessToken()

                if (accessToken != null) {
                    if (accessToken.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (dialog != null) {
                                dialog.dismiss()
                            }
                            showError("No Token Found!")
                        }
                        return@launch
                    }
                }

                // Prepare request body
                val jsonBody = JSONObject().apply {
                    put("booking_id", rideDetails.bookingId)
                    put("booking_status", status)
                    put("server_token", accessToken)
                    put("total_payment", rideDetails.totalPrice.roundToInt().toString())
                    put("customer_id", rideDetails.customerId)
                }

                val url = "${Constants.BASE_URL}update_booking_status_driver"

                val request = object : JsonObjectRequest(
                    Method.POST,
                    url,
                    jsonBody,
                    { response ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(2000) // 2 seconds delay as in Flutter code
                            if (dialog != null) {
                                dialog.dismiss()
                            }

                            // Refresh the screen
                            if(rideDetails.bookingStatus == "Complete"){

                                Handler(Looper.getMainLooper()).postDelayed({
                                    stopLocationUpdates()
                                    // Navigate to home
                                    startActivity(Intent(this@NewLiveRideActivity, GoodsDriverHomeActivity::class.java))
                                    finishAffinity()
                                }, 2000)

                            }else {
                                recreate()
                            }
                        }
                    },
                    { error ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (dialog != null) {
                                dialog.dismiss()
                            }
                            when {
                                error.toString().contains("No Data Found") -> {
                                    showError("Already Assigned to Another Driver.\nPlease be quick at receiving ride requests to earn more.")
                                    finish()
                                }
                                else -> {
                                    error.printStackTrace()
                                    showError("Error updating status")
                                }
                            }
                        }
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        return HashMap<String, String>().apply {
                            put("Content-Type", "application/json")
                        }
                    }
                }

                // Handle cancelled status
                if (status == "Cancelled") {
                    preferenceManager.saveStringValue("current_booking_id_assigned", "")
                    showError("This Booking has been Cancelled")
                    delay(3000)
                    stopLocationUpdates()
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@NewLiveRideActivity, GoodsDriverHomeActivity::class.java))
                        finish()
                    }
                    return@launch
                }

                // Add request to queue
                VolleySingleton.getInstance(this@NewLiveRideActivity).addToRequestQueue(request)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (dialog != null) {
                        dialog.dismiss()
                    }
                    e.printStackTrace()
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun showPaymentDialog(amount: String) {
        val dialogBinding = DailogPaymentDetailsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setView(dialogBinding.root)
            .create()

        with(dialogBinding) {
            // Set amount
            amountValue.text = "₹${amount.toDouble().roundToInt()}"

            // Get selected payment type
            fun getSelectedPaymentType(): String {
                return when (paymentTypeGroup.checkedRadioButtonId) {
                    R.id.cashRadioButton -> "Cash"
                    R.id.onlineRadioButton -> "Online"
                    else -> "Cash"
                }
            }

            // Setup buttons
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            confirmButton.setOnClickListener {
                dialog.dismiss()
                showLoading() // Show loading immediately

                // Launch in IO dispatcher
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val paymentMethod = getSelectedPaymentType()
                        val accessToken = AccessToken.getAccessToken()
                        val driverId = preferenceManager.getStringValue("goods_driver_id")

                        if (accessToken.isNullOrEmpty()) {
                            withContext(Dispatchers.Main) {
                                hideLoading()
                                showError("No Token Found!")
                            }
                            return@launch
                        }

                        val jsonObject = JSONObject().apply {
                            put("booking_id", rideDetails.bookingId)
                            put("payment_method", paymentMethod)
                            put("payment_id", -1)
                            put("booking_status", "End Trip")
                            put("server_token", accessToken)
                            put("driver_id", driverId)
                            put("customer_id", rideDetails.customerId)
                            put("total_amount", amount.toDouble().roundToInt())
                        }

                        Log.d("EndTrip", "Request: $jsonObject")

                        // Make network request
                        val response = suspendCoroutine<JSONObject> { continuation ->
                            val request = object : JsonObjectRequest(
                                Method.POST,
                                "${Constants.BASE_URL}generate_order_id_for_booking_id_goods_driver",
                                jsonObject,
                                { response ->
                                    Log.d("EndTrip", "Success Response: $response")
                                    continuation.resume(response)
                                },
                                { error ->
                                    Log.e("EndTrip", "Error: ${error.message}", error)
                                    continuation.resumeWithException(error)
                                }
                            ) {
                                override fun getHeaders(): MutableMap<String, String> {
                                    return HashMap<String, String>().apply {
                                        put("Content-Type", "application/json")
                                    }
                                }
                            }

                            VolleySingleton.getInstance(this@NewLiveRideActivity)
                                .addToRequestQueue(request)
                        }

                        // Handle successful response
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            preferenceManager.saveStringValue("current_booking_id_assigned", "")
                            stopLocationUpdates()
                            showSuccess("Trip Completed Successfully!")

                            // Delay before navigation
                            delay(2000)
                            navigateToHome()
                        }

                    } catch (e: Exception) {
                        Log.e("EndTrip", "Error in payment processing", e)
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            when {
                                e.toString().contains("No Data Found") -> {
                                    showError("Already Assigned to Another Driver")
                                    delay(2000)
                                    finish()
                                }
                                else -> {
                                    showError("Payment Processing Failed: ${e.message}")
                                }
                            }
                        }
                    }
                }

            }
        }

        dialog.show()
    }

    // Add a success message method
    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.green))
            .show()
    }

    private suspend fun saveAndResetEndTripDetails(
        status: String,
        otp: String? = null,
        amount: String? = null,
        paymentMethod: String? = null
    ) = withContext(Dispatchers.IO) {
        println("Entering into saveAndResetEndTripDetails")// Move to IO thread
        try {
            val pref = preferenceManager

            if (rideDetails.bookingStatus == "Cancelled") {
                withContext(Dispatchers.Main) {
                    preferenceManager.saveStringValue("current_booking_id_assigned", "")
                    showError("This Booking has been Cancelled")
                    delay(3000)
                    stopLocationUpdates()
                    navigateToHome()
                }
                return@withContext
            }

            withContext(Dispatchers.Main) { showLoading() }

            Log.d("EndTrip", "Updating status to: $status")
            amount?.let { Log.d("EndTrip", "Entered Amount: $it") }

            val driverId = pref.getStringValue("goods_driver_id")

            // Get access token in IO context
            val accessToken = AccessToken.getAccessToken()

            if (accessToken.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    showError("No Token Found!")
                }
                return@withContext
            }

            val jsonObject = JSONObject().apply {
                put("booking_id", rideDetails.bookingId)
                put("payment_method", paymentMethod)
                put("payment_id", -1)
                put("booking_status", status)
                put("server_token", accessToken)
                put("driver_id", driverId)
                put("customer_id", rideDetails.customerId)
                put("total_amount", amount?.toDouble()?.roundToInt())
            }

            println("jsonObject::$jsonObject")

            // Use suspendCoroutine to wrap Volley request in coroutine
            val response = suspendCoroutine<JSONObject> { continuation ->
                val request = JsonObjectRequest(
                    Request.Method.POST,
                    "${Constants.BASE_URL}generate_order_id_for_booking_id_goods_driver",
                    jsonObject,
                    { response -> continuation.resume(response) },
                    { error -> continuation.resumeWithException(error) }
                )

                VolleySingleton.getInstance(this@NewLiveRideActivity)
                    .addToRequestQueue(request)
            }

            Log.d("EndTrip", "Response: $response")

            // Handle successful response
            withContext(Dispatchers.Main) {
                preferenceManager.saveStringValue("current_booking_id_assigned", "")
                delay(3000)
                stopLocationUpdates()
                Log.d("EndTrip", "Trip Ended - Stopping driver's live location updates")
                navigateToHome()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                hideLoading()
                when {
                    e.toString().contains("No Data Found") -> {
                        showError("Already Assigned to Another Driver.\nPlease be quick at receiving ride requests to earn more.")
                        finish()
                    }
                    else -> {
                        showError("Something went wrong: ${e.message}")
                    }
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                hideLoading()
            }
        }
    }

    private fun navigateToHome() {
        Intent(this, GoodsDriverHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finishAffinity()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
        }

        startLocationUpdates()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
//            val locationRequest = LocationRequest.create().apply {
//                priority = Priority.PRIORITY_HIGH_ACCURACY
//                interval = 5000
//                fastestInterval = 3000
//            }
//
//            fusedLocationClient.requestLocationUpdates(
//                locationRequest,
//                locationCallback,
//                Looper.getMainLooper()
//            )
            startLocationService()
        }
    }

    private fun startLocationService() {
        // Create intent for the location service
        val serviceIntent = Intent(this, LocationUpdateService::class.java)

        // Start and bind the service
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)


    }




    private fun getBitmapDescriptor(vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(this, vectorResId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Helper function to scale the marker if needed
    private fun getBitmapDescriptorWithScale(
        vectorResId: Int,
        width: Int,
        height: Int
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(this, vectorResId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        vectorDrawable.setBounds(0, 0, width, height)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Call this when driver location updates
    // Moving heavy operations to a background thread
    private fun updateDriverLocation(location: Location) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val driverLatLng = LatLng(location.latitude, location.longitude)

                if (currentMarker == null) {
                    currentMarker = map.addMarker(
                        MarkerOptions()
                            .position(driverLatLng)
                            .title("Your Location")
                            .icon(getBitmapDescriptorWithScale(R.drawable.ic_driver_marker, 48, 48))
                    )
                } else {
                    currentMarker?.position = driverLatLng
                }

                // Update route based on new driver location
                showRoute(driverLatLng)
            } catch (e: Exception) {
                Log.e("LiveRide", "Error updating driver location: ${e.message}")
            }
        }
    }



    private fun getCurrentStatus(): String {
        return binding.statusChip.text.toString()
    }

    private fun updateStatus(newStatus: String) {
        binding.statusChip.text = newStatus
        // Update button text for next status
        binding.slideToActView.text = "Slide to ${getNextStatus(newStatus)}"
    }

    private fun getNextStatus(currentStatus: String): String {
        return when (currentStatus) {

            "Driver Accepted" -> "Driver Arrived"
            "Driver Arrived" -> "OTP Verified"
            "OTP Verified" -> "Start Trip"
            "Start Trip" -> "Make Payment"
            "Make Payment" -> "End Trip"
            else -> "Complete"
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            // Cleanup
            locationReceiver = null
            if (bound) {
                unbindService(serviceConnection)
                bound = false
            }
            stopLocationUpdates()
        } catch (e: Exception) {
            Log.e("LiveRide", "Error in onDestroy: ${e.message}")
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }



    // Update your activity's location receiver registration to use LocalBroadcastManager
    override fun onResume() {
        try {
            super.onResume()
            locationReceiver?.let { receiver ->
                LocalBroadcastManager.getInstance(this)
                    .registerReceiver(receiver, IntentFilter("LOCATION_UPDATE"))
            }
        } catch (e: Exception) {
            Log.e("LiveRide", "Error in onResume: ${e.message}")
        }
    }

    override fun onPause() {
        try {
            super.onPause()
            locationReceiver?.let { receiver ->
                LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(receiver)
            }
        } catch (e: Exception) {
            Log.e("LiveRide", "Error in onPause: ${e.message}")
        }
    }

    private fun calculateDistanceAndTime(origin: Location, destination: Location) {
        if (!shouldMakeApiCall()) {
            return
        }

        val originLatLng = "${origin.latitude},${origin.longitude}"
        val destLatLng = "${destination.latitude},${destination.longitude}"

        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "origins=$originLatLng" +
                "&destinations=$destLatLng" +
                "&mode=driving" +
                "&key=${getString(R.string.google_maps_key)}"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    Log.d("API_RESPONSE", response.toString())  // Log response for debugging

                    val rows = response.getJSONArray("rows")
                    if (rows.length() > 0) {
                        val elements = rows.getJSONObject(0).getJSONArray("elements")
                        if (elements.length() > 0) {
                            val element = elements.getJSONObject(0)

                            if (element.has("distance") && element.has("duration")) {
                                val distanceMeters = element.getJSONObject("distance").getInt("value") // Distance in meters
                                val durationSeconds = element.getJSONObject("duration").getInt("value") // Time in seconds

                                // Convert meters to kilometers (1 km = 1000 meters)
                                val distanceKm = distanceMeters / 1000.0

                                // Convert seconds to minutes (1 min = 60 seconds)
                                val durationMinutes = durationSeconds / 60

                                updateRouteInfo(
                                    "${String.format("%.1f", distanceKm)} km",
                                    "$durationMinutes mins",
                                    LatLng(destination.latitude, destination.longitude)
                                )

                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("API_ERROR", "Error parsing API response", e)
                }

                // If API fails or returns incomplete data, use fallback calculation
                fallbackDistanceCalculation(origin, destination)
            },
            { error ->
                Log.e("API_ERROR", "API Request Failed", error)
                fallbackDistanceCalculation(origin, destination) // Use fallback if API request fails
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }



    private fun fallbackDistanceCalculation(origin: Location, destination: Location) {
        val earthRadiusKm = 6371.0

        val lat1 = Math.toRadians(origin.latitude)
        val lon1 = Math.toRadians(origin.longitude)
        val lat2 = Math.toRadians(destination.latitude)
        val lon2 = Math.toRadians(destination.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distanceInKm = earthRadiusKm * c
        val estimatedSpeedKmH = 40.0 // Adjust based on urban/rural conditions
        val timeInMinutes = ((distanceInKm / estimatedSpeedKmH) * 60).roundToInt()

        updateRouteInfo(
            "${String.format("%.1f", distanceInKm)} km",
            "$timeInMinutes mins",
            LatLng(destination.latitude, destination.longitude)
        )
    }


//    private fun calculateDistanceAndTime(origin: Location, destination: Location) {
//        if (!shouldMakeApiCall()) {
//            return
//        }
//        val originLatLng = "${origin.latitude},${origin.longitude}"
//        val destLatLng = "${destination.latitude},${destination.longitude}"
//
//        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
//                "origins=$originLatLng" +
//                "&destinations=$destLatLng" +
//                "&mode=driving" +
//                "&key=${getString(R.string.google_maps_key)}"
//
//        val request = JsonObjectRequest(
//            Request.Method.GET,
//            url,
//            null,
//            { response ->
//                try {
//                    val rows = response.getJSONArray("rows")
//                    if (rows.length() > 0) {
//                        val elements = rows.getJSONObject(0).getJSONArray("elements")
//                        if (elements.length() > 0) {
//                            val element = elements.getJSONObject(0)
//
//                            val distance = element.getJSONObject("distance").getString("text")
//                            val duration = element.getJSONObject("duration").getString("text")
//
//                            // Update UI with real distance and time from Google Maps
//                            updateRouteInfo(
//                                distance,
//                                duration,
//                                LatLng(destination.latitude, destination.longitude)
//                            )
//                        }
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    // Fallback to simple calculation if API fails
//                    val distanceInMeters = origin.distanceTo(destination)
//                    val distanceInKm = distanceInMeters / 1000
//                    val timeInHours = distanceInKm / 30
//                    val timeInMinutes = (timeInHours * 60).roundToInt()
//
//                    updateRouteInfo(
//                        "${String.format("%.1f", distanceInKm)} km",
//                        "$timeInMinutes mins",
//                        LatLng(destination.latitude, destination.longitude)
//                    )
//                }
//            },
//            { error ->
//                error.printStackTrace()
//                // Fallback to simple calculation if API fails
//                val distanceInMeters = origin.distanceTo(destination)
//                val distanceInKm = distanceInMeters / 1000
//                val timeInHours = distanceInKm / 30
//                val timeInMinutes = (timeInHours * 60).roundToInt()
//
//                updateRouteInfo(
//                    "${String.format("%.1f", distanceInKm)} km",
//                    "$timeInMinutes mins",
//                    LatLng(destination.latitude, destination.longitude)
//                )
//            }
//        )
//
//        VolleySingleton.getInstance(this).addToRequestQueue(request)
//    }

    // Update the location receiver to calculate distance and time

//    private val locationReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            if (intent?.action == "LOCATION_UPDATE") {
//                var latitude = intent.getDoubleExtra("latitude", 0.0)
//                var longitude = intent.getDoubleExtra("longitude", 0.0)
//                val currentLocation = Location("").apply {
//                    this.latitude = latitude
//                    this.longitude = longitude
//                }
//
//                // Update driver's position in backend
//                lifecycleScope.launch {
//                    updateDriversCurrentPosition(latitude, longitude)
//                }
//
//                // Get destination based on status
//                val destination = if (rideDetails.bookingStatus == "Start Trip") {
//                    Location("").apply {
//                        latitude = rideDetails.destinationLat
//                        longitude = rideDetails.destinationLng
//                    }
//                } else {
//                    Location("").apply {
//                        latitude = rideDetails.pickupLat
//                        longitude = rideDetails.pickupLng
//                    }
//                }
//
//                // Calculate distance and time using Google Maps API
////                calculateDistanceAndTime(currentLocation, destination)
//            }
//        }
//    }

    private suspend fun updateDriversCurrentPosition(latitude: Double, longitude: Double) {
        try {
//            val sharedPreferences = getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)

            // Get previous location
            val previousLat = preferenceManager.getFloatValue("goods_driver_current_lat").toDouble()
            val previousLng = preferenceManager.getFloatValue("goods_driver_current_lng").toDouble()
            val driverId = preferenceManager.getStringValue("goods_driver_id")

            Log.d("DriverLocation", "Current Lat: $latitude")
            Log.d("DriverLocation", "Current Lng: $longitude")


// Check if location has changed
            if (isSameLocation(previousLat, previousLng, latitude, longitude)) {
                Log.d("DriverSameLocationSkipped", "Same location, skipping update")
                return
            }
            // Prepare request body
            val jsonBody = JSONObject().apply {
                put("goods_driver_id", driverId)
                put("lat", latitude)
                put("lng", longitude)
            }

            // Make API call using Volley
            val url = "${Constants.BASE_URL}update_goods_drivers_current_location"

            withContext(Dispatchers.IO) {
                val request = object : JsonObjectRequest(
                    Method.POST,
                    url,
                    jsonBody,
                    { response ->
                        Log.d("DriverLocation", "Update successful: $response")

                        // Save new location to SharedPreferences
                        preferenceManager.saveFloatValue("goods_driver_current_lat",latitude.toFloat())
                        preferenceManager.saveFloatValue("goods_driver_current_lng",longitude.toFloat())


                        Log.d("DriverLocation", "Saved prev_latitude: $latitude")
                        Log.d("DriverLocation", "Saved prev_longitude: $longitude")
                    },
                    { error ->
                        error.printStackTrace()
                        Log.e("DriverLocation", "Error updating location: ${error.message}")
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        return HashMap<String, String>().apply {
                            put("Content-Type", "application/json")
                        }
                    }
                }

                VolleySingleton.getInstance(this@NewLiveRideActivity).addToRequestQueue(request)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DriverLocation", "Error: ${e.message}")
        }
    }

    private fun isSameLocation(
        previousLat: Double?,
        previousLng: Double?,
        currentLat: Double,
        currentLng: Double
    ): Boolean {
        if (previousLat == null || previousLng == null) return false

        // You can adjust the threshold based on your needs
        val threshold = 0.0001 // Approximately 11 meters
        return abs(previousLat - currentLat) < threshold &&
                abs(previousLng - currentLng) < threshold
    }

    // Add rate limiting to avoid excessive API calls
    private var lastApiCallTime = 0L
    private val API_CALL_INTERVAL = 30000L // 30 seconds

    private fun shouldMakeApiCall(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastApiCallTime > API_CALL_INTERVAL) {
            lastApiCallTime = currentTime
            return true
        }
        return false
    }
}