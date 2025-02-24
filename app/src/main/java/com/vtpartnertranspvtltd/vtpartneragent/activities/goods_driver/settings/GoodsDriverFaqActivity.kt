package com.vtpartnertranspvtltd.vtpartneragent.activities.goods_driver.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.vtpartnertranspvtltd.vtpartneragent.R
import com.vtpartnertranspvtltd.vtpartneragent.adapters.FAQAdapter
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityGoodsDriverFaqBinding
import com.vtpartnertranspvtltd.vtpartneragent.models.FAQ

class GoodsDriverFaqActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoodsDriverFaqBinding

    private val faqList = listOf(
        FAQ(
            "Driver Registration",
            "How do I register as a goods driver agent?",
            "To register as a goods driver agent, complete the registration form with your personal information, vehicle details, and required documents. Once your details are verified, you'll be able to start accepting bookings."
        ),
        FAQ(
            "Booking Issues",
            "What should I do if I encounter issues with booking?",
            "If you experience issues with your booking, such as incorrect details or technical problems, please contact our support team through the app. We are available 24/7 to resolve any issues."
        ),
        FAQ(
            "Payment Methods",
            "What are the available payment methods for goods bookings?",
            "We accept payments via various methods, including credit/debit cards, digital wallets, and cash. You can choose your preferred payment method when completing the booking process."
        ),
        FAQ(
            "Distance and Charges",
            "How are the distance and charges calculated for goods delivery?",
            "The charges are based on the distance of the delivery route and the size of the goods. The fare is calculated automatically within the app, including any applicable taxes."
        ),
        FAQ(
            "Profile Update",
            "How can I update my profile or vehicle details?",
            "You can update your profile, vehicle details, and availability from the 'Profile' section of the app. Ensure your information is always accurate for efficient booking management."
        ),
        FAQ(
            "Driver Support",
            "How do I get support if I have issues during my ride?",
            "If you face any issues during the ride, such as delays or technical difficulties, you can contact support through the app. Our team will assist you promptly to resolve any problems."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsDriverFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
//        setupToolbar()
        setupRecyclerView()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.apply {
                title = "FAQ's"
                setSupportActionBar(this)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                // Handle navigation click directly on toolbar
                setNavigationOnClickListener {
                    finish()
                }
            }

        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "FAQs"
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GoodsDriverFaqActivity)
            adapter = FAQAdapter(faqList)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }
}