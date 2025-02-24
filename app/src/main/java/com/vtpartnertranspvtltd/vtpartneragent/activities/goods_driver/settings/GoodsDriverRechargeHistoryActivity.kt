package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.adapters.RechargeHistoryAdapter
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverRechargeHistoryBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.RechargeHistoryModel
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class GoodsDriverRechargeHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverRechargeHistoryBinding
    private var currentBalance = "0"
    private val rechargeHistory = mutableListOf<RechargeHistoryModel>()
    private var limitExceededBalance = 0.0
    private var isExpired = false
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverRechargeHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager.getInstance(this)
//        setupToolbar()
        setupUI()
        setupRecyclerView()
        fetchCurrentBalance()
        fetchRechargeHistory()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.apply {
                title = "Recharge History"
                setSupportActionBar(this)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                // Handle navigation click directly on toolbar
                setNavigationOnClickListener {
                    finish()
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Recharge History"
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GoodsDriverRechargeHistoryActivity)
            adapter = RechargeHistoryAdapter(rechargeHistory)
        }
    }

    private fun fetchCurrentBalance() {
        val jsonObject = JSONObject().apply {
            put("driver_id", getDriverId())
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}get_goods_driver_current_recharge_details",
            jsonObject,
            { response ->
                try {
                    val results = response.getJSONArray("results")
                    if (results.length() > 0) {
                        val data = results.getJSONObject(0)
                        var hasNegativePoints = false

                        currentBalance = data.getString("remaining_points")
                        val negativePoints = data.getDouble("negative_points")

                        if (negativePoints > 0) {
                            currentBalance = negativePoints.toString()
                            limitExceededBalance = currentBalance.toDouble()
                            hasNegativePoints = true
                            currentBalance = "-$currentBalance"

                            showSnackBar("To continue receiving ride requests, please ensure your account is recharged.")
                        }

                        val validTillDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(data.getString("valid_till_date"))
                        val currentDate = Calendar.getInstance().time

                        if (validTillDate?.before(currentDate) == true ||
                            validTillDate?.equals(currentDate) == true) {
                            if (hasNegativePoints) {
                                currentBalance = "-$limitExceededBalance"
                                isExpired = true
                                showSnackBar("Your previous plan has expired. Please recharge promptly.")
                            }
                        }

                        updateBalanceUI()
                    }
                } catch (e: Exception) {
                    handleError(e)
                }
            },
            { error -> handleError(error) }
        ) {
            override fun getHeaders() = HashMap<String, String>().apply {
                put("Content-Type", "application/json")
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun fetchRechargeHistory() {
        val jsonObject = JSONObject().apply {
            put("driver_id", getDriverId())
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}get_goods_driver_recharge_history_details",
            jsonObject,
            { response ->
                try {
                    val results = response.getJSONArray("results")
//                    println("results::$results")
                    rechargeHistory.clear()

                    for (i in 0 until results.length()) {
                        rechargeHistory.add(
                            RechargeHistoryModel.fromJson(results.getJSONObject(i))
                        )
                    }

                    updateUI()
                } catch (e: Exception) {
                    handleError(e)
                }
            },
            { error -> handleError(error) }
        ) {
            override fun getHeaders() = HashMap<String, String>().apply {
                put("Content-Type", "application/json")
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun updateUI() {
        binding.apply {
            if (rechargeHistory.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                (recyclerView.adapter as RechargeHistoryAdapter).notifyDataSetChanged()
            }
        }
    }

    private fun updateBalanceUI() {
        binding.apply {
            balanceAmount.text = "₹${currentBalance.toDouble().roundToInt()}"
        }
    }

    private fun handleError(error: Exception) {
        println("recharge history error:${error.message}")
        val message = when {
            error.message?.contains("No Data Found") == true ->
                "No Recharge History Found"
            else -> "An error occurred"
        }

        showSnackBar(message)
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun getDriverId(): String {
        val goods_driver_id =  preferenceManager.getStringValue("goods_driver_id")
        return goods_driver_id

    }
}