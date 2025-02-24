package com.vtpartnertranspvtltd.vtpartneragent.services

import com.android.volley.DefaultRetryPolicy
import com.android.volley.NoConnectionError
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.auth.oauth2.GoogleCredentials
import com.vtpartnertranspvtltd.vt_partner.utils.Constants
import com.vtpartnertranspvtltd.vtpartneragent.VTPartnerApp
import com.vtpartnertranspvtltd.vtpartneragent.network.VolleySingleton
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AccessToken {
    companion object {
        private var cachedToken: String? = null
        private var tokenExpiry: Long = 0

        fun getAccessToken(): String {
            // Return cached token if valid
            if (!cachedToken.isNullOrEmpty() && System.currentTimeMillis() < tokenExpiry) {
                return cachedToken!!
            }

            // Synchronously get new token
            return runBlocking {
                fetchNewToken()
            }
        }

        private suspend fun fetchNewToken(): String = suspendCoroutine { continuation ->
            val url = "${Constants.BASE_URL}get_customer_app_firebase_access_token"

            val request = object : JsonObjectRequest(
                Method.GET,
                url,
                null,
                { response ->
                    try {
                        when (response.getString("status")) {
                            "success" -> {
                                val token = response.getString("token")
                                // Cache token with 50 minutes expiry
                                cachedToken = token
                                tokenExpiry = System.currentTimeMillis() + (50 * 60 * 1000)
                                continuation.resume(token)
                            }
                            else -> {
                                val errorMessage = response.optString("message", "Unknown error")
                                continuation.resumeWithException(
                                    Exception("Failed to get access token: $errorMessage")
                                )
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                { error ->
                    val errorMessage = when (error) {
                        is NoConnectionError -> "No internet connection"
                        is TimeoutError -> "Request timed out"
                        is ServerError -> {
                            val serverResponse = String(
                                error.networkResponse?.data ?: ByteArray(0),
                                Charset.defaultCharset()
                            )
                            "Server error: $serverResponse"
                        }
                        else -> "Error: ${error.message}"
                    }
                    continuation.resumeWithException(Exception(errorMessage))
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return HashMap<String, String>().apply {
                        put("Content-Type", "application/json")
                    }
                }
            }

            request.retryPolicy = DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            VolleySingleton.getInstance(VTPartnerApp.instance).addToRequestQueue(request)
        }
    }
}

//object AccessToken {
//
//    private val firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging"
//
//    fun getAccessToken():String?{
//        try {
//            val jsonString:String ="{\n" +
//                    "  \"type\": \"service_account\",\n" +
//                    "  \"project_id\": \"vt-partner-agent-app\",\n" +
//                    "  \"private_key_id\": \"e3be501cb621104602759406778871c40254d5d1\",\n" +
//                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCx0JjFi/FCXXA/\\nj36lm4xn4kn0hiTcTbrXTn7HbMnNmIvve84zl5kKx+1S3x/rGf3bJFcwFnYQHHae\\n1MwTHpWmX/LSBaxoV8qO8AuCdHF1L4jPZMEkHhLvfZxZomKYaIJ+wwjI4n32+j2j\\nFtEQ9lvt5EPbhDnQq5Z4Wml74Ty946Id4+QDr9GPe2CCSPh0GsIii7hDWyZhQRno\\nG/LTTEYxDO6kURkU5yUGQh8Oq/vDGEFOomewtH5Ve9GB6g2fHN9t+ME6yODd8db1\\nL69UmawOlEGxLesXMfRi5qm/da01Cyr5Wv20xsqIz9F88GYWRlClDdDrski6ChKu\\nzyA+D2B/AgMBAAECggEAD9kIlKq5UPHk/DU802O+qs+XScze4ienXGMpl3qRrdpH\\ntotxQFXllPlmpHkwbhK99lcR0j5ePWdcByHuIlIagl6Q1LkeuZoBeqXYUMMyDYC0\\nD8/qCt8HTwMB+VcotG60GrolQUo2cdmxvqRc88cRQG2Uwq7RPFDes1FTj2/uqvnF\\nVu8cbE5NKnooqD1XGv0pmu4xSBLrGKB+qX3cOYiOQqFUddTa025GT0XEJTUiWzt4\\n96O9R8k+EsmI/MSjJe/Z3OMTdHbQ9BlM0hpsN07A+3tJL8WlemvmwO4ev/KfebUK\\nKPX1ELBE4dxq4QJxGnT+vysYKpMiPwuOSKo76wiAqQKBgQD3yyV82PgT5zHQIsSZ\\nfCfxehRMLX3NTqKRvIucxNjtN1aZcWsiwerkcdD7LLPmeWirZfv+5II/zSynDYTr\\nzT2A0GR2u+hiUFBnxjZY26YS8KBffns576Rn1oVL1+JhObUx0Dmi9n3WDUS7dfxz\\n6miruIdOHy7rqE+chJMG6TUJwwKBgQC3tCdmRaPo7OhvWmOo+tRUGDF2l6y8MCIt\\nzBVStVBWg06AAwYw3bJpzFHI5Yz8gkaFVjuxANrQNz6Qms4PbkBTfqOo4LB7+JqZ\\nxEkigQ+iqUU+PApsx5i3rr9CumqEoxZXNzzgnCDTx/dljnSTmD+XVH4gBlpkYXXb\\nLdmMPK9mlQKBgGR4xUl6/BOt4X/AKTEGq3d5BXPh2il94eLvrTgyhLaigoWS/FrK\\ngACCubauaH9h6PPeVTAD3WAbRCi0DZpCzNZHKQUPqej7Ia8CKpUa8pqpYI13zmUu\\nat4DmGapMUw0xuhcwpH2Gg3JsX3FGEiz2h8OoiYl9LNuumD/TFI4Ct5bAoGADrl0\\n8wCf+7qJgutm05OPU1JBHLVZlhfxlWQnTWLVFqodr6sOYvpSI6LJ52Vm4JJ8npFj\\n5XMhFtFmxWZzH8+Bfm/HJHEmFDnAApU2G3rmyu3wa+WaHE//ULHECNAyW4FK+CCo\\nU4SQKQl9Lfm2JGJurm2KUnzP3/3j2XaaWmA+2uUCgYEAq8WfftzlmefPK7lEI7Mb\\nUvZYjJAos9t7AV5s9hnxN6fEihJDquYf7f6rRse1UGsxiFZZ3vePM+/FNvj0UnFj\\ndxuJSqXw5RzF8bw5i9VHSY20CZ3Mnz68rcEx7NUQwjMv2cpolpiG/QJScobkj28p\\nU+9IUDdNKY6lGpMW/dCyrDY=\\n-----END PRIVATE KEY-----\\n\",\n" +
//                    "  \"client_email\": \"new-service-for-agent-app@vt-partner-agent-app.iam.gserviceaccount.com\",\n" +
//                    "  \"client_id\": \"102266092525388666285\",\n" +
//                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
//                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
//                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
//                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/new-service-for-agent-app%40vt-partner-agent-app.iam.gserviceaccount.com\",\n" +
//                    "  \"universe_domain\": \"googleapis.com\"\n" +
//                    "}\n"
//
//            val stream = ByteArrayInputStream(jsonString.toByteArray(StandardCharsets.UTF_8))
//
//            val googleCredential = GoogleCredentials.fromStream(stream)
//                .createScoped(arrayListOf(firebaseMessagingScope))
//
//            googleCredential.refresh()
//            return googleCredential.accessToken.tokenValue
//
//        }
//        catch (e: IOException){
//            return null
//        }
//    }
//}