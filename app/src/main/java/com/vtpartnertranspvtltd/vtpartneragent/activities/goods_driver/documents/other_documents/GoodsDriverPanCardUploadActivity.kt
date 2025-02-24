package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.other_documents


import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.VolleyError
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverPanCardUploadBinding
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

class GoodsDriverPanCardUploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverPanCardUploadBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var panFrontUri: Uri? = null
    private var panBackUri: Uri? = null
    private var previousPanFront: String? = null
    private var previousPanBack: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverPanCardUploadBinding.inflate(layoutInflater)
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
            panFrontContainer.setOnClickListener { showCameraPreview(true) }
            panBackContainer.setOnClickListener { showCameraPreview(false) }
            updateButton.setOnClickListener { savePanDetails() }
        }
    }

    private fun loadSavedData() {
        with(preferenceManager) {
            previousPanFront = getStringValue("pan_front_photo_url")
            previousPanBack = getStringValue("pan_back_photo_url")
            binding.panNumberInput.setText(getStringValue("pan_no"))

            loadImage(previousPanFront, binding.panFrontImage, binding.panFrontPlaceholder)
            loadImage(previousPanBack, binding.panBackImage, binding.panBackPlaceholder)
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
                Log.e(GoodsDriverPanCardUploadActivity.TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("AADHAR_${timeStamp}_", ".jpg", storageDir)
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
                    showToast("Failed to capture image")
                }
            }
        )
    }

    private fun handleCapturedImage(uri: Uri, isFront: Boolean) {
        if (isFront) {
            panFrontUri = uri
            loadImage(uri.toString(), binding.panFrontImage, binding.panFrontPlaceholder)
        } else {
            panBackUri = uri
            loadImage(uri.toString(), binding.panBackImage, binding.panBackPlaceholder)
        }
    }

    private fun savePanDetails() {
        val panNo = binding.panNumberInput.text.toString().trim()

        when {
            panNo.isEmpty() -> {
                showToast("Please provide your PAN number")
                return
            }
            isNewEntry() && panFrontUri == null -> {
                showToast("Please select and upload your PAN Card front image")
                return
            }
            isNewEntry() && panBackUri == null -> {
                showToast("Please select and upload your PAN Card back image")
                return
            }
        }

        if (needsFrontUpload()) {
            panFrontUri?.let { uploadImage(it, true) }
        }

        if (needsBackUpload()) {
            panBackUri?.let { uploadImage(it, false) }
        }

        preferenceManager.saveStringValue("pan_no", panNo)

        if (!needsFrontUpload() && !needsBackUpload()) {
            showToast("PAN Card details saved successfully")
            finish()
        }
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
            handleUploadError(e, loadingDialog)
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
        return "goods_driver_id_${driverId}_${driverName}_pan_${if(isFront) "front" else "back"}.jpg"
    }

    private fun handleUploadSuccess(response: NetworkResponse, isFront: Boolean, dialog: Dialog) {
        try {
            val imageUrl = JSONObject(String(response.data)).getString("image_url")
            updatePreferencesAndUI(imageUrl, isFront)
            dialog.dismiss()
            showToast("${if(isFront) "Front" else "Back"} image uploaded successfully")
        } catch (e: Exception) {
            handleUploadError(e, dialog)
        }
    }

    private fun handleUploadError(error: Exception, dialog: Dialog) {
        Log.e(TAG, "Upload failed", error)
        runOnUiThread {
            dialog.dismiss()
            showToast("Failed to upload image")
        }
    }

    private fun updatePreferencesAndUI(imageUrl: String, isFront: Boolean) {
        if (isFront) {
            preferenceManager.saveStringValue("pan_front_photo_url", imageUrl)
            previousPanFront = imageUrl
            loadImage(imageUrl, binding.panFrontImage, binding.panFrontPlaceholder)
        } else {
            preferenceManager.saveStringValue("pan_back_photo_url", imageUrl)
            previousPanBack = imageUrl
            loadImage(imageUrl, binding.panBackImage, binding.panBackPlaceholder)
        }
    }

    private fun isNewEntry() = preferenceManager.getStringValue("pan_no").isEmpty() &&
            (previousPanFront.isNullOrEmpty() || previousPanBack.isNullOrEmpty())

    private fun needsFrontUpload() = isNewEntry() ||
            (panFrontUri != null && previousPanFront != panFrontUri.toString())

    private fun needsBackUpload() = isNewEntry() ||
            (panBackUri != null && previousPanBack != panBackUri.toString())

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "GoodsDriverPanCardUploadActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    // Camera permission handling methods (same as AadharCard activity)
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
            GoodsDriverPanCardUploadActivity.CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GoodsDriverPanCardUploadActivity.CAMERA_PERMISSION_REQUEST -> {
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
}