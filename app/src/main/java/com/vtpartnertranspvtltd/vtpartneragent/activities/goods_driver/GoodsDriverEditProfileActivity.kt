package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.adapters.DocumentsAdapter
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverEditProfileBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.Document
import com.vtpartnertranspvtltd.vtpartneragent.models.DocumentType
import com.vtpartnertranspvtltd.vtpartneragent.models.DriverDetails
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.ImagePreviewDialog
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class GoodsDriverEditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverEditProfileBinding
    private lateinit var documentsAdapter: DocumentsAdapter
    private lateinit var preferenceManager: PreferenceManager
    private val documents = mutableListOf<Document>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)
        setupUI()
        fetchDriverDetails()
    }

    private fun fetchDriverDetails() {
        showLoading()

        val jsonObject = JSONObject().apply {
            put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"))
        }

        val request = object : JsonObjectRequest(
            Method.POST,
            "${Constants.BASE_URL}get_goods_driver_details",
            jsonObject,
            { response ->
                try {
                    hideLoading()
                    val driverDetails = DriverDetails.fromJson(response.getJSONObject("result"))
                    updateUI(driverDetails)
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError("Failed to parse data")
                }
            },
            { error ->
                hideLoading()
                error.printStackTrace()
                when {
                    error.toString().contains("No Data Found") -> {
                        showError("Driver details not found")
                    }
                    else -> {
                        showError("Failed to load profile")
                    }
                }
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

    private fun updateUI(driverDetails: DriverDetails) {
        binding.apply {
            // Profile Image
            Glide.with(this@GoodsDriverEditProfileActivity)
                .load(driverDetails.profilePic)
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(profileImage)

            // Personal Information
            // Personal Information
            nameText.text = "Name: ${driverDetails.driverFirstName}"
            mobileText.text = "Mobile No: ${driverDetails.mobileNo}"
            addressText.text = "Address: ${driverDetails.fullAddress}"
            cityText.text = "City: ${driverDetails.cityName}"

            // Vehicle Information
            vehicleNameText.text = "Vehicle: ${driverDetails.vehicleName}"
            vehicleTypeText.text = "Type: ${driverDetails.vehicleTypeName}"
            vehiclePlateText.text = "Plate No: ${driverDetails.vehiclePlateNo}"
            fuelTypeText.text = "Fuel Type: ${driverDetails.vehicleFuelType}"
            rcNumberText.text = "RC Number: ${driverDetails.rcNo}"
            insuranceNumberText.text = "Insurance No: ${driverDetails.insuranceNo}"
//            lastNameInput.setText(driverDetails.driverLastName)
//            mobileInput.setText(driverDetails.mobileNo)
//            addressInput.setText(driverDetails.fullAddress)
//            genderInput.setText(driverDetails.gender)
//            cityInput.setText(driverDetails.cityName)
//
//            // Vehicle Information
//            vehicleNameInput.setText(driverDetails.vehicleName)
//            vehicleTypeInput.setText(driverDetails.vehicleTypeName)
//            vehiclePlateInput.setText(driverDetails.vehiclePlateNo)
//            fuelTypeInput.setText(driverDetails.vehicleFuelType)
//            rcNumberInput.setText(driverDetails.rcNo)
//            insuranceNumberInput.setText(driverDetails.insuranceNo)

            // Documents
            documents.clear()
            documents.addAll(driverDetails.toDocumentsList())
            documentsAdapter.notifyDataSetChanged()

            // Status Chip
//            statusChip.apply {
//                text = driverDetails.getVerificationStatus()
//                setChipBackgroundColorResource(
//                    when {
//                        driverDetails.isVerified -> R.color.success
//                        !driverDetails.hasAllDocuments() -> R.color.warning
//                        else -> R.color.info
//                    }
//                )
//            }
        }
    }

    private fun showLoading() {
        binding.scrollView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun setupUI() {
        binding.apply {
            // Toolbar
            toolbar.apply {
                title = "Edit Profile"
                setSupportActionBar(this)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }

            // Documents RecyclerView
            documentsRecyclerView.apply {
                layoutManager = GridLayoutManager(this@GoodsDriverEditProfileActivity, 3)
                documentsAdapter = DocumentsAdapter(documents) { document ->
                    showImagePreviewDialog(document)
                }
                adapter = documentsAdapter
//                addItemDecoration(GridSpacingItemDecoration(3, 16, true))
            }

            // Profile Image Click
            profileImage.setOnClickListener {
                showImagePreviewDialog(Document(
                    "profile",
                    "Profile Picture",
                    preferenceManager.getStringValue("profile_pic"),
                    DocumentType.PROFILE
                ))
            }

            // Edit Profile Button
            editProfileButton.setOnClickListener {
                // Handle profile image update
                showImagePickerDialog()
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Choose Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
//                    0 -> openCamera()
//                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun showImagePreviewDialog(document: Document) {


        ImagePreviewDialog.newInstance(
            name = document.name.toString(),
            imageUrl = document.imageUrl
        ).show(supportFragmentManager, "image_preview")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}