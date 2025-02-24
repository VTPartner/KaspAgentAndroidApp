package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.activities.BaseActivity
import com.vtpartnertranspvtltd.vtpartneragent.adapters.RatingsAdapter
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverRatingsBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.OrderRatingModel
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject

class GoodsDriverRatingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverRatingsBinding
    private var isLoading = true
    private var noOrdersFound = true
    private val ordersList = mutableListOf<OrderRatingModel>()
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverRatingsBinding.inflate(layoutInflater)
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
                title = "My Ratings"
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
            title = "My Ratings"
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GoodsDriverRatingsActivity)
            adapter = RatingsAdapter(ordersList)
        }
    }

    private fun fetchAllOrders() {
        isLoading = true
        ordersList.clear()
        binding.progressBar.visibility = View.VISIBLE

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
                        val orderJson = results.getJSONObject(i)
                        ordersList.add(OrderRatingModel.fromJson(orderJson))
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
            override fun getHeaders(): MutableMap<String, String> {
                return HashMap<String, String>().apply {
                    put("Content-Type", "application/json")
                }
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun updateUI() {
        binding.apply {
            if (noOrdersFound) {
                recyclerView.visibility = View.GONE
                noOrdersText.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                noOrdersText.visibility = View.GONE
                (recyclerView.adapter as RatingsAdapter).notifyDataSetChanged()
            }
        }
    }

    private fun getDriverId(): String {
        val goods_driver_id =  preferenceManager.getStringValue("goods_driver_id")
        return goods_driver_id

    }

    private fun handleError(error: Exception) {
        val message = when {
            error.message?.contains("No Data Found") == true -> {
                noOrdersFound = true
                "No Ratings Found"
            }
            else -> "An error occurred"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}