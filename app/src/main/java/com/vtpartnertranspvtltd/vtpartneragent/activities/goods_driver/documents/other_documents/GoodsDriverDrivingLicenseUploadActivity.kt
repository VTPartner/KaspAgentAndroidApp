package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.other_documents

import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverDrivingLicenseUploadBinding
import com.vtpartnertranspvtltd.vtpartneragent.databinding.DialogCameraPreviewBinding
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleyFileUploadRequest
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GoodsDriverDrivingLicenseUploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverDrivingLicenseUploadBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var licenseFrontUri: Uri? = null
    private var licenseBackUri: Uri? = null
    private var previousLicenseFront: String? = null
    private var previousLicenseBack: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverDrivingLicenseUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkCameraPermission()) {
            requestCameraPermission()
        } else {
            initializeApp()
        }
    }

    private fun initializeApp() {
        preferenceManager = PreferenceManager.getInstance(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupUI()
        loadSavedData()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            GoodsDriverDrivingLicenseUploadActivity.CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GoodsDriverDrivingLicenseUploadActivity.CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    initializeApp()
                } else {
                    // Permission denied
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app requires camera permission to capture documents. Please grant the permission to continue.")
            .setCancelable(false)
            .setPositiveButton("Grant Permission") { _, _ ->
                requestCameraPermission()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }
    private fun setupUI() {
        binding.apply {
            backButton.setOnClickListener { finish() }
            licenseFrontContainer.setOnClickListener { showCameraPreview(true) }
            licenseBackContainer.setOnClickListener { showCameraPreview(false) }
            updateButton.setOnClickListener { saveLicenseDetails() }
        }
    }

    private fun loadSavedData() {
        with(preferenceManager) {
            previousLicenseFront = getStringValue("license_front_photo_url")
            previousLicenseBack = getStringValue("license_back_photo_url")
            binding.licenseNumberInput.setText(getStringValue("license_no"))

            loadImage(previousLicenseFront, binding.licenseFrontImage, binding.licenseFrontPlaceholder)
            loadImage(previousLicenseBack, binding.licenseBackImage, binding.licenseBackPlaceholder)
        }
    }

    // ... (camera and image handling code same as AadharCard activity) ...
    private fun loadImage(url: String?, imageView: ImageView, placeholder: View) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this)
                .load(url)
                .into(imageView)
            placeholder.visibility = View.GONE
        }
    }

    private fun showCameraPreview(isFront: Boolean) {
        if (!checkCameraPermission()) {
            showPermissionDeniedDialog()
            return
        }
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogCameraPreviewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        startCamera(dialogBinding.previewView)

        dialogBinding.captureButton.setOnClickListener {
            takePhoto(isFront)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(GoodsDriverDrivingLicenseUploadActivity.TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(isFront: Boolean) {
        val imageCapture = imageCapture ?: return

        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    handleCapturedImage(uri, isFront)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(GoodsDriverDrivingLicenseUploadActivity.TAG, "Photo capture failed", exception)
                    showToast("Failed to capture image")
                }
            }
        )
    }

    private fun handleCapturedImage(uri: Uri, isFront: Boolean) {
        if (isFront) {
            licenseFrontUri = uri
            loadImage(uri.toString(), binding.licenseFrontImage, binding.licenseFrontPlaceholder)
        } else {
            licenseBackUri = uri
            loadImage(uri.toString(), binding.licenseBackImage, binding.licenseBackPlaceholder)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("AADHAR_${timeStamp}_", ".jpg", storageDir)
    }

    private fun uploadImage(uri: Uri, isFront: Boolean) {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Uploading ${if(isFront) "Front" else "Back"} Image")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            val request = createUploadRequest(uri, isFront, loadingDialog)
            VolleySingleton.getInstance(this).addToRequestQueue(request)
        } catch (e: Exception) {
            Log.e(GoodsDriverDrivingLicenseUploadActivity.TAG, "Upload preparation failed", e)
            loadingDialog.dismiss()
            showToast("Error preparing image")
        }
    }

    private fun createUploadRequest(uri: Uri, isFront: Boolean, loadingDialog: Dialog) =
        object : VolleyFileUploadRequest(
            Method.POST,
            "${Constants.IMAGE_SERVER_URL}/upload",
            { handleUploadSuccess(it, isFront, loadingDialog) },
            { handleUploadError(it, loadingDialog) }
        ) {
            override fun getByteData(): Map<String, DataPart> {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: ByteArray(0)
                inputStream?.close()

                val fileName = createFileName(isFront)
                return mapOf("image" to DataPart(fileName, bytes, "image/jpeg"))
            }
        }


    private fun createFileName(isFront: Boolean): String {
        val driverName = preferenceManager.getStringValue("goods_driver_name").ifEmpty { "NA" }
        val driverId = preferenceManager.getStringValue("goods_driver_id")
        return "goods_driver_id_${driverId}_${driverName}_license_${if(isFront) "front" else "back"}.jpg"
    }

    private fun handleUploadSuccess(response: NetworkResponse, isFront: Boolean, dialog: Dialog) {
        try {
            val imageUrl = JSONObject(String(response.data)).getString("image_url")
            updatePreferencesAndUI(imageUrl, isFront)
            dialog.dismiss()
            showToast("${if(isFront) "Front" else "Back"} image uploaded successfully")
        } catch (e: Exception) {
            Log.e(GoodsDriverDrivingLicenseUploadActivity.TAG, "Upload response processing failed", e)
            dialog.dismiss()
            showToast("Failed to process response")
        }
    }

    private fun handleUploadError(error: VolleyError, dialog: Dialog) {
        Log.e(GoodsDriverDrivingLicenseUploadActivity.TAG, "Upload failed", error)
        dialog.dismiss()
        showToast("Failed to upload image")
    }


    private fun updatePreferencesAndUI(imageUrl: String, isFront: Boolean) {
        if (isFront) {
            preferenceManager.saveStringValue("license_front_photo_url", imageUrl)
            previousLicenseFront = imageUrl
            loadImage(imageUrl, binding.licenseFrontImage, binding.licenseFrontPlaceholder)
        } else {
            preferenceManager.saveStringValue("license_back_photo_url", imageUrl)
            previousLicenseBack = imageUrl
            loadImage(imageUrl, binding.licenseBackImage, binding.licenseBackPlaceholder)
        }
    }

    private fun saveLicenseDetails() {
        val licenseNo = binding.licenseNumberInput.text.toString().trim()

        when {
            licenseNo.isEmpty() -> {
                showToast("Please provide your Driving License number")
                return
            }
            isNewEntry() && licenseFrontUri == null -> {
                showToast("Please select and upload your Driving License front image")
                return
            }
            isNewEntry() && licenseBackUri == null -> {
                showToast("Please select and upload your Driving License back image")
                return
            }
        }

        if (needsFrontUpload()) {
            licenseFrontUri?.let { uploadImage(it, true) }
        }

        if (needsBackUpload()) {
            licenseBackUri?.let { uploadImage(it, false) }
        }

        preferenceManager.saveStringValue("license_no", licenseNo)

        if (!needsFrontUpload() && !needsBackUpload()) {
            showToast("License details saved successfully")
            finish()
        }
    }

    private fun isNewEntry() = preferenceManager.getStringValue("license_no").isEmpty() &&
            (previousLicenseFront.isNullOrEmpty() || previousLicenseBack.isNullOrEmpty())

    private fun needsFrontUpload() = isNewEntry() ||
            (licenseFrontUri != null && previousLicenseFront != licenseFrontUri.toString())

    private fun needsBackUpload() = isNewEntry() ||
            (licenseBackUri != null && previousLicenseBack != licenseBackUri.toString())

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    companion object {
        private const val TAG = "GoodsDriverLicenseUploadActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}