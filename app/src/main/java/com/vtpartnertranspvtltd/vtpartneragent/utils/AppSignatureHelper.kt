package com.vtpartnertranspvtltd.vt_partner.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest
import java.util.Arrays

class AppSignatureHelper(private val context: Context) {
    val appSignatures: ArrayList<String>
        get() {
            val appSignatures = ArrayList<String>()
            try {
                val packageName = context.packageName
                val packageManager = context.packageManager
                val signatures = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures

                signatures.forEach { signature ->
                    val hash = hash(packageName, signature.toCharsString())
                    if (hash != null) {
                        appSignatures.add(String.format("%s", hash))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
            return appSignatures
        }

    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        try {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(appInfo.toByteArray(charset("UTF-8")))
            val hashSignature = messageDigest.digest()

            // truncated into NUM_HASHED_BYTES
            val hashBytes = Arrays.copyOfRange(hashSignature, 0, 9)
            // encode into Base64
            var base64Hash = Base64.encodeToString(hashBytes, Base64.NO_PADDING or Base64.NO_WRAP)
            base64Hash = base64Hash.substring(0, 11)
            return base64Hash
        } catch (e: Exception) {
            // Handle error
        }
        return null
    }
} 