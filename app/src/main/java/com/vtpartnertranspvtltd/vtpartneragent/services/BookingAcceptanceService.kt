package com.vtpartnertranspvtltd.vtpartneragent.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.VTPartnerApp
import android.view.ContextThemeWrapper
import android.os.CountDownTimer
import android.widget.ProgressBar
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject

class BookingAcceptanceService : Service() {
    private var windowManager: WindowManager? = null
    private var bookingView: View? = null
    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var preferenceManager:PreferenceManager
    private val DISPLAY_DURATION = 15000L // 15 seconds
    private val NOTIFICATION_ID = 2

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        initMediaPlayer()
        preferenceManager = PreferenceManager.getInstance(this)
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.booking_notification)
        mediaPlayer?.isLooping = true
    }

    private fun createNotification(): Notification {
        val channelId = "booking_acceptance_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Booking Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // No sound for notification channel
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("New Booking Request")
            .setContentText("Processing booking request...")
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW_BOOKING" -> {
                val bookingId = intent.getStringExtra("booking_id")
                val bookingDetails = intent.getStringExtra("booking_details")
                if (bookingId != null) {
                    removeCurrentView()
                    showBookingAcceptanceView(bookingId, bookingDetails)
                }
            }
        }
        return START_STICKY
    }

    private fun showBookingAcceptanceView(bookingId: String, bookingDetails: String?) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        bookingView = inflater.inflate(R.layout.layout_booking_acceptance, null)

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP
            y = 100
        }

        setupUI(bookingId, bookingDetails)

        try {
            windowManager?.addView(bookingView, params)
            startCountdown()
            playNotificationSound()
        } catch (e: Exception) {
            Log.e("BookingService", "Error showing view: ${e.message}")
        }
    }

    private fun setupUI(bookingId: String, bookingDetails: String?) {
        bookingView?.apply {
            try {
                val details = JSONObject(bookingDetails ?: "{}")

                // Set passenger details
                findViewById<TextView>(R.id.passengerName)?.text = details.optString("customer_name", "New Customer")

                // Set fare and distance
                findViewById<TextView>(R.id.rideFare)?.text = "₹${details.optString("total_price", "0")}"
                findViewById<TextView>(R.id.locationDistance)?.text = "${details.optString("total_distance", "0")} km"
                findViewById<TextView>(R.id.tripDuration)?.text = details.optString("total_time", "")

                // Set locations
                findViewById<TextView>(R.id.pickupLocation)?.text = details.optString("pickup_address", "")
                findViewById<TextView>(R.id.dropLocation)?.text = details.optString("drop_address", "")

                // Load passenger image
                Glide.with(this)
                    .load(details.optString("customer_image", ""))
                    .placeholder(R.drawable.demo_user)

                    .into(findViewById(R.id.passengerImage))

                // Setup buttons
                findViewById<Button>(R.id.acceptButton)?.setOnClickListener {
                    handleBookingAcceptance(bookingId, details)
                }

                findViewById<Button>(R.id.rejectButton)?.setOnClickListener {
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e("BookingService", "Error setting up UI: ${e.message}")
            }
        }
    }

    private fun startCountdown() {
        val progressBar = bookingView?.findViewById<ProgressBar>(R.id.timeProgressBar)
        val timeRemainingText = bookingView?.findViewById<TextView>(R.id.timeRemainingText)

        progressBar?.max = DISPLAY_DURATION.toInt()
        progressBar?.progress = DISPLAY_DURATION.toInt()

        countDownTimer = object : CountDownTimer(DISPLAY_DURATION, 100) {
            override fun onTick(millisUntilFinished: Long) {
                progressBar?.progress = millisUntilFinished.toInt()
                timeRemainingText?.text = "${(millisUntilFinished / 1000)} sec left"
            }

            override fun onFinish() {
                stopSelf()
            }
        }.start()
    }

    private fun playNotificationSound() {
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("BookingService", "Error playing sound: ${e.message}")
        }
    }

    private fun handleBookingAcceptance(bookingId: String, details: JSONObject) {
        countDownTimer?.cancel()
        stopNotificationSound()

        // Make API call using Volley
        val url = "${Constants.BASE_URL}goods_driver_booking_accepted"
        val accessToken = AccessToken.getAccessToken()
        var goods_driver_id = preferenceManager.getStringValue("goods_driver_id")
        val requestBody = JSONObject().apply {
            put("booking_id", bookingId)
            put("driver_id", goods_driver_id)
            put("server_token", accessToken)
            put("customer_id", details.optString("customer_id"))
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            { response ->
                // Save current booking ID
                //PreferenceManager(this).saveCurrentBookingId(bookingId)

                // Start new activity
                Intent(this, GoodsDriverHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("route", "ongoing_ride")
                    putExtra("booking_id", bookingId)
                    startActivity(this)
                }

                stopSelf()
            },
            { error ->
                Log.e("BookingService", "Error accepting booking: ${error.message}")
                Toast.makeText(this, "Failed to accept booking", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun stopNotificationSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    private fun removeCurrentView() {
        if (windowManager != null && bookingView != null) {
            try {
                windowManager?.removeView(bookingView)
                bookingView = null
            } catch (e: IllegalArgumentException) {
                // View might already be removed
            }
        }
        countDownTimer?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeCurrentView()
        stopNotificationSound()
    }
}