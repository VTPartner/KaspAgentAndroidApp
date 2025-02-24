package com.vtpartnertranspvtltd.vtpartneragent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.network.ApiService
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.vtpartnertranspvtltd.vtpartneragent.utils.LocaleHelper

class VTPartnerApp : Application() {
    lateinit var apiService: ApiService
    lateinit var preferenceManager: PreferenceManager

    override fun attachBaseContext(base: Context) {
        // Use the base context directly with LocaleHelper
        val prefs = base.getSharedPreferences("VTPartnerPrefs", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("app_language", "en") ?: "en"
        super.attachBaseContext(LocaleHelper.setLocale(base, languageCode))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupDependencies()
        // Initialize Firebase first
        FirebaseApp.initializeApp(this)
        createNotificationChannel()
        
        // Now it's safe to use PreferenceManager
        val languageCode = PreferenceManager.getInstance(this).getLanguage() ?: "en"
        LocaleHelper.setLocale(this, languageCode)
    }

    private fun setupDependencies() {
        // Setup Retrofit
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Initialize PreferenceManager
        preferenceManager = PreferenceManager.getInstance(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FLOATING_SERVICE_CHANNEL_ID,
                "Floating Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for floating service notifications"
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val FLOATING_SERVICE_CHANNEL_ID = "floating_service"
        lateinit var instance: VTPartnerApp
            private set
    }
} 