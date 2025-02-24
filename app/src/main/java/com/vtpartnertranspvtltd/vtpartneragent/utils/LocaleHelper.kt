package com.vtpartnertranspvtltd.vtpartneragent.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            configuration.locale = locale
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }

    fun getLanguageName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "hi" -> "हिंदी"
            "kn" -> "ಕನ್ನಡ"
            "mr" -> "मराठी"
            "ta" -> "தமிழ்"
            "te" -> "తెలుగు"
            "bn" -> "বাংলা"
            "ml" -> "മലയാളം"
            else -> "English"
        }
    }
} 