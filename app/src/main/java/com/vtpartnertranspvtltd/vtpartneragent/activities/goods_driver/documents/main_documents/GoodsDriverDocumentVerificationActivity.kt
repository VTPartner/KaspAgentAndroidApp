package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.main_documents

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.android.volley.toolbox.JsonObjectRequest
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R

import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.other_documents.GoodsDriverAadharCardUploadActivity

import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.other_documents.GoodsDriverDrivingLicenseUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.other_documents.GoodsDriverOwnerSelfieUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.other_documents.GoodsDriverPanCardUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverDocumentVerificationBinding
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject

class GoodsDriverDocumentVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverDocumentVerificationBinding
    private lateinit var preferenceManager: PreferenceManager
    private var selectedGender: String? = null
    private var selectedCityId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverDocumentVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)
        setupUI()
        fetchCities()
    }

    private fun setupUI() {
        // Setup Gender Dropdown
        val genders = listOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(this, R.layout.item_dropdown, genders)
        binding.genderDropdown.setAdapter(genderAdapter)
        binding.genderDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedGender = genders[position]
        }

        // Setup Click Listeners
        binding.apply {
            backButton.setOnClickListener { finish() }

            // Document Upload Clicks
            drivingLicenseItem.setOnClickListener {
                startActivity(
                    Intent(this@GoodsDriverDocumentVerificationActivity,
                    GoodsDriverDrivingLicenseUploadActivity::class.java)
                )
            }

            aadharCardItem.setOnClickListener {
                startActivity(Intent(this@GoodsDriverDocumentVerificationActivity,
                    GoodsDriverAadharCardUploadActivity::class.java))
            }

            panCardItem.setOnClickListener {
                startActivity(Intent(this@GoodsDriverDocumentVerificationActivity,
                    GoodsDriverPanCardUploadActivity::class.java))
            }

            selfieItem.setOnClickListener {
                startActivity(Intent(this@GoodsDriverDocumentVerificationActivity,
                    GoodsDriverOwnerSelfieUploadActivity::class.java))
            }

            continueButton.setOnClickListener {
                saveDriverDetails()
            }
        }

        // Restore saved values if any
        binding.apply {
            nameInput.setText(preferenceManager.getStringValue("driver_name"))
            addressInput.setText(preferenceManager.getStringValue("full_address"))
            selectedGender = preferenceManager.getStringValue("driver_gender")
            selectedGender?.let { genderDropdown.setText(it, false) }
        }
    }

    private fun fetchCities() {
        val jsonObject = JSONObject()

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}all_cities",
            jsonObject,
            { response ->
                try {
                    val cities = response.getJSONArray("results")
                    val cityNames = mutableListOf<String>()
                    val cityIds = mutableListOf<String>()

                    for (i in 0 until cities.length()) {
                        val city = cities.getJSONObject(i)
                        cityNames.add(city.getString("city_name"))
                        cityIds.add(city.getString("city_id"))
                    }

                    val cityAdapter = ArrayAdapter(this, R.layout.item_dropdown, cityNames)
                    binding.cityDropdown.setAdapter(cityAdapter)
                    binding.cityDropdown.setOnItemClickListener { _, _, position, _ ->
                        selectedCityId = cityIds[position]
                    }

                    // Restore selected city if any
                    val savedCityId = preferenceManager.getStringValue("driver_city_id")
                    if (savedCityId.isNotEmpty()) {
                        val index = cityIds.indexOf(savedCityId)
                        if (index != -1) {
                            binding.cityDropdown.setText(cityNames[index], false)
                            selectedCityId = savedCityId
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Failed to load cities")
                }
            },
            { error ->
                error.printStackTrace()
                showError("Failed to fetch cities")
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

    private fun saveDriverDetails() {
        val name = binding.nameInput.text.toString().trim()
        val address = binding.addressInput.text.toString().trim()

        when {
            name.isEmpty() -> {
                showError("Please provide your full name")
                return
            }
            selectedGender == null -> {
                showError("Please select your gender")
                return
            }
            address.isEmpty() -> {
                showError("Please provide your full address")
                return
            }
            selectedCityId == null -> {
                showError("Please select your registration city")
                return
            }
            !checkDocuments() -> {
                return
            }
        }

        // Save to SharedPreferences
        preferenceManager.apply {
            saveStringValue("driver_name", name.trim())
            saveStringValue("full_address", address.trim())
            saveStringValue("driver_gender", selectedGender!!)
            saveStringValue("driver_city_id", selectedCityId!!)
        }

        // Navigate to next screen
         startActivity(Intent(this, GoodsDriverVehicleDocumentsVerificationActivity::class.java))
    }

    private fun checkDocuments(): Boolean {
        with(preferenceManager) {
            when {
                getStringValue("license_front_photo_url").isEmpty() -> {
                    showError("Please upload Driving License Front Picture")
                    return false
                }
                getStringValue("license_back_photo_url").isEmpty() -> {
                    showError("Please upload Driving License Back Picture")
                    return false
                }
                getStringValue("license_no").isEmpty() -> {
                    showError("Please upload Driving License Number")
                    return false
                }
                getStringValue("aadhar_front_photo_url").isEmpty() -> {
                    showError("Please upload Aadhar Front Picture")
                    return false
                }
                getStringValue("aadhar_back_photo_url").isEmpty() -> {
                    showError("Please upload Aadhar Back Picture")
                    return false
                }
                getStringValue("aadhar_no").isEmpty() -> {
                    showError("Please provide your Aadhar Number")
                    return false
                }
                getStringValue("pan_no").isEmpty() -> {
                    showError("Please upload PAN Number")
                    return false
                }
                getStringValue("pan_front_photo_url").isEmpty() -> {
                    showError("Please upload PAN Front Picture")
                    return false
                }
                getStringValue("pan_back_photo_url").isEmpty() -> {
                    showError("Please upload PAN Back Picture")
                    return false
                }
                getStringValue("selfie_photo_url").isEmpty() -> {
                    showError("Please upload your selfie")
                    return false
                }

                else -> {}
            }
        }
        return true
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}