package com.vtpartnertranspvtltd.vtpartneragent.network

import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.Collections.min
import kotlin.math.min

open class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
    ) : Request<NetworkResponse>(method, url, errorListener) {

        private var responseListener: Response.Listener<NetworkResponse>? = null

        init {
            this.responseListener = listener
        }

        override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
            return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
        }

        override fun deliverResponse(response: NetworkResponse) {
            responseListener?.onResponse(response)
        }

        override fun getBodyContentType(): String {
            return "multipart/form-data; boundary=${BOUNDARY}"
        }

        @Throws(AuthFailureError::class)
        override fun getBody(): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val dataOutputStream = DataOutputStream(byteArrayOutputStream)

            try {
                // Add byte data
                for ((key, value) in getByteData()) {
                    buildPart(dataOutputStream, value.data, key, value.type)
                }

                // End of multipart/form-data
                dataOutputStream.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END)

                return byteArrayOutputStream.toByteArray()

            } catch (e: IOException) {
                e.printStackTrace()
            }
            return super.getBody()
        }

        @Throws(IOException::class)
        private fun buildPart(dataOutputStream: DataOutputStream, fileData: ByteArray, fileName: String, mimeType: String) {
            dataOutputStream.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$fileName\"; filename=\"$fileName\"$LINE_END")
            dataOutputStream.writeBytes("Content-Type: $mimeType$LINE_END")
            dataOutputStream.writeBytes(LINE_END)

            val fileInputStream = ByteArrayInputStream(fileData)
            var bytesAvailable = fileInputStream.available()
            val maxBufferSize = 1024 * 1024
            var bufferSize = min(bytesAvailable, maxBufferSize)
            val buffer = ByteArray(bufferSize)

            var bytesRead = fileInputStream.read(buffer, 0, bufferSize)
            while (bytesRead > 0) {
                dataOutputStream.write(buffer, 0, bufferSize)
                bytesAvailable = fileInputStream.available()
                bufferSize = min(bytesAvailable, maxBufferSize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize)
            }

            dataOutputStream.writeBytes(LINE_END)
        }

        open fun getByteData(): Map<String, DataPart> = HashMap()

        companion object {
            private const val LINE_END = "\r\n"
            private const val TWO_HYPHENS = "--"
            private  val BOUNDARY = "apiclient-" + System.currentTimeMillis()
        }

        class DataPart(
            val fileName: String,
            val data: ByteArray,
            val type: String
        )
    }