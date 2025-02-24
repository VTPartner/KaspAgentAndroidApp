package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener

import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.GoodsDriverHomeActivity
import com.vtpartnertranspvtltd.vtpartneragent.adapters.RechargeAdapter
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverRechargeHomeBinding
import com.vtpartnertranspvtltd.vtpartneragent.databinding.BottomSheetRechargeBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.RechargeModel
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoodsDriverRechargeHomeActivity : AppCompatActivity(), PaymentResultListener {
    private lateinit var binding: ActivityGoodsDriverRechargeHomeBinding
    private var allRechargesList = mutableListOf<RechargeModel>()
    private var selectedIndex = 0
    private var currentBalance = "0"
    private var topUpId = "0"
    private var lastValidityDate = "0"
    private lateinit var checkout: Checkout
    private lateinit var preferenceManager : PreferenceManager
    private var hasNegativePoints = false
    private var isExpired = false
    private var previousNegativePoints = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverRechargeHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager.getInstance(this)
//        setupToolbar()
        setupUI()
        initRazorpay()
        setupRecyclerView()
        fetchCurrentBalance()
        fetchAllRecharges()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.apply {
                title = "Recharge"
                setSupportActionBar(this)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                // Handle navigation click directly on toolbar
                setNavigationOnClickListener {
                    finish()
                }
            }
        }
    }
    private fun initRazorpay() {
        Checkout.preload(applicationContext)
        checkout = Checkout()
        checkout.setKeyID(Constants.RAZORPAY_KEY)
    }



    private fun fetchAllRecharges() {
        showLoading()

        val jsonObject = JSONObject().apply {
            put("category_id", "1")
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "${Constants.BASE_URL}get_goods_driver_recharge_list",
            jsonObject,
            { response ->
                hideLoading()
                try {
                    if (response.has("results")) {
                        val rechargesArray = response.getJSONArray("results")
                        val rechargesList = mutableListOf<RechargeModel>()

                        for (i in 0 until rechargesArray.length()) {
                            val rechargeJson = rechargesArray.getJSONObject(i)
                            rechargesList.add(RechargeModel.fromJson(rechargeJson))
                        }

                        allRechargesList.clear()
                        allRechargesList.addAll(rechargesList)
                        updateRecyclerView(rechargesList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    handleError(e)
                }
            },
            { error ->
                hideLoading()
                handleError(error)
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun fetchCurrentBalance() {
        showLoading()


        val driverId = preferenceManager.getStringValue("goods_driver_id")

        val jsonObject = JSONObject().apply {
            put("driver_id", driverId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "${Constants.BASE_URL}get_goods_driver_current_recharge_details",
            jsonObject,
            { response ->
                hideLoading()
                try {
                    if (response.has("results")) {
                        val results = response.getJSONArray("results")
                        if (results.length() > 0) {
                            val result = results.getJSONObject(0)

                            currentBalance = result.getString("remaining_points")
                            topUpId = result.getString("topup_id")
                            lastValidityDate = result.getString("valid_till_date")

                            val negativePoints = result.optDouble("negative_points", 0.0)
                            if (negativePoints > 0) {
                                currentBalance = negativePoints.toString()
                                previousNegativePoints = currentBalance.toDouble()
                                currentBalance = "-$currentBalance"
                            }

                            // Parse and compare dates
                            val validTillDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .parse(result.getString("valid_till_date"))
                            val currentDate = Calendar.getInstance().time

                            if (validTillDate != null && validTillDate.before(currentDate)) {
                                if (hasNegativePoints) {
                                    currentBalance = "-${previousNegativePoints}"
                                    isExpired = true
                                    showSnackBar("Your previous plan has expired. Please recharge promptly to continue receiving ride requests.")
                                }
                            }

                            // Update UI
                            binding.balanceText.text = "₹ $currentBalance"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (e.message?.contains("No Data Found") == true) {
                        showSnackBar("Not Yet Subscribed to any Top-Up Recharge plan.")
                    } else {
                        handleError(e)
                    }
                }
            },
            { error ->
                hideLoading()
                handleError(error)
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun getDriverId(): String {
        val goods_driver_id =  preferenceManager.getStringValue("goods_driver_id")
        return goods_driver_id

    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Recharge Plans"
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { finish() }
        }
    }



    private fun setupRecyclerView() {
        binding.rechargesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GoodsDriverRechargeHomeActivity)
            adapter = RechargeAdapter { position ->
                handleRechargeClick(position)
            }
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun updateRecyclerView(newRecharges: List<RechargeModel>) {
        (binding.rechargesRecyclerView.adapter as? RechargeAdapter)?.let { adapter ->
            adapter.updateData(newRecharges)
        }
    }

    private fun handleRechargeClick(position: Int) {
        val recharge = allRechargesList[position]
        val absoluteBalance = if (currentBalance.startsWith("-")) {
            currentBalance.substring(1).toDouble()
        } else {
            currentBalance.toDouble()
        }

        when {
            absoluteBalance > recharge.points.toDouble() -> {
                showSnackBar("Please select a pack that covers your negative balance.")
            }
            currentBalance.toDouble() <= 0 -> {
                showPaymentBottomSheet(position)
            }
            else -> {
                showSnackBar("Multiple top-up recharges are not allowed.")
            }
        }
    }

    private fun showPaymentBottomSheet(index: Int) {
        val bottomSheet = BottomSheetDialog(this)
        val sheetBinding = BottomSheetRechargeBinding.inflate(layoutInflater)

        sheetBinding.apply {
            planPrice.text = "₹${allRechargesList[index].amount}"
            validity.text = "${allRechargesList[index].validDays} Days"

            payNowButton.setOnClickListener {
                selectedIndex = index
                startRazorpayPayment(index)
                bottomSheet.dismiss()
            }
        }

        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }


    private fun saveRechargeDetails(paymentId: String) {
        showLoading()

        val recharge = allRechargesList[selectedIndex]
        val validDays = recharge.validDays.toInt()
        val validTillDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, validDays)
        }.time.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
        }

        val jsonObject = JSONObject().apply {
            put("driver_id", getDriverId())
            put("recharge_id", recharge.rechargeId)
            put("topup_id", topUpId)
            put("last_validity_date", lastValidityDate)
            put("amount", recharge.amount)
            put("allotted_points", recharge.points)
            put("valid_till_date", validTillDate)
            put("payment_method", "RazorPay")
            put("payment_id", paymentId)
            put("previous_negative_points", previousNegativePoints)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "${Constants.BASE_URL}new_goods_driver_recharge",
            jsonObject,
            {
                hideLoading()
                navigateToHome()
            },
            { error ->
                hideLoading()
                handleError(error)
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun showSuccessDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun navigateToHome() {
        // Clear activity stack and go to home
        Intent(this, GoodsDriverHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finishAffinity()
    }

    private fun handleError(error: Exception) {
        hideLoading()
        val errorMessage = when (error) {
            is com.android.volley.NoConnectionError -> "No internet connection"
            is com.android.volley.TimeoutError -> "Request timed out"
            is com.android.volley.ServerError -> "Server error"
            else -> "An error occurred: ${error.message}"
        }
        showSnackBar(errorMessage)
    }



    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun startRazorpayPayment(index: Int) {
        try {
            val options = JSONObject().apply {
                put("key", Constants.RAZORPAY_KEY)
                // Convert amount to smallest currency unit (paise)
                put("amount", (allRechargesList[index].amount.toDouble() * 100).toInt())
                put("currency", "INR")
                put("name", "VT Partner Trans Pvt Ltd")
                put("description", "Order Payment")
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 1)
                })
                put("send_sms_hash", true)
                put("prefill", JSONObject().apply {
                    put("contact", "")
                    put("email", "test@razorpay.com")
                })
                put("theme", JSONObject().apply {
                    put("color", "#0042D9")
                })
                put("external", JSONObject().apply {
                    put("wallets", JSONArray().put("paytm"))
                })
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackBar("Error in payment: ${e.message}")
        }
    }

    override fun onPaymentSuccess(paymentId: String?) {
        try {
            showSuccessDialog("Payment Successful", "Payment ID: $paymentId")
            paymentId?.let { saveRechargeDetails(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPaymentError(code: Int, message: String?) {
        try {
            val errorMessage = when (code) {
                Checkout.PAYMENT_CANCELED -> "Payment cancelled by user"
                Checkout.NETWORK_ERROR -> "Network connection error"
                Checkout.INVALID_OPTIONS -> "Invalid payment options"
                else -> "Payment failed: $message"
            }
            showErrorDialog("Payment Failed", errorMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup Razorpay
        checkout.setKeyID(null)
    }
}