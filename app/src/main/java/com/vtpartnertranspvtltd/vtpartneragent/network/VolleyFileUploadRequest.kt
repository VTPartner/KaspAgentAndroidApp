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

abstract class VolleyFileUploadRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, errorListener) {

    private val boundary = "boundary=${System.currentTimeMillis()}"

    override fun getBodyContentType(): String {
        return "multipart/form-data; boundary=$boundary"
    }

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val dataOutputStream = DataOutputStream(byteArrayOutputStream)

        try {
            // Add data parts
            for ((key, dataPart) in getByteData()) {
                dataOutputStream.writeBytes("--$boundary\r\n")
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"${dataPart.fileName}\"\r\n")
                dataOutputStream.writeBytes("Content-Type: ${dataPart.mimeType}\r\n\r\n")

                dataOutputStream.write(dataPart.data)
                dataOutputStream.writeBytes("\r\n")
            }

            // Close multipart form data
            dataOutputStream.writeBytes("--$boundary--\r\n")

            return byteArrayOutputStream.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                dataOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ByteArray(0)
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: NetworkResponse) {
        listener.onResponse(response)
    }

    abstract fun getByteData(): Map<String, DataPart>

    class DataPart(
        val fileName: String,
        val data: ByteArray,
        val mimeType: String
    )
}