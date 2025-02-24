package com.vtpartnertranspvtltd.vtpartneragent.activities


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityLoginBinding

import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var phoneUtil: PhoneNumberUtil
    private var backPressedTime: Long = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullScreen()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("VTPartner", Context.MODE_PRIVATE)
        phoneUtil = PhoneNumberUtil.createInstance(this)
        setupViews()
        setupKeyboardBehavior()
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

    private fun setupKeyboardBehavior() {
        binding.phoneInput.apply {
            clearFocus()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (validatePhoneNumber()) {
                        hideKeyboard()
                    }
                }
            })
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }

    private fun setupViews() {
        with(binding) {
            backButton.setOnClickListener { finish() }

            countryCodePicker.apply {
                setDefaultCountryUsingNameCode("IN")
                setOnCountryChangeListener {
                    if (selectedCountryNameCode != "IN") {
                        showCustomToast(getString(R.string.country_not_available))
                        setDefaultCountryUsingNameCode("IN")
                    }
                    validatePhoneNumber()
                }
            }

            continueButton.setOnClickListener {
                if (validatePhoneNumber()) {
                    hideKeyboard()
                    val phoneNumber = "+${countryCodePicker.selectedCountryCode}${phoneInput.text}"
                    savePhoneNumber(phoneNumber)
                    navigateToOtpScreen(phoneNumber)
                }
            }
        }
    }

    private fun showCustomToast(message: String) {
        val layout = layoutInflater.inflate(R.layout.custom_toast_layout, null)
        layout.findViewById<TextView>(R.id.toastText).text = message

        Toast(applicationContext).apply {
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }

    private fun validatePhoneNumber(): Boolean {
        val phoneNumber = binding.phoneInput.text.toString()

        return try {
            val number = phoneUtil.parse(phoneNumber, binding.countryCodePicker.selectedCountryNameCode)
            val isValid = phoneUtil.isValidNumber(number)

            binding.phoneInputLayout.apply {
                error = if (isValid) null else getString(R.string.invalid_phone)
                isErrorEnabled = !isValid
            }
            isValid
        } catch (e: NumberParseException) {
            binding.phoneInputLayout.apply {
                error = getString(R.string.invalid_phone)
                isErrorEnabled = true
            }
            false
        }
    }

    private fun savePhoneNumber(phoneNumber: String) {
        sharedPreferences.edit().putString("goods_driver_mobile_no", phoneNumber).apply()
    }

    private fun navigateToOtpScreen(phoneNumber:String) {
        startActivity(Intent(this, OTPActivity::class.java).apply {
            putExtra("goods_driver_mobile_no", phoneNumber)

        })

    }

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
        } else {
            showSnackbar("Press back once again to exit")
            backPressedTime = System.currentTimeMillis()
        }
    }
} 