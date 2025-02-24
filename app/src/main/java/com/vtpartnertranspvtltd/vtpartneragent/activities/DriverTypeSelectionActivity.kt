package com.vtpartnertranspvtltd.vtpartneragent.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityDriverTypeSelectionBinding
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

class DriverTypeSelectionActivity : BaseActivity() {
    private var backPressedTime: Long = 0
    private lateinit var binding: ActivityDriverTypeSelectionBinding
    private val preferenceManager by lazy { PreferenceManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullScreen()
        binding = ActivityDriverTypeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)


        requestNotificationPermission()

        // Apply animations to cards
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        
        binding.apply {
            goodsDriverCard.startAnimation(fadeIn)
            cabDriverCard.startAnimation(fadeIn)
            jcbProviderCard.startAnimation(fadeIn)
            onlyDriverCard.startAnimation(fadeIn)
            handymanCard.startAnimation(fadeIn)
            
            // Add click listeners with ripple effect
            goodsDriverCard.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    preferenceManager.saveDriverType(DriverType.GOODS_DRIVER.name)
                    val goodsDriverID = preferenceManager.getStringValue("goods_driver_id")
                    val goodsDriverName = preferenceManager.getStringValue("goods_driver_name")
                    if(goodsDriverID != null && goodsDriverID.isNotEmpty() && goodsDriverName != null && goodsDriverName.isNotEmpty() && !goodsDriverName.contentEquals("NA")){
                        startActivity(
                            Intent(
                                this@DriverTypeSelectionActivity,
                                GoodsDriverHomeActivity::class.java
                            )
                        )
                    }else {
                        startActivity(
                            Intent(
                                this@DriverTypeSelectionActivity,
                                LoginActivity::class.java
                            )
                        )
                    }
//                    finish()
                }.start()
            }
            
            cabDriverCard.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    preferenceManager.saveDriverType(DriverType.CAB_DRIVER.name)
                    Toast.makeText(this@DriverTypeSelectionActivity,"Please check the Goods Driver implementation first",Toast.LENGTH_LONG).show()
//                    startActivity(Intent(this@DriverTypeSelectionActivity, LoginActivity::class.java))
//                    finish()
                }.start()
            }
            
            jcbProviderCard.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    preferenceManager.saveDriverType(DriverType.JCB_PROVIDER.name)
                    Toast.makeText(this@DriverTypeSelectionActivity,"Please check the Goods Driver implementation first",Toast.LENGTH_LONG).show()
//                    startActivity(Intent(this@DriverTypeSelectionActivity, LoginActivity::class.java))
//                    finish()
                }.start()
            }
            
            onlyDriverCard.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    preferenceManager.saveDriverType(DriverType.ONLY_DRIVER.name)
                    Toast.makeText(this@DriverTypeSelectionActivity,"Please check the Goods Driver implementation first",Toast.LENGTH_LONG).show()
//                    startActivity(Intent(this@DriverTypeSelectionActivity, LoginActivity::class.java))
//                    finish()
                }.start()
            }
            
            handymanCard.setOnClickListener {
                it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    preferenceManager.saveDriverType(DriverType.HANDYMAN.name)
                    Toast.makeText(this@DriverTypeSelectionActivity,"Please check the Goods Driver implementation first",Toast.LENGTH_LONG).show()
//                    startActivity(Intent(this@DriverTypeSelectionActivity, LoginActivity::class.java))
//                    finish()
                }.start()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
        } else {
            showSnackbar("Press back once again to exit")
            backPressedTime = System.currentTimeMillis()
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }

    enum class DriverType {
        GOODS_DRIVER,
        CAB_DRIVER,
        JCB_PROVIDER,
        ONLY_DRIVER,
        HANDYMAN
    }
} 