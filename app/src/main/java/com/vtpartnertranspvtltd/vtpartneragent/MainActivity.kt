package com.vtpartnertranspvtltd.vtpartneragent

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import com.vtpartnertranspvtltd.vtpartneragent.services.AccessToken
import com.vtpartnertranspvtltd.vtpartneragent.services.FloatingHeadService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.vtpartnertranspvtltd.vtpartneragent.services.BookingAcceptanceService
import android.os.PowerManager
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Get tokens first
        getTokens()
        
        // Start permission checks
        checkBatteryOptimization()
        
        // Handle intent data if any
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Save the new intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Log.d(TAG, "Handling intent: $intent")
        
        if (intent?.getBooleanExtra("show_booking", false) == true) {
            val bookingId = intent.getStringExtra("booking_id")
            val bookingDetails = intent.getStringExtra("booking_details")
            val fromNotification = intent.getBooleanExtra("from_notification", false)
            
            Log.d(TAG, "Showing booking details for ID: $bookingId, from notification: $fromNotification")
            
            if (fromNotification) {
                // If from notification, show floating view
                showBookingDetails(bookingId, bookingDetails)
            }
        }
    }

    private fun getTokens() {
        // Get server access token
        GlobalScope.launch {
            val token = AccessToken.getAccessToken()
            println("Server Access Token: $token")
        }

        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            println("FCM TOken::$token")
            Log.d(ContentValues.TAG, "fcm token updated successfully")
            Log.d(ContentValues.TAG, token)
        })
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog()
            } else {
                checkOverlayPermission()
            }
        } else {
            checkOverlayPermission()
        }
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage("Please disable battery optimization for this app to ensure reliable operation.")
            .setPositiveButton("Disable") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE)
            }
            .setNegativeButton("Later") { _, _ ->
                checkOverlayPermission()
            }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BATTERY_OPTIMIZATION_REQUEST_CODE -> {
                checkOverlayPermission()
            }
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Settings.canDrawOverlays(this)) {
                    checkNotificationPermission()
                } else {
                    showOverlayPermissionDialog()
                }
            }
        }
    }

    private fun showBookingDetails(bookingId: String?, bookingDetails: String?) {
        if (bookingId != null) {
            try {
                // Start floating service with booking details
                val serviceIntent = Intent(this, FloatingHeadService::class.java).apply {
                    action = "SHOW_BOOKING"
                    putExtra("booking_id", bookingId)
                    putExtra("booking_details", bookingDetails)
                    putExtra("force_show", true)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                
                // Minimize the app
                moveTaskToBack(true)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing booking details: ${e.message}")
                Toast.makeText(this, "Error showing booking details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
        } else {
            checkNotificationPermission()
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app requires 'Display over other apps' permission to function properly.")
            .setPositiveButton("Grant Permission") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            }
            .setNegativeButton("Exit App") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission()
            } else {
                checkLocationPermission()
            }
        } else {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    // Start services and restart app
                    startServices()
                    //restartApp()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    showLocationPermissionDialog()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun showLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app requires location permission to function properly.")
            .setPositiveButton("Grant Permission") { _, _ ->
                checkLocationPermission()
            }
            .setNegativeButton("Exit App") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }
    }

    private fun startServices() {
        startService(Intent(this, FloatingHeadService::class.java))
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                checkLocationPermission()
            }
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 100
        private const val NOTIFICATION_PERMISSION_CODE = 101
        private const val BATTERY_OPTIMIZATION_REQUEST_CODE = 102
        private const val TAG = "MainActivity"
    }
}