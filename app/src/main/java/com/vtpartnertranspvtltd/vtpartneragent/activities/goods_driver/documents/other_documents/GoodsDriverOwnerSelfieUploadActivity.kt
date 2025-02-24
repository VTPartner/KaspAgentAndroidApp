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
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverOwnerSelfieUploadBinding
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

class GoodsDriverOwnerSelfieUploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverOwnerSelfieUploadBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var cameraExecutor: ExecutorService
    private var isFrontCamera = true
    private var imageCapture: ImageCapture? = null
    private var selfieUri: Uri? = null
    private var previousSelfie: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverOwnerSelfieUploadBinding.inflate(layoutInflater)
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
            GoodsDriverOwnerSelfieUploadActivity.CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            GoodsDriverOwnerSelfieUploadActivity.CAMERA_PERMISSION_REQUEST -> {
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
            selfieContainer.setOnClickListener { showCameraPreview() }
            updateButton.setOnClickListener { saveSelfie() }
        }
    }

    private fun loadSavedData() {
        previousSelfie = preferenceManager.getStringValue("selfie_photo_url")
        previousSelfie?.let { url ->
            if(url.isNotEmpty()) {
                loadImage(url, binding.selfieImage, binding.selfiePlaceholder)
            }
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

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA // Use front camera

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
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
                    if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showCameraPreview() {
        if (!checkCameraPermission()) {
            showPermissionDeniedDialog()
            return
        }

        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogCameraPreviewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        startCamera(dialogBinding.previewView)
        dialogBinding.switchCameraButton.visibility = View.VISIBLE
        // Add camera switch button click listener
        dialogBinding.switchCameraButton.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera(dialogBinding.previewView)
        }

        dialogBinding.captureButton.setOnClickListener {
            takePhoto()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    selfieUri = uri
                    loadImage(uri.toString(), binding.selfieImage, binding.selfiePlaceholder)
                }

                override fun onError(exception: ImageCaptureException) {
                    showToast("Failed to capture selfie")
                }
            }
        )
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile("DRIVER_SELFIE_${timeStamp}_", ".jpg", storageDir)
    }

    private fun saveSelfie() {
        when {
            selfieUri == null && previousSelfie != null -> {
                showToast("This Selfie already exists")
                return
            }
            selfieUri == null -> {
                showToast("Please click on camera icon to upload your selfie")
                return
            }
            else -> {
                uploadImage(selfieUri!!)
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Uploading Selfie")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            val request = object : VolleyFileUploadRequest(
                Method.POST,
                "${Constants.IMAGE_SERVER_URL}/upload",
                { response ->
                    try {
                        val jsonResponse = JSONObject(String(response.data))
                        val imageUrl = jsonResponse.getString("image_url")

                        preferenceManager.saveStringValue("selfie_photo_url", imageUrl)

                        runOnUiThread {
                            loadingDialog.dismiss()
                            showToast("Selfie uploaded successfully")
                            finish()
                        }
                    } catch (e: Exception) {
                        handleUploadError(e, loadingDialog)
                    }
                },
                { error -> handleUploadError(error, loadingDialog) }
            ) {
                override fun getByteData(): Map<String, DataPart> {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: ByteArray(0)
                    inputStream?.close()

                    val driverName = preferenceManager.getStringValue("goods_driver_name").ifEmpty { "NA" }
                    val driverId = preferenceManager.getStringValue("goods_driver_id")

                    return mapOf(
                        "image" to DataPart(
                            "goods_driver_id_${driverId}_${driverName}_selfie.jpg",
                            bytes,
                            "image/jpeg"
                        )
                    )
                }
            }

            VolleySingleton.getInstance(this).addToRequestQueue(request)

        } catch (e: Exception) {
            handleUploadError(e, loadingDialog)
        }
    }

    private fun handleUploadError(error: Exception, dialog: Dialog) {
        Log.e(TAG, "Upload failed", error)
        runOnUiThread {
            dialog.dismiss()
            showToast("Failed to upload selfie")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "OwnerSelfieUploadActivity"
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}