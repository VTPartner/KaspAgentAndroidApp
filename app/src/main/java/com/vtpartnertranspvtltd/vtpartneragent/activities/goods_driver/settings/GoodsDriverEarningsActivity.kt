package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.AuthFailureError
import com.android.volley.NetworkError
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.snackbar.Snackbar
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.VTPartnerApp
import com.vtpartnertranspvtltd.vtpartneragent.activities.BaseActivity
import com.vtpartnertranspvtltd.vtpartneragent.adapters.OrdersAdapter
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverEarningsBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.OrderModel
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class GoodsDriverEarningsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverEarningsBinding
    private var monthlyEarnings = mutableListOf<Double>()
    private var monthlyEarningsTotal = 0.0
    private var allOrdersList = mutableListOf<OrderModel>()
    private var isLoading = true
    private var noOrdersFound = true
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverEarningsBinding.inflate(layoutInflater)
        setContentView(binding.root)
preferenceManager = PreferenceManager.getInstance(this)
        setupUI()
        fetchWholeYearsEarnings()
        fetchAllOrders()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.apply {
                title = "My Earnings"
                setSupportActionBar(this)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                // Handle navigation click directly on toolbar
                setNavigationOnClickListener {
                    finish()
                }
            }
        }
    }

    private fun fetchWholeYearsEarnings() {
        showLoading()

        val jsonObject = JSONObject().apply {
            var goodsDriverId = getDriverId()
            println("goodsDriverId::$goodsDriverId")
            put("driver_id", goodsDriverId)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}goods_driver_whole_year_earnings",
            jsonObject,
            { response ->
                try {
                    val results = response.getJSONArray("results")
                    monthlyEarningsTotal = calculateMonthlyEarnings(results)
                    monthlyEarnings.clear()

                    for (i in 0 until results.length()) {
                        val earning = results.getJSONObject(i).getDouble("total_earnings")
                        monthlyEarnings.add(earning)
                    }

                    updateEarningsUI()
                } catch (e: Exception) {
                    showError("Error parsing earnings data")
                }
                hideLoading()
            },
            { error ->
                handleError(error)
                hideLoading()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return HashMap<String, String>().apply {
                    put("Content-Type", "application/json")
                }
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun fetchAllOrders() {
        val jsonObject = JSONObject().apply {
            put("driver_id", getDriverId())
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}goods_driver_all_orders",
            jsonObject,
            { response ->
                try {
                    val results = response.getJSONArray("results")
                    allOrdersList.clear()

                    for (i in 0 until results.length()) {
                        val orderJson = results.getJSONObject(i)
                        allOrdersList.add(OrderModel(
                            customerName = orderJson.getString("customer_name"),
                            bookingDate = orderJson.getString("booking_date"),
                            totalPrice = orderJson.getString("total_price")
                        ))
                    }

                    noOrdersFound = allOrdersList.isEmpty()
                    updateOrdersUI()
                } catch (e: Exception) {
                    showError("Error parsing orders data")
                }
                isLoading = false
            },
            { error ->
                handleError(error)
                isLoading = false
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return HashMap<String, String>().apply {
                    put("Content-Type", "application/json")
                }
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun updateEarningsUI() {
        binding.apply {
            earningsChart.apply {
                data = generateBarChartData()
                invalidate()
            }
            totalEarningsText.text = "₹${monthlyEarningsTotal.roundToInt()} /-"
        }
    }

    private fun updateOrdersUI() {
        binding.apply {
            if (noOrdersFound) {
                ordersRecyclerView.visibility = View.GONE
                noOrdersText.visibility = View.VISIBLE
            } else {
                ordersRecyclerView.apply {
                    visibility = View.VISIBLE
                    adapter = OrdersAdapter(allOrdersList)
                    layoutManager = LinearLayoutManager(this@GoodsDriverEarningsActivity)
                }
                noOrdersText.visibility = View.GONE
            }
        }
    }

    private fun generateBarChartData(): BarData {
        val entries = monthlyEarnings.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Monthly Earnings").apply {
            color = ContextCompat.getColor(this@GoodsDriverEarningsActivity, R.color.primary_dark)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        return BarData(dataSet)
    }

    private fun calculateMonthlyEarnings(results: JSONArray): Double {
        var total = 0.0
        for (i in 0 until results.length()) {
            total += results.getJSONObject(i).getDouble("total_earnings")
        }
        return total
    }

    private fun getDriverId(): String {
        val goods_driver_id =  preferenceManager.getStringValue("goods_driver_id")
        return goods_driver_id

    }

    private fun handleError(error: VolleyError) {
        val message = when (error) {
            is NoConnectionError -> "No internet connection"
            is TimeoutError -> "Request timed out"
            is ServerError -> "Server error"
            else -> "An error occurred"
        }
        showError(message)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }
}