package com.vtpartnertranspvtltd.vtpartneragent.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ItemFaqBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.FAQ

class FAQAdapter(private val faqs: List<FAQ>) :
    RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    private var expandedPosition = -1

    inner class FAQViewHolder(private val binding: ItemFaqBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(faq: FAQ, position: Int) {
            binding.apply {
                titleText.text = faq.title
                questionText.text = faq.question
                answerText.text = faq.answer

                val isExpanded = position == expandedPosition
                answerText.visibility = if (isExpanded) View.VISIBLE else View.GONE
                expandIcon.setImageResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )

                root.setOnClickListener {
                    expandedPosition = if (isExpanded) -1 else position
                    notifyDataSetChanged()
                }

                // Apply colors and styles
                expandIcon.setColorFilter(
                    ContextCompat.getColor(root.context, R.color.primary_dark)
                )
                titleText.setTextColor(
                    ContextCompat.getColor(root.context, R.color.grey)
                )
                questionText.setTextColor(
                    ContextCompat.getColor(root.context, R.color.black)
                )
                answerText.setTextColor(
                    ContextCompat.getColor(root.context, R.color.grey)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        return FAQViewHolder(
            ItemFaqBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, 
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        holder.bind(faqs[position], position)
    }

    override fun getItemCount() = faqs.size
}