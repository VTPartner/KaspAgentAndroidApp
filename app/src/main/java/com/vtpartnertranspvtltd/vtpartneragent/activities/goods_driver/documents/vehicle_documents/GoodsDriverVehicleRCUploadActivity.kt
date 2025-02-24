package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents

import androidx.camera.view.PreviewView
import com.vtpartnertranspvtltd.vtpartneragent.databinding.DialogCameraPreviewBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale



import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.NetworkResponse
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverVehicleRcuploadBinding

import com.vtpartnertranspvtltd.vtpartneragent.network.VolleyFileUploadRequest
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton

import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GoodsDriverVehicleRCUploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverVehicleRcuploadBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewDialog: Dialog? = null

    private var imageCapture: ImageCapture? = null
    private var rcImageUri: Uri? = null
    private var previousRC: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverVehicleRcuploadBinding.inflate(layoutInflater)
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

    private fun setupUI() {
        binding.apply {
            backButton.setOnClickListener { finish() }
            rcContainer.setOnClickListener { showCameraPreview(true) }
            updateButton.setOnClickListener { saveRCDetails() }
        }
    }

    private fun loadSavedData() {
        with(preferenceManager) {
            previousRC = getStringValue("rc_photo_url")
            binding.rcNumberInput.setText(getStringValue("rc_no"))
            loadImage(previousRC, binding.rcImage, binding.rcPlaceholder)
        }
    }

    private fun loadImage(url: String?, imageView: ImageView, placeholder: View) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this)
                .load(url)
                .into(imageView)
            placeholder.visibility = View.GONE
        }
    }

    private fun saveRCDetails() {
        val rcNo = binding.rcNumberInput.text.toString().trim()

        when {
            rcNo.isEmpty() -> {
                showToast("Please provide your RC number")
                return
            }
            isNewEntry() && rcImageUri == null -> {
                showToast("Please select and upload your RC book image")
                return
            }
        }

        if (needsImageUpload()) {
            rcImageUri?.let { uploadImage(it) }
        }

        preferenceManager.saveStringValue("rc_no", rcNo)

        if (!needsImageUpload() && rcNo == preferenceManager.getStringValue("rc_no")) {
            showToast("No changes made")
            return
        }

        if (!needsImageUpload()) {
            showToast("Aadhar details saved successfully")
            finish()
        }
    }

    private fun uploadImage(uri: Uri) {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Uploading RC Book")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            val request = createUploadRequest(uri, loadingDialog)
            VolleySingleton.getInstance(this).addToRequestQueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "Upload preparation failed", e)
            loadingDialog.dismiss()
            showToast("Error preparing image")
        }
    }

    private fun createUploadRequest(uri: Uri, loadingDialog: Dialog) =
        object : VolleyFileUploadRequest(
            Method.POST,
            "${Constants.IMAGE_SERVER_URL}/upload",
            { handleUploadSuccess(it, loadingDialog) },
            { handleUploadError(it, loadingDialog) }
        ) {
            override fun getByteData(): Map<String, DataPart> {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: ByteArray(0)
                inputStream?.close()

                val fileName = createFileName()
                return mapOf("image" to DataPart(fileName, bytes, "image/jpeg"))
            }
        }

    private fun createFileName(): String {
        val driverName = preferenceManager.getStringValue("goods_driver_name").ifEmpty { "NA" }
        val driverId = preferenceManager.getStringValue("goods_driver_id")
        return "goods_driver_id_${driverId}_${driverName}_rc.jpg"
    }

    private fun handleUploadSuccess(response: NetworkResponse, dialog: Dialog) {
        try {
            val imageUrl = JSONObject(String(response.data)).getString("image_url")
            updatePreferencesAndUI(imageUrl)
            dialog.dismiss()
            showToast("RC book uploaded successfully")
        } catch (e: Exception) {
            handleUploadError(e, dialog)
        }
    }

    private fun handleUploadError(error: Exception, dialog: Dialog) {
        Log.e(TAG, "Upload failed", error)

            dialog.dismiss()
            showToast("Failed to upload image")

    }

    private fun updatePreferencesAndUI(imageUrl: String) {
        preferenceManager.saveStringValue("rc_photo_url", imageUrl)
        previousRC = imageUrl
        loadImage(imageUrl, binding.rcImage, binding.rcPlaceholder)
    }

    private fun isNewEntry() = preferenceManager.getStringValue("rc_no").isEmpty() &&
            previousRC.isNullOrEmpty()

    private fun needsImageUpload() = isNewEntry() ||
            (rcImageUri != null && previousRC != rcImageUri.toString())

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
            CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    initializeApp()
                } else {
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

    private fun showCameraPreview(isFront: Boolean) {
        if (!checkCameraPermission()) {
            showPermissionDeniedDialog()
            return
        }
        previewDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogCameraPreviewBinding.inflate(layoutInflater)
        previewDialog?.setContentView(dialogBinding.root)

        startCamera(dialogBinding.previewView)

        dialogBinding.captureButton.setOnClickListener {
            takePhoto(isFront)
            previewDialog?.dismiss()
        }

        previewDialog?.show()
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
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
                    Log.e(TAG, "Photo capture failed", exception)
                    showToast("Failed to capture image")
                }
            }
        )
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(System.currentTimeMillis())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("VEHICLE_RC_${timeStamp}_", ".jpg", storageDir)
    }

    private fun handleCapturedImage(uri: Uri, isFront: Boolean) {
        rcImageUri = uri
        loadImage(uri.toString(), binding.rcImage, binding.rcPlaceholder)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        previewDialog?.dismiss()
    }

    companion object {
        private const val TAG = "GoodsDriverRCUploadActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}