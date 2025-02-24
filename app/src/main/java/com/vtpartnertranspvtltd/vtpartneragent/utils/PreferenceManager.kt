package com.vtpartnertranspvtltd.vtpartneragent.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.vtpartnertranspvtltd.vtpartneragent.data.models.UserData

class PreferenceManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "VTPartnerPrefs"
        private const val KEY_DRIVER_ID = "goods_driver_id"
        private const val KEY_DRIVER_NAME = "driver_name"
        private const val KEY_PROFILE_PIC = "profile_pic"
        private const val KEY_MOBILE_NO = "mobile_no"
        private const val KEY_FULL_ADDRESS = "full_address"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_LICENSE_FRONT = "license_front_photo_url"
        private const val KEY_LICENSE_BACK = "license_back_photo_url"
        private const val KEY_LICENSE_NUMBER = "license_no"
        private const val KEY_AADHAR_FRONT = "aadhar_front_photo_url"
        private const val KEY_AADHAR_BACK = "aadhar_back_photo_url"
        private const val KEY_AADHAR_NUMBER = "aadhar_no"
        private const val KEY_PAN_FRONT = "pan_front_photo_url"
        private const val KEY_PAN_BACK = "pan_back_photo_url"
        private const val KEY_PAN_NUMBER = "pan_no"
        private const val KEY_SELFIE = "selfie_photo_url"
        private const val KEY_DRIVER_GENDER = "driver_gender"
        private const val KEY_DRIVER_CITY_ID = "driver_city_id"
        private const val KEY_SELFIE_URL = "recent_online_pic"
        private const val KEY_CURRENT_BOOKING = "current_booking_id_assigned"
        private const val PREF_DRIVER_TYPE = "driver_type"
        private const val PREF_LANGUAGE = "app_language"

        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun saveUserData(userData: UserData) {
        editor.putString(KEY_DRIVER_ID, userData.driverId)
        editor.putString(KEY_DRIVER_NAME, userData.name)
        editor.putString(KEY_PROFILE_PIC, userData.profilePic)
        editor.putString(KEY_MOBILE_NO, userData.mobileNo)
        editor.putString(KEY_FULL_ADDRESS, userData.address)
        editor.apply()
    }

    fun saveAuthToken(token: String) {
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.apply()
    }

    fun getDriverId(): String? = prefs.getString(KEY_DRIVER_ID, null)
    fun setDriverId(id: String) = prefs.edit().putString(KEY_DRIVER_ID, id).apply()

    fun getDriverName(): String = prefs.getString(KEY_DRIVER_NAME, "") ?: ""
    fun getProfilePic(): String = prefs.getString(KEY_PROFILE_PIC, "") ?: ""
    fun getMobileNo(): String = prefs.getString(KEY_MOBILE_NO, "") ?: ""
    fun getFullAddress(): String = prefs.getString(KEY_FULL_ADDRESS, "") ?: ""
    fun getAuthToken(): String = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""

    fun getLicenseFrontUrl(): String = prefs.getString(KEY_LICENSE_FRONT, "") ?: ""
    fun getLicenseBackUrl(): String = prefs.getString(KEY_LICENSE_BACK, "") ?: ""
    fun getLicenseNumber(): String = prefs.getString(KEY_LICENSE_NUMBER, "") ?: ""
    fun getAadharFrontUrl(): String = prefs.getString(KEY_AADHAR_FRONT, "") ?: ""
    fun getAadharBackUrl(): String = prefs.getString(KEY_AADHAR_BACK, "") ?: ""
    fun getAadharNumber(): String = prefs.getString(KEY_AADHAR_NUMBER, "") ?: ""
    fun getPanFrontUrl(): String = prefs.getString(KEY_PAN_FRONT, "") ?: ""
    fun getPanBackUrl(): String = prefs.getString(KEY_PAN_BACK, "") ?: ""
    fun getPanNumber(): String = prefs.getString(KEY_PAN_NUMBER, "") ?: ""
    fun getSelfieUrl(): String = prefs.getString(KEY_SELFIE_URL, "")?:""
    fun getDriverGender(): String = prefs.getString(KEY_DRIVER_GENDER, "") ?: ""
    fun getDriverCityId(): String = prefs.getString(KEY_DRIVER_CITY_ID, "") ?: ""
    fun saveLicenseNumber(url: String) = editor.putString(KEY_LICENSE_NUMBER, url).apply()
    fun saveLicenseFrontUrl(url: String) = editor.putString(KEY_LICENSE_FRONT, url).apply()
    fun saveLicenseBackUrl(url: String) = editor.putString(KEY_LICENSE_BACK, url).apply()
    fun saveAadharFrontUrl(url: String) = editor.putString(KEY_AADHAR_FRONT, url).apply()
    fun saveAadharBackUrl(url: String) = editor.putString(KEY_AADHAR_BACK, url).apply()
    fun saveAadharNumber(number: String) = editor.putString(KEY_AADHAR_NUMBER, number).apply()
    fun savePanFrontUrl(url: String) = editor.putString(KEY_PAN_FRONT, url).apply()
    fun savePanBackUrl(url: String) = editor.putString(KEY_PAN_BACK, url).apply()
    fun savePanNumber(number: String) = editor.putString(KEY_PAN_NUMBER, number).apply()
    fun saveSelfieUrl(url: String?) = prefs.edit().putString(KEY_SELFIE_URL, url).apply()
    fun clearSelfieUrl() = prefs.edit().remove(KEY_SELFIE_URL).apply()
    fun saveDriverGender(gender: String) = editor.putString(KEY_DRIVER_GENDER, gender).apply()
    fun saveDriverCityId(cityId: String) = editor.putString(KEY_DRIVER_CITY_ID, cityId).apply()

    fun saveDriverName(name: String) = editor.putString(KEY_DRIVER_NAME, name).apply()
    fun saveDriverAddress(address: String) = editor.putString(KEY_FULL_ADDRESS, address).apply()

    fun getCurrentBookingId(): String? = prefs.getString(KEY_CURRENT_BOOKING, null)
    fun setCurrentBookingId(id: String?) = prefs.edit().putString(KEY_CURRENT_BOOKING, id).apply()

    fun clearAll() {
        editor.clear()
        editor.apply()
    }

    fun getDouble(key: String, defaultValue: Double): Double {
        return prefs.getString(key, defaultValue.toString())?.toDoubleOrNull() ?: defaultValue
    }

    fun saveDouble(key: String, value: Double) {
        prefs.edit().putString(key, value.toString()).apply()
    }

    fun saveBooleanValue(key:String,value:Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun saveIntValue(key:String,value:Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun saveStringValue(key:String,value:String) {
        prefs.edit().putString(key, value).apply()
    }
    fun saveFloatValue(key:String,value:Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getBooleanValue(key:String):Boolean {
        return prefs.getBoolean(key, false)?:false
    }

    fun getIntValue(key:String):Int {
        return prefs.getInt(key, 0)?:0
    }

    fun getStringValue(key:String):String {
        return prefs.getString(key, "")?:""
    }

    fun getFloatValue(key:String):Float {
        return prefs.getFloat(key,0F)
    }

    fun saveDriverType(type: String) {
        editor.putString(PREF_DRIVER_TYPE, type)
        editor.apply()
    }

    fun getDriverType(): String? {
        return prefs.getString(PREF_DRIVER_TYPE, null)
    }

    fun saveLanguage(languageCode: String) {
        editor.putString(PREF_LANGUAGE, languageCode).apply()
    }

    fun getLanguage(): String? {
        return prefs.getString(PREF_LANGUAGE, "en")
    }
} 