package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.notification_dialog

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings.NewLiveRideActivity
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.services.AccessToken
import com.vtpartnertranspvtltd.vtpartneragent.services.FCMService
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

class BookingDialogActivity : AppCompatActivity() {
    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isShowMore = true
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var preferenceManager:PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_dialog)
        preferenceManager = PreferenceManager.getInstance(this)
        val bottomSheet = findViewById<View>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.isDraggable = false

        initializeMediaPlayer()

        val bookingDetailsStr = intent.getStringExtra("booking_details")
        val bookingId = intent.getStringExtra("booking_id")

        if (bookingDetailsStr != null) {
            try {
                val bookingDetails = JSONObject(bookingDetailsStr)
                setupDialog(bookingDetails, bookingId)
            } catch (e: Exception) {
                finish()
            }
        } else {
            finish()
        }
    }

    private fun calculateDistance(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
        callback: (String?, String?) -> Unit
    ) {
        val origin = "$originLat,$originLng"
        val destination = "$destinationLat,$destinationLng"
        val apiKey = Constants.MAP_KEY

        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "origins=$origin&" +
                "destinations=$destination&" +
                "mode=driving&" +
                "key=$apiKey"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    val rows = response.getJSONArray("rows")
                    if (rows.length() > 0) {
                        val elements = rows.getJSONObject(0).getJSONArray("elements")
                        if (elements.length() > 0) {
                            val element = elements.getJSONObject(0)

                            val distance = element.getJSONObject("distance").getString("text")
                            val duration = element.getJSONObject("duration").getString("text")

                            callback(distance, duration)
                        } else {
                            callback(null, null)
                        }
                    } else {
                        callback(null, null)
                    }
                } catch (e: Exception) {
                    Log.e("DistanceCalculation", "Error parsing response: ${e.message}")
                    callback(null, null)
                }
            },
            { error ->
                Log.e("DistanceCalculation", "Error fetching distance: ${error.message}")
                callback(null, null)
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    // Get current location
    private fun getCurrentLocationAndCalculateDistance(bookingDetails: JSONObject,destinationLat: Double, destinationLng: Double) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    calculateDistance(
                        location.latitude,
                        location.longitude,
                        destinationLat,
                        destinationLng
                    ) { distance, duration ->
                        if (distance != null && duration != null) {
                            // Use the distance and duration
                            runOnUiThread {
//                                Toast.makeText(
//                                    this,
//                                    "Distance: $distance, Duration: $duration",
//                                    Toast.LENGTH_LONG
//                                ).show()
                                findViewById<TextView>(R.id.pickupLocationDistance).text = distance

                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Could not calculate distance",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Error getting location: ${e.message}")
            }
    }

    private fun setupDialog(bookingDetails: JSONObject, bookingId: String?) {
        println("bookingDetails::$bookingDetails")
        val pickup_lat = bookingDetails.optString("pickup_lat")
        val pickup_lng = bookingDetails.optString("pickup_lng")
        getCurrentLocationAndCalculateDistance(bookingDetails, pickup_lat.toDouble(), pickup_lng.toDouble())

        // Setup views
        findViewById<TextView>(R.id.pickupAddress).text =
            "Pickup: ${bookingDetails.optString("pickup_address")}"
        findViewById<TextView>(R.id.dropAddress).text =
            "Drop: ${bookingDetails.optString("drop_address")}"

        findViewById<TextView>(R.id.customerName).text =
            bookingDetails.optString("customer_name")

        // Format total price - rounded to whole number
        val totalPrice = try {
            bookingDetails.optDouble("total_price").roundToInt()
        } catch (e: Exception) {
            0
        }
        findViewById<TextView>(R.id.rideFare).text = "₹$totalPrice"

        // Format distance - 1 decimal point
        val distance = try {
            String.format("%.1f", bookingDetails.optDouble("distance"))
        } catch (e: Exception) {
            "0.0"
        }
        findViewById<TextView>(R.id.distance).text = "$distance Km"

        // Rest of your code...
        findViewById<Button>(R.id.acceptButton).setOnClickListener {
            countDownTimer?.cancel()
            stopNotificationSound()
            acceptRideRequest(bookingDetails, bookingId)
        }

        findViewById<Button>(R.id.rejectButton).setOnClickListener {
            countDownTimer?.cancel()
            stopNotificationSound()
            finish()
        }

        findViewById<Button>(R.id.moreButton).setOnClickListener {
            isShowMore = !isShowMore
            updateExpandedState()
        }

        playNotificationSound()
        startCountdownTimer()
    }

    private fun updateExpandedState() {
        val moreButton = findViewById<Button>(R.id.moreButton)
        val expandedContent = findViewById<View>(R.id.expandedContent)

        moreButton.text = if (isShowMore) "Less" else "More"
        expandedContent.visibility = if (isShowMore) View.VISIBLE else View.GONE

        // Update arrow icon
        moreButton.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (isShowMore) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up,
            0
        )
    }

    private fun startCountdownTimer() {
        countDownTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                findViewById<TextView>(R.id.timerText).text =
                    "${millisUntilFinished / 1000} sec left"
            }

            override fun onFinish() {
                finish()
            }
        }.start()
    }

    private fun acceptRideRequest(bookingDetails: JSONObject, bookingId: String?) {
        if (bookingId == null) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Cancel timer and stop sound
        countDownTimer?.cancel()
        stopNotificationSound()

        // Show loading dialog
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Please wait...")
        progressDialog.show()

        // Launch coroutine for network operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get SharedPreferences

                val driverId = preferenceManager.getStringValue("goods_driver_id")

                // Get access token in background thread
                val accessToken = withContext(Dispatchers.IO) {
                    AccessToken.getAccessToken()
                }

                if (accessToken != null) {
                    if (accessToken.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(this@BookingDialogActivity, "No Token Found!", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }

                // Save current booking ID
                preferenceManager.saveStringValue("current_booking_id_assigned", bookingId)

                // Prepare request body
                val jsonBody = JSONObject().apply {
                    put("booking_id", bookingId)
                    put("driver_id", driverId)
                    put("server_token", accessToken)
                    put("customer_id", bookingDetails.optString("customer_id"))
                }

                // Make API request using Volley
                withContext(Dispatchers.Main) {
                    val request = object : JsonObjectRequest(
                        Method.POST,
                        "${Constants.BASE_URL}goods_driver_booking_accepted",
                        jsonBody,
                        { response ->
                            progressDialog.dismiss()
                            // Handle successful response
                            val intent = Intent(this@BookingDialogActivity, NewLiveRideActivity::class.java)
                            startActivity(intent)

                        },
                        { error ->
                            progressDialog.dismiss()
                            handleVolleyError(error)
                        }
                    ) {
                        override fun getHeaders(): MutableMap<String, String> {
                            return HashMap<String, String>().apply {
                                put("Content-Type", "application/json")
                                put("Authorization", "Bearer $accessToken")
                            }
                        }
                    }

                    // Add request timeout
                    request.retryPolicy = DefaultRetryPolicy(
                        30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )

                    // Add to request queue
                    VolleySingleton.getInstance(this@BookingDialogActivity).addToRequestQueue(request)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    handleError(e)
                }
            }
        }
    }

    private fun handleVolleyError(error: VolleyError) {
        if (error.networkResponse?.data != null) {
            try {
                val errorJson = String(error.networkResponse!!.data)
                val errorObj = JSONObject(errorJson)
                if (errorObj.optString("message").contains("No Data Found")) {
                    handleNoDataFound()
                } else {
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(error)
            }
        } else {
            handleError(error)
        }
    }

    private fun handleNoDataFound() {
        // Clear current booking ID
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .edit()
            .putString("current_booking_id_assigned", "")
            .apply()

        Toast.makeText(
            this,
            "Already Assigned to Another Driver.\nPlease be quick at receiving ride requests to earn more.",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun handleError(error: Exception) {
        Toast.makeText(
            this,
            "Error: ${error.message ?: "Unknown error occurred"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.booking_notification)
        mediaPlayer?.isLooping = true
    }

    private fun playNotificationSound() {
        mediaPlayer?.start()
    }

    private fun stopNotificationSound() {
        mediaPlayer?.stop()
        mediaPlayer?.prepare()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        mediaPlayer?.release()
    }

     val LOCATION_PERMISSION_REQUEST_CODE = 1001
}