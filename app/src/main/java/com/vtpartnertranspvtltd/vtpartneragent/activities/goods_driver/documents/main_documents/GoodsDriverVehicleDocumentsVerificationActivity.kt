package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.main_documents



import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.JsonObjectRequest
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents.GoodsDriverPUCUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents.GoodsDriverVehicleImageUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents.GoodsDriverVehicleInsuranceUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents.GoodsDriverVehicleNOCUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents.GoodsDriverVehiclePlateNoUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents.GoodsDriverVehicleRCUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverVehicleDocumentsVerificationBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.VehiclesModel
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton

import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject

class GoodsDriverVehicleDocumentsVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverVehicleDocumentsVerificationBinding
    private lateinit var preferenceManager: PreferenceManager
    private var vehicles = mutableListOf<VehiclesModel>()
    private val fuelTypes = listOf("Diesel", "Petrol", "CNG", "Electrical")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverVehicleDocumentsVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)
        setupUI()
        fetchVehicles()
        setupFuelTypeSpinner()
        loadSavedData()
    }

    private fun setupUI() {
        binding.apply {
            backButton.setOnClickListener { finish() }
            continueButton.setOnClickListener { saveVehicleDetails() }

            // Setup document click listeners
            vehicleImagesCard.setOnClickListener {
                startActivity(
                    Intent(this@GoodsDriverVehicleDocumentsVerificationActivity,
                    GoodsDriverVehicleImageUploadActivity::class.java)
                )
            }

            vehiclePlateCard.setOnClickListener {
                startActivity(Intent(this@GoodsDriverVehicleDocumentsVerificationActivity,
                    GoodsDriverVehiclePlateNoUploadActivity::class.java))
            }

            rcCard.setOnClickListener {
                startActivity(Intent(this@GoodsDriverVehicleDocumentsVerificationActivity,
                    GoodsDriverVehicleRCUploadActivity::class.java))
            }

            insuranceCard.setOnClickListener {
                startActivity(Intent(this@GoodsDriverVehicleDocumentsVerificationActivity,GoodsDriverVehicleInsuranceUploadActivity::class.java))
            }

            nocCard.setOnClickListener {
                startActivity(Intent(this@GoodsDriverVehicleDocumentsVerificationActivity,GoodsDriverVehicleNOCUploadActivity::class.java))
            }

            pucCard.setOnClickListener {
                startActivity(Intent(this@GoodsDriverVehicleDocumentsVerificationActivity,GoodsDriverPUCUploadActivity::class.java))
            }
        }
    }

    private fun fetchVehicles() {
        val requestData = JSONObject().apply {
            put("category_id", 1)
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}all_vehicles",
            requestData,
            { response ->
                try {
                    if (response.has("results")) {
                        val results = response.getJSONArray("results")
                        vehicles.clear()

                        for (i in 0 until results.length()) {
                            val vehicle = results.getJSONObject(i)
                            vehicles.add(VehiclesModel(
                                vehicleId = vehicle.getString("vehicle_id"),
                                vehicleName = vehicle.getString("vehicle_name")
                            ))
                        }

                        setupVehicleSpinner()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing vehicles", e)
                    showToast("Error loading vehicles")
                }
            },
            { error ->
                Log.e(TAG, "Error fetching vehicles", error)
                if (error.message?.contains("No Data Found") == true) {
                    showToast("No Vehicles Found")
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                // Add any other required headers
                return headers
            }
        }

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun setupVehicleSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            vehicles.map { it.vehicleName }
        )

        binding.vehicleSpinner.apply {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                val selectedVehicle = vehicles[position]
                preferenceManager.saveStringValue("driver_vehicle_id", selectedVehicle.vehicleId)
            }
        }
    }

    private fun setupFuelTypeSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            fuelTypes
        )

        binding.fuelTypeSpinner.apply {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                val selectedFuel = fuelTypes[position]
                preferenceManager.saveStringValue("driver_vehicle_fuel_type", selectedFuel)
            }
        }
    }



    private fun loadSavedData() {
        with(preferenceManager) {
            binding.vehicleNumberInput.setText(getStringValue("driver_vehicle_no"))

            // Load saved fuel type
            val savedFuelType = getStringValue("driver_vehicle_fuel_type")
            if (savedFuelType.isNotEmpty()) {
                binding.fuelTypeSpinner.setText(savedFuelType, false)
            }

            // Load saved vehicle
            val savedVehicleId = getStringValue("driver_vehicle_id")
            if (savedVehicleId.isNotEmpty()) {
                val savedVehicle = vehicles.find { it.vehicleId == savedVehicleId }
                savedVehicle?.let {
                    binding.vehicleSpinner.setText(it.vehicleName, false)
                }
            }
        }
    }

    private fun saveVehicleDetails() {
        val vehicleNo = binding.vehicleNumberInput.text.toString().trim()
        val selectedFuelType = binding.fuelTypeSpinner.text.toString()
        val selectedVehicleName = binding.vehicleSpinner.text.toString()
        val selectedVehicle = vehicles.find { it.vehicleName == selectedVehicleName }

        when {
            vehicleNo.isEmpty() -> {
                showToast("Please provide your valid vehicle number")
                return
            }
            selectedFuelType.isEmpty() || !fuelTypes.contains(selectedFuelType) -> {
                showToast("Please select vehicle fuel type")
                return
            }
            selectedVehicle == null -> {
                showToast("Please select your registration vehicle")
                return
            }
        }

        // Verify all required documents are uploaded
        if (!verifyDocuments()) {
            return
        }

        // Save vehicle details
        with(preferenceManager) {
            saveStringValue("driver_vehicle_no", vehicleNo)
            saveStringValue("driver_vehicle_fuel_type", selectedFuelType)
            if (selectedVehicle != null) {
                saveStringValue("driver_vehicle_id", selectedVehicle.vehicleId)
            }
        }

        // Navigate to next screen
        startActivity(Intent(this, GoodsDriverVehicleOwnerDetailsActivity::class.java))
        finish()
    }

    private fun verifyDocuments(): Boolean {
        with(preferenceManager) {
            when {
                getStringValue("vehicle_front_photo_url").isEmpty() ||
                        getStringValue("vehicle_back_photo_url").isEmpty() -> {
                    showToast("Please provide Vehicle Images")
                    return false
                }
                getStringValue("vehicle_plate_front_photo_url").isEmpty() ||
                        getStringValue("vehicle_plate_back_photo_url").isEmpty() -> {
                    showToast("Please upload Vehicle Plate Images")
                    return false
                }
                getStringValue("rc_photo_url").isEmpty() ||
                        getStringValue("rc_no").isEmpty() -> {
                    showToast("Please upload RC details")
                    return false
                }
                getStringValue("insurance_photo_url").isEmpty() ||
                        getStringValue("insurance_no").isEmpty() -> {
                    showToast("Please upload Insurance details")
                    return false
                }
                getStringValue("noc_photo_url").isEmpty() ||
                        getStringValue("noc_no").isEmpty() -> {
                    showToast("Please upload NOC details")
                    return false
                }
                getStringValue("puc_photo_url").isEmpty() ||
                        getStringValue("puc_no").isEmpty() -> {
                    showToast("Please upload PUC details")
                    return false
                }
                else -> return true
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "GoodsDriverVehicleDocsVerification"
    }
}