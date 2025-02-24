package com.vtpartnertranspvtltd.vtpartneragent.utils

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.DialogImagePreviewBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ImagePreviewDialog : DialogFragment() {
    private lateinit var binding: DialogImagePreviewBinding
    private var documentName: String = ""
    private var imageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogImagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments
        documentName = arguments?.getString(ARG_DOCUMENT_NAME) ?: ""
        imageUrl = arguments?.getString(ARG_IMAGE_URL) ?: ""

        setupUI()
        loadImage()
    }

    private fun setupUI() {
        binding.apply {
            // Set document name
            documentName.text = documentName.toString()

            // Close button
            closeButton.setOnClickListener { dismiss() }

            // Share button
            shareButton.setOnClickListener { shareImage() }

            // Download button
            downloadButton.setOnClickListener { downloadImage() }

            // Setup PhotoView
            imageView.apply {
                maximumScale = 5f
                mediumScale = 3f
            }
        }
    }

    private fun loadImage() {
        binding.apply {
            progressBar.visibility = View.VISIBLE

            Glide.with(requireContext())
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        showError("Failed to load image")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView)
        }
    }

    private fun shareImage() {
        lifecycleScope.launch {
            try {
                val bitmap = Glide.with(requireContext())
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()

                val uri = saveImageToCache(bitmap)
                shareImageUri(uri, documentName)
            } catch (e: Exception) {
                showError("Failed to share image")
            }
        }
    }

    private fun downloadImage() {
        if (checkStoragePermission()) {
            startDownload()
        } else {
            requestStoragePermission()
        }
    }

    private fun startDownload() {
        val request = DownloadManager.Request(Uri.parse(imageUrl))
            .setTitle(documentName)
            .setDescription("Downloading document...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "KASP_PARTNER/${documentName}.jpg"
            )

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        showSuccess("Download started")
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.red))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            .show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.green))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            .show()
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri {
        val imagesFolder = File(requireContext().cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "shared_image.png")

        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        stream.flush()
        stream.close()

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
    }

    private fun shareImageUri(uri: Uri, name: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Image"))
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload()
            } else {
                showError("Storage permission required for download")
            }
        }
    }

    companion object {
        private const val ARG_DOCUMENT_NAME = "document_name"
        private const val ARG_IMAGE_URL = "image_url"
        private const val STORAGE_PERMISSION_CODE = 100

        fun newInstance(name: String, imageUrl: String) = ImagePreviewDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_DOCUMENT_NAME, name)
                putString(ARG_IMAGE_URL, imageUrl)
            }
        }
    }
}