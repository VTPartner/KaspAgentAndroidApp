package com.vtpartnertranspvtltd.vtpartneragent.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivitySplashBinding
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullScreen()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)
        // Start animations
        startAnimations()

        // Navigate to login after delay
        lifecycleScope.launch {
            delay(1000) // 1 seconds delay

            startActivity(Intent(this@SplashActivity, DriverTypeSelectionActivity::class.java))
//            startActivity(Intent(this@SplashActivity, GoodsDriverHomeActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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

    private fun startAnimations() {
        // Fade in animations
        val fadeInLogo = createFadeInAnimator(binding.logoImage)
        val fadeInName = createFadeInAnimator(binding.appNameText)
        val fadeInSlogan = createFadeInAnimator(binding.sloganText)
        val fadeInCopyright = createFadeInAnimator(binding.copyrightText)

        // Scale animations for logo
        val scaleXLogo = ObjectAnimator.ofFloat(binding.logoImage, View.SCALE_X, 0.8f, 1f)
        val scaleYLogo = ObjectAnimator.ofFloat(binding.logoImage, View.SCALE_Y, 0.8f, 1f)

        // Combine logo animations
        val logoAnimSet = AnimatorSet().apply {
            playTogether(fadeInLogo, scaleXLogo, scaleYLogo)
        }

        // Play all animations in sequence
        AnimatorSet().apply {
            playSequentially(
                logoAnimSet,
                fadeInName,
                fadeInSlogan,
                fadeInCopyright
            )
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun createFadeInAnimator(view: View): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            duration = 500
        }
    }
} 