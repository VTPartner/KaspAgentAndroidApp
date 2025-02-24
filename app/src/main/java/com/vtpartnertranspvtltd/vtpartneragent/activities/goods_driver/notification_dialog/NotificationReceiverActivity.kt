package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.notification_dialog

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.services.AccessToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class NotificationReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show loading dialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Fetching booking details...")
            setCancelable(false)
            show()
        }

        // Get booking ID from intent
        val bookingId = intent.getStringExtra("booking_id")

        // Fetch booking details
        fetchBookingDetails(bookingId, progressDialog)
    }

    private fun fetchBookingDetails(bookingId: String?, progressDialog: ProgressDialog) {
        if (bookingId == null) {
            progressDialog.dismiss()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverToken = withContext(Dispatchers.IO) {
                    AccessToken.getAccessToken()
                }

                val url = "${Constants.BASE_URL}booking_details_for_ride_acceptance"
                val jsonBody = JSONObject().apply {
                    put("booking_id", bookingId)
                }

                withContext(Dispatchers.Main) {
                    val request = object : JsonObjectRequest(
                        Method.POST,
                        url,
                        jsonBody,
                        { response ->
                            progressDialog.dismiss()
                            try {
                                val results = response.optJSONArray("results")
                                if (results != null && results.length() > 0) {
                                    val bookingDetails = results.getJSONObject(0)
                                    // Start booking dialog activity
                                    startActivity(
                                        Intent(this@NotificationReceiverActivity,
                                        BookingDialogActivity::class.java).apply {
                                        putExtra("booking_details", bookingDetails.toString())
                                        putExtra("booking_id", bookingId)
                                    })
                                }
                                finish()
                            } catch (e: Exception) {
                                handleError(e)
                            }
                        },
                        { error ->
                            progressDialog.dismiss()
                            handleError(error)
                            finish()
                        }
                    ) {
                        override fun getHeaders(): MutableMap<String, String> {
                            return HashMap<String, String>().apply {
                                put("Content-Type", "application/json")
                                put("Authorization", "Bearer $serverToken")
                            }
                        }
                    }

                    request.retryPolicy = DefaultRetryPolicy(
                        30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )

                    VolleySingleton.getInstance(this@NotificationReceiverActivity)
                        .addToRequestQueue(request)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    handleError(e)
                    finish()
                }
            }
        }
    }

    private fun handleError(error: Exception) {
        Toast.makeText(
            this,
            "Error: ${error.message ?: "Unknown error occurred"}",
            Toast.LENGTH_SHORT
        ).show()
    }
}