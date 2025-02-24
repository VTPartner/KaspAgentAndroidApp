package com.vtpartnertranspvtltd.vtpartneragent.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemDocumentBinding
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemProfileDocumentBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.Document

class DocumentsAdapter(
    private val documents: List<Document>,
    private val onDocumentClick: (Document) -> Unit
) : RecyclerView.Adapter<DocumentsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemProfileDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(document: Document) {
            binding.apply {
                // Set document name
                documentName.text = document.name

                // Load image with Glide
                Glide.with(itemView.context)
                    .load(document.imageUrl)
                    .placeholder(R.drawable.ic_document)
                    .error(R.drawable.ic_document)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(documentImage)

                // Set status indicator color
                statusIndicator.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        when {
                            document.imageUrl.isEmpty() -> R.color.red
                            else -> R.color.green
                        }
                    )
                )

                // Set click listener
                root.setOnClickListener {
                    if (document.imageUrl.isNotEmpty()) {
                        onDocumentClick(document)
                    } else {
                        showUploadPrompt(itemView.context, document)
                    }
                }

                // Optional: Add shimmer effect while loading
//                shimmerLayout.apply {
//                    if (document.imageUrl.isEmpty()) {
//                        startShimmer()
//                        visibility = View.VISIBLE
//                    } else {
//                        stopShimmer()
//                        visibility = View.GONE
//                    }
//                }
            }
        }

        private fun showUploadPrompt(context: Context, document: Document) {
            MaterialAlertDialogBuilder(context)
                .setTitle("Upload Document")
                .setMessage("Would you like to upload ${document.name}?")
                .setPositiveButton("Upload") { dialog, _ ->
                    // Handle document upload
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProfileDocumentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(documents[position])
    }

    override fun getItemCount() = documents.size
}