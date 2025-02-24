package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.owner_documents



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
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.NetworkResponse
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.documents.vehicle_documents.GoodsDriverVehicleRCUploadActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverVehicleOwnerSelfieUploadBinding
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

class GoodsDriverVehicleOwnerSelfieUploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverVehicleOwnerSelfieUploadBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var selfieUri: Uri? = null
    private var previousSelfie: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverVehicleOwnerSelfieUploadBinding.inflate(layoutInflater)
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
        setupCamera()
    }

    private fun setupUI() {
        binding.apply {
            backButton.setOnClickListener { finish() }
            selfieContainer.setOnClickListener { showCameraPreview(false) }
            updateButton.setOnClickListener { saveSelfie() }
        }
    }

    private fun loadSavedData() {
        previousSelfie = preferenceManager.getStringValue("owner_selfie_photo_url")
        loadImage(previousSelfie, binding.selfieImage, binding.selfiePlaceholder)
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadImage(url: String?, imageView: ImageView, placeholder: View) {
        if (!url.isNullOrEmpty()) {
            Glide.with(this)
                .load(url)
                .into(imageView)
            placeholder.visibility = View.GONE
        }
    }

    private fun saveSelfie() {
        when {
            selfieUri == null && previousSelfie.isNullOrEmpty() -> {
                showToast("Please take a selfie")
                return
            }
            selfieUri == null && !previousSelfie.isNullOrEmpty() -> {
                showToast("This selfie already exists")
                return
            }
        }

        selfieUri?.let { uploadImage(it) }
    }

    private fun uploadImage(uri: Uri) {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Uploading Selfie")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            val request = createUploadRequest(uri, loadingDialog)
            VolleySingleton.getInstance(this).addToRequestQueue(request)
        } catch (e: Exception) {
            handleUploadError(e, loadingDialog)
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
        return "goods_driver_id_${driverId}_${driverName}_owner_selfie.jpg"
    }

    private fun handleUploadSuccess(response: NetworkResponse, dialog: Dialog) {
        try {
            val imageUrl = JSONObject(String(response.data)).getString("image_url")
            updatePreferencesAndUI(imageUrl)
            dialog.dismiss()
            showToast("Selfie uploaded successfully")
            finish()
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

    private fun updatePreferencesAndUI(imageUrl: String) {
        preferenceManager.saveStringValue("owner_selfie_photo_url", imageUrl)
        previousSelfie = imageUrl
        loadImage(imageUrl, binding.selfieImage, binding.selfiePlaceholder)
    }

    // ... Camera handling methods (same as other activities) ...

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
            GoodsDriverVehicleOwnerSelfieUploadActivity.CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GoodsDriverVehicleOwnerSelfieUploadActivity.CAMERA_PERMISSION_REQUEST -> {
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
                Log.e(GoodsDriverVehicleOwnerSelfieUploadActivity.TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("VEHICLE_REGISTER_OWNER_SELFIE_${timeStamp}_", ".jpg", storageDir)
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
                    Log.e(GoodsDriverVehicleOwnerSelfieUploadActivity.TAG, "Photo capture failed", exception)
                    showToast("Failed to capture image")
                }
            }
        )
    }

    private fun handleCapturedImage(uri: Uri, isFront: Boolean) {
        if (isFront) {
            selfieUri = uri
            loadImage(uri.toString(), binding.selfieImage, binding.selfiePlaceholder)
        }
    }

    companion object {
        private const val TAG = "GoodsDriverOwnerSelfieUploadActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}