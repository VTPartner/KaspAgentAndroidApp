package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings

import OrderRidesModel
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.activities.BaseActivity
import com.vtpartnertranspvtltd.vtpartneragent.adapters.RidesAdapter
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverRidesBinding
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject

class GoodsDriverRidesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverRidesBinding
    private var isLoading = true
    private var noOrdersFound = true
    private val ordersList = mutableListOf<OrderRidesModel>()
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverRidesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager.getInstance(this)
//        setupToolbar()
        setupUI()
        setupRecyclerView()
        fetchAllOrders()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.apply {
                title = "My Rides"
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
            title = "My Rides"
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupRecyclerView() {
        binding.ridesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GoodsDriverRidesActivity)
            adapter = RidesAdapter(ordersList) { order ->
                // Handle ride item click
//                startActivity(
//                    Intent(this@GoodsDriverRidesActivity,
//                    GoodsDriverRideDetailsActivity::class.java).apply {
//                    putExtra("order", order)
//                })
            }
        }
    }

    private fun fetchAllOrders() {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        ordersList.clear()

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
                    ordersList.clear()

                    for (i in 0 until results.length()) {
                        ordersList.add(OrderRidesModel.fromJson(results.getJSONObject(i)))
                    }

                    noOrdersFound = ordersList.isEmpty()
                    updateUI()
                } catch (e: Exception) {
                    handleError(e)
                } finally {
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                }
            },
            { error ->
                handleError(error)
                isLoading = false
                binding.progressBar.visibility = View.GONE
            }
        ) {
            override fun getHeaders() = HashMap<String, String>().apply {
                put("Content-Type", "application/json")
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun updateUI() {
        binding.apply {
            if (noOrdersFound) {
                ridesRecyclerView.visibility = View.GONE
//                emptyView.visibility = View.VISIBLE
            } else {
                ridesRecyclerView.visibility = View.VISIBLE
//                emptyView.visibility = View.GONE
                (ridesRecyclerView.adapter as RidesAdapter).notifyDataSetChanged()
            }
        }
    }

    private fun handleError(error: Exception) {
        val message = when {
            error.message?.contains("No Data Found") == true -> {
                noOrdersFound = true
                "No Rides Found"
            }
            else -> "An error occurred"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun getDriverId(): String {
        val goods_driver_id =  preferenceManager.getStringValue("goods_driver_id")
        return goods_driver_id

    }
}