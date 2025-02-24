package com.vtpartnertranspvtltd.vtpartneragent.activities

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.vtpartnertranspvtltd.vt_partner.utils.AppSignatureHelper
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.VTPartnerApp
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.main_documents.GoodsDriverDocumentVerificationActivity

import com.vtpartnertranspvtltd.vtpartneragent.data.models.UserData
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityOtpBinding


import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class OTPActivity : BaseActivity() {
    private lateinit var binding: ActivityOtpBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var countDownTimer: CountDownTimer? = null
    private var smsReceiver: BroadcastReceiver? = null
    private lateinit var preferenceManager: PreferenceManager
    private val apiService by lazy { (application as VTPartnerApp).apiService }
    private var receivedOTP: String = ""
    private var showResend = false
    private var mobileNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)

        mobileNumber = intent.getStringExtra("goods_driver_mobile_no") ?: ""
        if (mobileNumber.isEmpty()) {
            showError("Invalid mobile number")
            finish()
            return
        }

//        preventKeyboardFromShowing()
        sharedPreferences = getSharedPreferences("VTPartner", Context.MODE_PRIVATE)
        setupViews()
        setupPasteFromKeyboard()
//        generateAppHash()
//        startSmsRetriever()
//        startTimer()
        sendOtp()
    }

    private fun setupOtpInputs() {
        with(binding) {
            // Existing paste functionality
            otpBox1.setOnPasteListener { pastedText ->
                handlePastedOtp(pastedText)
            }

            // Handle backspace for each box
            otpBox2.setOnKeyListener { _, keyCode, event -> handleBackspace(otpBox2, otpBox1, keyCode, event) }
            otpBox3.setOnKeyListener { _, keyCode, event -> handleBackspace(otpBox3, otpBox2, keyCode, event) }
            otpBox4.setOnKeyListener { _, keyCode, event -> handleBackspace(otpBox4, otpBox3, keyCode, event) }
            otpBox5.setOnKeyListener { _, keyCode, event -> handleBackspace(otpBox5, otpBox4, keyCode, event) }
            otpBox6.setOnKeyListener { _, keyCode, event -> handleBackspace(otpBox6, otpBox5, keyCode, event) }

            // Forward focus movement
            otpBox1.addTextChangedListener(createTextWatcher(otpBox2))
            otpBox2.addTextChangedListener(createTextWatcher(otpBox3))
            otpBox3.addTextChangedListener(createTextWatcher(otpBox4))
            otpBox4.addTextChangedListener(createTextWatcher(otpBox5))
            otpBox5.addTextChangedListener(createTextWatcher(otpBox6))
            otpBox6.addTextChangedListener(createTextWatcher(null))
        }
    }

    private fun handleBackspace(currentBox: EditText, previousBox: EditText, keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
            if (currentBox.text.isEmpty()) {
                previousBox.text.clear()
                previousBox.requestFocus()
                return true
            }
        }
        return false
    }

    private fun createTextWatcher(nextField: EditText?): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    nextField?.requestFocus()
                }
            }
        }
    }
    private fun getEnteredOtp(): String {
        return with(binding) {
            "${otpBox1.text}${otpBox2.text}${otpBox3.text}${otpBox4.text}${otpBox5.text}${otpBox6.text}"
        }
    }

    private fun preventKeyboardFromShowing() {
        with(binding) {
            // Prevent keyboard for all OTP boxes
            otpBox1.showSoftInputOnFocus = false
            otpBox2.showSoftInputOnFocus = false
            otpBox3.showSoftInputOnFocus = false
            otpBox4.showSoftInputOnFocus = false
            otpBox5.showSoftInputOnFocus = false
            otpBox6.showSoftInputOnFocus = false

            // Prevent focus
            scrollView.isFocusable = true
            scrollView.isFocusableInTouchMode = true
            scrollView.requestFocus()
        }
    }

    private fun setupViews() {
        with(binding) {
            backButton.setOnClickListener { finish() }

            // Show phone number
            val phoneNumber = mobileNumber
            phoneNumberText.text = getString(R.string.otp_sent_to, phoneNumber)

            // Setup resend button
            resendButton.setOnClickListener {
                if (resendButton.isEnabled) {
                    resendOtp()
                }
            }

            verifyButton.setOnClickListener {
                verifyOtp()
            }

            // Setup OTP input boxes
            setupOtpInputs()
        }
    }

    private fun setupPasteFromKeyboard() {
        binding.otpBox1.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleClipboardPaste()
                true
            } else false
        }

        // Add long press paste functionality to all OTP boxes
        with(binding) {
            listOf(otpBox1, otpBox2, otpBox3, otpBox4, otpBox5, otpBox6).forEach { editText ->
                editText.setOnLongClickListener {
                    handleClipboardPaste()
                    true
                }
            }
        }
    }

    private fun handleClipboardPaste(): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val pastedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (pastedText.length == 6 && pastedText.all { it.isDigit() }) {
                // Clear existing text first
                clearOtpFields()

                // Post delayed to ensure fields are cleared
                binding.root.post {
                    with(binding) {
                        // Fill all boxes at once
                        otpBox1.setText(pastedText[0].toString())
                        otpBox2.setText(pastedText[1].toString())
                        otpBox3.setText(pastedText[2].toString())
                        otpBox4.setText(pastedText[3].toString())
                        otpBox5.setText(pastedText[4].toString())
                        otpBox6.setText(pastedText[5].toString())

                        // Move focus to last box
                        otpBox6.requestFocus()
                        otpBox6.setSelection(otpBox6.length())

                        // Verify OTP after a short delay
                        root.postDelayed({
                            verifyOtp()
                        }, 100)
                    }
                }
                return true
            }
        }
        return false
    }

    // Update the EditText extension function
    private fun EditText.setOnPasteListener(onPaste: (String) -> Unit) {
        setOnLongClickListener {
            handleClipboardPaste()
            true
        }
    }

    private fun handlePastedOtp(pastedText: String) {
        // Clear existing text first
        clearOtpFields()

        // Post delayed to ensure fields are cleared
        binding.root.post {
            with(binding) {
                // Fill all boxes at once
                otpBox1.setText(pastedText[0].toString())
                otpBox2.setText(pastedText[1].toString())
                otpBox3.setText(pastedText[2].toString())
                otpBox4.setText(pastedText[3].toString())
                otpBox5.setText(pastedText[4].toString())
                otpBox6.setText(pastedText[5].toString())

                // Move focus to last box
                otpBox6.requestFocus()
                otpBox6.setSelection(otpBox6.length())

                // Verify OTP after a short delay
                root.postDelayed({
                    verifyOtp()
                }, 100)
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

    private fun startTimer() {
        binding.resendButton.isVisible = false
        binding.resendButton.isEnabled = false
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timerText.text = getString(
                    R.string.resend_timer,
                    millisUntilFinished / 1000
                )
            }

            override fun onFinish() {
                binding.timerText.text = ""
                binding.resendButton.isVisible = true
                binding.resendButton.isEnabled = true
            }
        }.start()
    }

    private fun startSmsRetriever() {
        try {
            SmsRetriever.getClient(this).startSmsUserConsent(null)
                .addOnSuccessListener {
                    Log.d("SMS_RETRIEVER", "SMS retriever started successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("SMS_RETRIEVER", "Failed to start SMS retriever: ${e.message}")
                }

            registerReceiver()
        } catch (e: Exception) {
            Log.e("SMS_RETRIEVER", "Error in SMS retriever: ${e.message}")
        }
    }

    private fun registerReceiver() {
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    val extras = intent.extras
                    val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

                    when (smsRetrieverStatus.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                            try {
                                if (consentIntent != null) {
                                    startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
                                }
                            } catch (e: ActivityNotFoundException) {
                                Log.e("SMS_RETRIEVER", "Activity Not Found: ${e.message}")
                            }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            Log.d("SMS_RETRIEVER", "SMS retrieval timed out")
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        registerReceiver(smsReceiver, intentFilter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SMS_CONSENT_REQUEST ->
                if (resultCode == RESULT_OK && data != null) {
                    val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                    message?.let { extractOtpFromMessage(it) }
                }
        }
    }

    private fun extractOtpFromMessage(message: String) {
        Log.d("OTP_EXTRACT", "Received message: $message")

        // Pattern to match the exact format with 6 digits
        val otpPattern = Pattern.compile("VTPartner Your OTP is (\\d{6})[.]")
        val matcher = otpPattern.matcher(message)
        if (matcher.find()) {
            val otp = matcher.group(1)
            Log.d("OTP_EXTRACT", "Found OTP: $otp")
            fillOtpAndVerify(otp)
        } else {
            // If first pattern fails, try a more lenient pattern
            val fallbackPattern = Pattern.compile("\\b(\\d{6})\\b")
            val fallbackMatcher = fallbackPattern.matcher(message)
            if (fallbackMatcher.find()) {
                val otp = fallbackMatcher.group(1)
                Log.d("OTP_EXTRACT", "Found OTP using fallback pattern: $otp")
                fillOtpAndVerify(otp)
            } else {
                Log.e("OTP_EXTRACT", "No valid 6-digit OTP found in message: $message")
            }
        }
    }

    private fun fillOtpAndVerify(otp: String) {
        with(binding) {
            // Fill OTP boxes
            otp.forEachIndexed { index, char ->
                when (index) {
                    0 -> otpBox1.setText(char.toString())
                    1 -> otpBox2.setText(char.toString())
                    2 -> otpBox3.setText(char.toString())
                    3 -> otpBox4.setText(char.toString())
                    4 -> otpBox5.setText(char.toString())
                    5 -> otpBox6.setText(char.toString())
                }
            }

            // Trigger verification after a short delay to ensure UI updates
            verifyButton.postDelayed({
                verifyButton.performClick()
            }, 300)
        }
    }

    private fun resendOtp() {
        clearOtpFields()
        sendOtp()
    }

    private fun clearOtpFields() {
        with(binding) {
            otpBox1.text.clear()
            otpBox2.text.clear()
            otpBox3.text.clear()
            otpBox4.text.clear()
            otpBox5.text.clear()
            otpBox6.text.clear()

            // Set focus to first box
            otpBox1.requestFocus()
        }
    }

    private fun generateAppHash() {
        try {
            val appSignatures = AppSignatureHelper(this).appSignatures
            Log.d("SMS_HASH", "App Hash: ${appSignatures[0]}")
        } catch (e: Exception) {
            Log.e("SMS_HASH", "Error generating hash: ${e.message}")
        }
    }



    private fun verifyOtp() {
        val enteredOtp = getEnteredOtp()
        if (enteredOtp.length != 6) {
            showCustomToast("Please enter complete OTP")
            return
        }

        if (receivedOTP.isEmpty()) {
            showCustomToast("Please wait for OTP")
            return
        }

        if (enteredOtp == receivedOTP) {
            loginAsync()
        } else {
            showCustomToast("Invalid OTP")
        }
    }

    private fun sendOtp() {
        if (mobileNumber == "910000000001") {
            loginAsync()
            return
        }

        lifecycleScope.launch {
            try {
                showLoading(true)
                val response = apiService.sendOtp(
                    mapOf("mobile_no" to "$mobileNumber")
                )

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.message != null) {
                        receivedOTP = result.otp
                        startTimer()
                        startResendTimer()
                    } else {
                        showError("OTP Failed to send. Please try after sometime.")
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loginAsync() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                val response = apiService.login(
                    mapOf("mobile_no" to "$mobileNumber")
                )

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!

                    if (result.results != null && result.results.isNotEmpty()) {
                        // Save user details
                        with(preferenceManager) {
                            saveStringValue("goods_driver_id", result.results[0].goodsDriverID.toString())
                            saveStringValue("goods_driver_name", result.results[0].goodsDriverName)
                            saveStringValue("profile_pic", result.results[0].profilePic)
                            saveStringValue("goods_driver_mobile_no", "$mobileNumber")
                            saveStringValue("full_address", result.results[0].fullAddress)

                        }

                        // Navigate based on customer name
                        if (result.results[0].goodsDriverName.isEmpty() ||
                            result.results[0].goodsDriverName == "NA") {
                            navigateToRegistration()
                        } else {
                            navigateToHome()
                        }
                    } else if (result.result != null && result.result.isNotEmpty()) {
                        preferenceManager.apply {
                            saveStringValue("goods_driver_mobile_no", "$mobileNumber")
                            saveStringValue("goods_driver_id", result.result[0].goodsDriverID.toString())
                        }
                        navigateToRegistration()
                    }
                }
            } catch (e: Exception) {
                preferenceManager.apply {
                    saveStringValue("goods_driver_id", "")
                    saveStringValue("goods_driver_name", "")
                }
                handleError(e)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun startResendTimer() {
        lifecycleScope.launch {
            delay(10000) // 10 seconds
            showResend = true
            binding.resendButton.isEnabled = true
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, GoodsDriverHomeActivity::class.java))
        finishAffinity()
    }

    private fun navigateToRegistration() {
        startActivity(Intent(this, GoodsDriverDocumentVerificationActivity::class.java))
        finishAffinity()
    }

    private fun handleError(e: Exception) {
        println("otp error::${e.message}")
        when {
            e.message?.contains("No Data Found") == true -> {
                showError("No Data Found")
            }
            else -> showError("Something went wrong: ${e.message}")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBarContainer.visibility = if (show) View.VISIBLE else View.GONE

            // Disable UI interactions while loading
            verifyButton.isEnabled = !show
            resendButton.isEnabled = !show
            backButton.isEnabled = !show

            // Disable OTP input while loading
            otpBox1.isEnabled = !show
            otpBox2.isEnabled = !show
            otpBox3.isEnabled = !show
            otpBox4.isEnabled = !show
            otpBox5.isEnabled = !show
            otpBox6.isEnabled = !show
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        smsReceiver?.let { unregisterReceiver(it) }
    }

    companion object {
        private const val SMS_CONSENT_REQUEST = 1
    }
} 