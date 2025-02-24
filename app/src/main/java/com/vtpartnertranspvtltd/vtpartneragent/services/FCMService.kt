package com.vtpartnertranspvtltd.vtpartneragent.services

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import com.vtpartnertranspvtltd.vtpartneragent.R
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.app.ActivityManager
import android.app.Notification
import android.graphics.Color
import android.media.MediaPlayer
import android.os.PowerManager
import android.media.RingtoneManager
import android.view.WindowManager
import androidx.core.app.ActivityCompat.finishAffinity
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.messaging.FirebaseMessaging
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.MainActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.DriverTypeSelectionActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.SplashActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.notification_dialog.BookingDialogActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.notification_dialog.NotificationReceiverActivity
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject


class FCMService : FirebaseMessagingService() {
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var notificationManager: NotificationManager
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createBookingChannel()
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.booking_notification)
        mediaPlayer?.isLooping = true
    }

    private fun playNotificationSound() {
        try {
            mediaPlayer?.start()
            // Stop sound after 15 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                stopNotificationSound()
            }, 15000)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound: ${e.message}")
        }
    }

    private fun stopNotificationSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                prepare()
            }
        }
    }





    override fun onNewToken(token: String) {
        super.onNewToken(token)
//        preferenceManager.saveFcmToken(token)
        // Send token to your server
        updateDriverAuthToken(token)
    }

    private fun updateDriverAuthToken(deviceToken: String) {
        Log.d("FCMNewTokenFound", "updating goods driver authToken")


        val driverId = preferenceManager.getStringValue("goods_driver_id")
        if (driverId.isEmpty()) {
            return
        }
        if (deviceToken.isNullOrEmpty()) {

            return
        }

        // Get server key token
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverToken = AccessToken.getAccessToken()

                val url = "${Constants.BASE_URL}update_firebase_goods_driver_token"

                val jsonBody = JSONObject().apply {
                    put("goods_driver_id", driverId)
                    put("authToken", deviceToken)
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

                VolleySingleton.getInstance(this@FCMService)
                    .addToRequestQueue(request)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        Log.d(TAG, "Received FCM message: $data")

        when (data["intent"]) {
            "driver" -> {
                val bookingId = data["booking_id"]
                Log.d(TAG, "Notification For Pickup booking_id: $bookingId")
                fetchBookingDetails(bookingId)


            }


            else -> showRegularNotification(data)
        }
    }

    private fun showBookingRequestNotification(bookingId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create pending intent for notification click
        val intent = Intent(this, NotificationReceiverActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("booking_id", bookingId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, "booking_requests")
            .setContentTitle("New Booking Request")
            .setContentText("Tap to view booking details")
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        // Show the notification
        notificationManager.notify(bookingId.hashCode(), notification)
    }



    // Update fetchBookingDetails to work with the full screen notification
    private fun fetchBookingDetails(bookingId: String?) {
        if (bookingId == null) return
//playNotificationSound()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverToken = AccessToken.getAccessToken()
                val url = "${Constants.BASE_URL}booking_details_for_ride_acceptance"

                val jsonBody = JSONObject().apply {
                    put("booking_id", bookingId)
                }

                val request = object : JsonObjectRequest(
                    Method.POST,
                    url,
                    jsonBody,
                    { response ->
                        try {
                            val results = response.optJSONArray("results")
                            if (results != null && results.length() > 0) {
                                val bookingDetails = results.getJSONObject(0)
                                showBookingDialog(bookingDetails, bookingId)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing booking details: ${e.message}")
                        }
                    },
                    { error ->
                        Log.e(TAG, "Error fetching booking details: ${error.message}")
                    }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        return HashMap<String, String>().apply {
                            put("Content-Type", "application/json")
                            put("Authorization", "Bearer $serverToken")
                        }
                    }
                }

                VolleySingleton.getInstance(this@FCMService)
                    .addToRequestQueue(request)

            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchBookingDetails: ${e.message}")
            }
        }
    }


    private fun showBookingDialog(bookingDetails: JSONObject, bookingId: String) {
        // Create an intent that will show the dialog
        val intent = Intent(this, BookingDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("booking_details", bookingDetails.toString())
            putExtra("booking_id", bookingId)
        }

        if (isAppInForeground()) {
            // App is in foreground, show dialog directly
            startActivity(intent)

        } else {
            // App is in background, show notification
            showBookingNotification(bookingDetails, bookingId)
        }
    }



    private fun showBookingNotification(bookingDetails: JSONObject, bookingId: String) {
        val channelId = "booking_notifications"
        val notificationId = System.currentTimeMillis().toInt()

        // Create intent for notification click
        val intent = Intent(this, BookingDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("booking_details", bookingDetails.toString())
            putExtra("booking_id", bookingId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Booking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("New Booking Request")
            .setContentText("From: ${bookingDetails.optString("pickup_address")}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("From: ${bookingDetails.optString("pickup_address")}\nTo: ${bookingDetails.optString("drop_address")}")
            )
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    private fun showNotificationWithFullScreenIntent(data: Map<String, String>) {
        Log.d(TAG,"showNotificationWithFullScreenIntent:$data")
        try {
            // Create full screen intent
            val fullScreenIntent = Intent(this, BookingAcceptanceService::class.java).apply {
                action = "SHOW_BOOKING"
                putExtra("booking_id", data["booking_id"])
                putExtra("booking_details", data["message"])
                putExtra("force_show", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                // Add these flags for screen wake
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            }

            // Wake device
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "FCMService:WakeLock"
            )
            wakeLock.acquire(30*1000L) // 30 seconds

            // Create pending intent
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getService(
                    this,
                    0,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    this,
                    0,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Build notification
            val notification = NotificationCompat.Builder(this, BOOKING_CHANNEL_ID)
                .setContentTitle(data["title"] ?: "New Booking Request")
                .setContentText(data["message"] ?: "Tap to view booking details")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(true)
                .setTimeoutAfter(30000)
                .setSound(null) // We're using custom sound
                .setVibrate(longArrayOf(0, 500, 1000))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            // Show notification
            notificationManager.notify(
                data["booking_id"]?.hashCode() ?: System.currentTimeMillis().toInt(),
                notification
            )

            // Play sound
            playNotificationSound()

            // Start service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(fullScreenIntent)
            } else {
                startService(fullScreenIntent)
            }

            // Release wake lock after delay
            Handler(Looper.getMainLooper()).postDelayed({
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }, 30000) // 30 seconds

        } catch (e: Exception) {
            Log.e(TAG, "Error showing full screen notification: ${e.message}")
            e.printStackTrace()
        }
    }

    // Update createBookingChannel to include screen wake settings
    private fun createBookingChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BOOKING_CHANNEL_ID,
                "Booking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new bookings"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 1000)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                importance = NotificationManager.IMPORTANCE_HIGH
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    // Helper extension function to convert JSONObject to Map
    private fun JSONObject.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        this.keys().forEach { key ->
            map[key] = this.optString(key, "")
        }
        return map
    }

    private fun saveNotificationData(data: Map<String, String>) {
        preferenceManager.apply {
            saveStringValue("last_booking_id", data["booking_id"] ?: "")
            saveStringValue("last_booking_title", data["title"] ?: "")
            saveStringValue("last_booking_message", data["message"] ?: "")
            saveStringValue("last_booking_timestamp", System.currentTimeMillis().toString())
            // Save other relevant data fields
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName

        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcess.processName == packageName
            ) {
                // Check if our activity is actually visible
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val tasks = activityManager.getRunningTasks(1)
                if (!tasks.isEmpty()) {
                    val topActivity = tasks[0].topActivity
                    if (topActivity?.packageName == packageName) {
                        return true
                    }
                }
            }
        }
        return false
    }



    private fun showRegularNotification(title: String, message: String, data: Map<String, String>) {
        // Create notification channel (keep existing channel code)

        // Create intent based on data["intent"]
        val intent = when (data["intent"]) {
            "driver" -> {
                fetchBookingDetails(data["booking_id"])
                Intent(this, BookingDialogActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("booking_id", data["booking_id"])
                    putExtra("intent", "driver")

                    // Add any other specific extras needed for driver intent
                }
            }
            else -> {
                Intent(this, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    // Add general data extras
                    data.forEach { (key, value) ->
                        putExtra(key, value)
                    }
                }
            }
        }

        // Create unique request code based on booking_id if available
        val requestCode = data["booking_id"]?.hashCode() ?: 0

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build and show notification
        val notification = NotificationCompat.Builder(this, BOOKING_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = data["booking_id"]?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun showRegularNotification(data: Map<String, String>) {
        val intent = Intent(this, DriverTypeSelectionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, BOOKING_CHANNEL_ID)
            .setContentTitle(data["title"] ?: "")
            .setContentText(data["message"] ?: "")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNotificationSound()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    companion object {
        private const val TAG = "FCMService"
        private const val BOOKING_CHANNEL_ID = "booking_notifications"
    }
}