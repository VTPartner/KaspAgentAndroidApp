package com.vtpartnertranspvtltd.vtpartneragent.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vtpartnertranspvtltd.vtpartneragent.databinding.ActivityLanguageSelectionBinding

import com.vtpartnertranspvtltd.vtpartneragent.utils.LocaleHelper
import com.vtpartnertranspvtltd.vtpartneragent.utils.PreferenceManager

class LanguageSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLanguageSelectionBinding
    private val preferenceManager by lazy { PreferenceManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLanguageSelection()
    }

    private fun setupLanguageSelection() {
        val currentLanguage = preferenceManager.getLanguage() ?: "en"
        updateSelectedLanguage(currentLanguage)

        binding.apply {
            englishCard.setOnClickListener { selectLanguage("en") }
            hindiCard.setOnClickListener { selectLanguage("hi") }
//            kannadaCard.setOnClickListener { selectLanguage("kn") }
//            marathiCard.setOnClickListener { selectLanguage("mr") }
//            tamilCard.setOnClickListener { selectLanguage("ta") }
//            teluguCard.setOnClickListener { selectLanguage("te") }
//            bengaliCard.setOnClickListener { selectLanguage("bn") }
//            malayalamCard.setOnClickListener { selectLanguage("ml") }

            continueButton.setOnClickListener {
                startActivity(Intent(this@LanguageSelectionActivity, DriverTypeSelectionActivity::class.java))
                finish()
            }
        }
    }

    private fun selectLanguage(languageCode: String) {
        preferenceManager.saveLanguage(languageCode)
        LocaleHelper.setLocale(this, languageCode)
        updateSelectedLanguage(languageCode)
        recreate()
    }

    private fun updateSelectedLanguage(languageCode: String) {
        binding.apply {
            englishCheck.visibility = if (languageCode == "en") View.VISIBLE else View.GONE
            hindiCheck.visibility = if (languageCode == "hi") View.VISIBLE else View.GONE
//            kannadaCheck.visibility = if (languageCode == "kn") View.VISIBLE else View.GONE
//            marathiCheck.visibility = if (languageCode == "mr") View.VISIBLE else View.GONE
//            tamilCheck.visibility = if (languageCode == "ta") View.VISIBLE else View.GONE
//            teluguCheck.visibility = if (languageCode == "te") View.VISIBLE else View.GONE
//            bengaliCheck.visibility = if (languageCode == "bn") View.VISIBLE else View.GONE
//            malayalamCheck.visibility = if (languageCode == "ml") View.VISIBLE else View.GONE
        }
    }
} 