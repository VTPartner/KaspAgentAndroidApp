package com.vtpartnertranspvtltd.vtpartneragent.services

import BookingActionReceiver
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vt_partner.utils.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.vtpartnertranspvtltd.vtpartneragent.VTPartnerApp
import kotlinx.coroutines.cancel
import com.vtpartnertranspvtltd.vtpartneragent.MainActivity
import android.os.CountDownTimer
import android.util.Log
import android.view.ContextThemeWrapper
import android.os.PowerManager
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.provider.Settings
import com.google.android.material.button.MaterialButton

class FloatingHeadService : Service() {
    private var windowManager: WindowManager? = null
    private var currentView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var countDownTimer: CountDownTimer? = null
    private val DISPLAY_DURATION = 30000L // 30 seconds for booking view

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Request ignore battery optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
        
        registerNotificationReceiver()
        startServiceKeepAlive()
    }

    private fun registerNotificationReceiver() {
        val filter = IntentFilter().apply {
            addAction("com.vtpartnertranspvtltd.vtpartneragent.NEW_BOOKING")
        }
        registerReceiver(notificationReceiver, filter)
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.vtpartnertranspvtltd.vtpartneragent.NEW_BOOKING" -> {
                    val bookingId = intent.getStringExtra("booking_id")
                    val bookingDetails = intent.getStringExtra("booking_details")
                    if (bookingId != null) {
                        showBookingAcceptanceView(bookingId, bookingDetails)
                    }
                }
            }
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "VTPartner:FloatingHeadWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun showFloatingHead() {
        removeCurrentView()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        currentView = LayoutInflater.from(this).inflate(R.layout.layout_floating_head, null)

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            
            // Handle different Android versions
            type = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                else -> {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
                }
            }

            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        setupFloatingHeadTouchListener(params)
        windowManager?.addView(currentView, params)
    }

    private fun showBookingAcceptanceView(bookingId: String, bookingDetails: String?) {
        Log.d(TAG, "Attempting to show booking view for: $bookingId")
        
        // Ensure we're on main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                showBookingAcceptanceView(bookingId, bookingDetails)
            }
            return
        }

        try {
            // Wake device and turn screen on
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "VTPartner:BookingViewWakeLock"
            )
            wakeLock.acquire(30000) // 30 seconds

            // Remove any existing view
            removeCurrentView()

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val themedContext = ContextThemeWrapper(this, R.style.Theme_VTPartnerAgent)
            
            currentView = LayoutInflater.from(themedContext).inflate(R.layout.layout_booking_acceptance, null)
            
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP
            }

            setupBookingUI(bookingId, bookingDetails)
            
            // Try multiple times to add the view
            var attempts = 0
            val maxAttempts = 3
            
            while (attempts < maxAttempts) {
                try {
                    windowManager?.addView(currentView, params)
                    Log.d(TAG, "Successfully added booking view for: $bookingId")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Attempt ${attempts + 1} failed: ${e.message}")
                    attempts++
                    if (attempts < maxAttempts) {
                        Thread.sleep(500)
                    }
                }
            }

            wakeLock.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing booking view: ${e.message}")
            // Try again after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                showBookingAcceptanceView(bookingId, bookingDetails)
            }, 1000)
        }
    }

    private fun setupBookingUI(bookingId: String, bookingDetails: String?) {
        currentView?.apply {
            // Set booking details
//            findViewById<TextView>(R.id.booking_details)?.text = bookingDetails
//
//            // Setup accept button
//            findViewById<MaterialButton>(R.id.accept_button)?.setOnClickListener {
//                countDownTimer?.cancel()
//                sendBroadcast(Intent(this@FloatingHeadService, BookingActionReceiver::class.java).apply {
//                    action = "ACCEPT_BOOKING"
//                    putExtra("booking_id", bookingId)
//                })
//                removeCurrentView()
//            }
//
//            // Setup reject button
//            findViewById<MaterialButton>(R.id.reject_button)?.setOnClickListener {
//                countDownTimer?.cancel()
//                sendBroadcast(Intent(this@FloatingHeadService, BookingActionReceiver::class.java).apply {
//                    action = "REJECT_BOOKING"
//                    putExtra("booking_id", bookingId)
//                })
//                removeCurrentView()
//            }
//
//            // Setup close button
//            findViewById<MaterialButton>(R.id.close_button)?.setOnClickListener {
//                countDownTimer?.cancel()
//                removeCurrentView()
//            }

            // Start countdown
            startCountdown()
        }
    }

    private fun setupFloatingHeadTouchListener(params: WindowManager.LayoutParams) {
        currentView?.findViewById<ImageView>(R.id.floating_head_image)?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(currentView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(event.rawX - initialTouchX) < 10 && 
                        Math.abs(event.rawY - initialTouchY) < 10) {
                        // Open app if minimized
//                        val intent = Intent(this, MainActivity::class.java).apply {
//                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
//                        }
//                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        
        val progressBar = currentView?.findViewById<ProgressBar>(R.id.timeProgressBar)
        progressBar?.max = 5000 // Match layout max value
        progressBar?.progress = 5000 // Start from full

        countDownTimer = object : CountDownTimer(30000, 100) { // 30 seconds, update every 100ms
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((millisUntilFinished.toFloat() / 30000) * 5000).toInt()
                progressBar?.progress = progress
            }

            override fun onFinish() {
                Handler(Looper.getMainLooper()).post {
                    removeCurrentView()
                }
            }
        }.start()
    }

    private fun handleBookingAcceptance(bookingId: String) {
        // TODO: Implement booking acceptance
        showFloatingHead()
    }

    private fun handleBookingRejection(bookingId: String) {
        // TODO: Implement booking rejection
        showFloatingHead()
    }

    private fun removeCurrentView() {
        if (windowManager != null && currentView != null) {
            try {
                windowManager?.removeView(currentView)
                currentView = null
            } catch (e: IllegalArgumentException) {
                // View might already be removed
            }
        }
        countDownTimer?.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW_BOOKING" -> {
                val bookingId = intent.getStringExtra("booking_id")
                val bookingDetails = intent.getStringExtra("booking_details")

                if (bookingId != null) {
                    // Wake device and ensure screen is on
                    wakeDevice()
                    
                    // Remove existing view
                    removeCurrentView()
                    
                    // Show new booking view with retries
                    var attempts = 0
                    val maxAttempts = 3
                    
                    fun tryShowView() {
                        if (attempts < maxAttempts) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    showBookingAcceptanceView(bookingId, bookingDetails)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Attempt ${attempts + 1} failed: ${e.message}")
                                    attempts++
                                    tryShowView()
                                }
                            }, (500 * (attempts + 1)).toLong())
                        }
                    }
                    
                    tryShowView()
                }
            }
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, VTPartnerApp.FLOATING_SERVICE_CHANNEL_ID)
            .setContentTitle("VT Partner Active")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Receiver might already be unregistered
        }
        wakeLock?.release()
        removeCurrentView()
        serviceScope.cancel()
    }

    private fun wakeDevice() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE, "VTPartner:BookingWakeLock"
        )
        wakeLock.acquire(10000) // Release after 10 seconds
    }

    private fun startServiceKeepAlive() {
        serviceScope.launch {
            while (true) {
                delay(30000) // Check every 30 seconds
                if (currentView == null) {
                    showFloatingHead()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Restart service if killed
        val restartIntent = Intent(applicationContext, FloatingHeadService::class.java)
        restartIntent.setPackage(packageName)
        startService(restartIntent)
    }

    companion object {
        private const val TAG = "FloatingHeadService"
        private const val NOTIFICATION_ID = 1
    }
} 