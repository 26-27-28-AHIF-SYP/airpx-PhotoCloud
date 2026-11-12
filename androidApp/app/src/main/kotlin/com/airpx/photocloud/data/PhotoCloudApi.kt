package com.airpx.photocloud.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PhotoCloudApi(private val baseUrl: String, private val token: String) {
    fun login(email: String, password: String): String {
        val connection = open("/auth/login", "POST", "application/json")
        connection.outputStream.use { it.write(JSONObject().put("email", email).put("password", password).toString().toByteArray()) }
        val response = connection.responseCode
        val body = (if (response in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (response !in 200..299) error("Anmeldung fehlgeschlagen ($response)")
        return JSONObject(body).getString("accessToken")
    }

    fun checkServer(): Boolean {
        val url = if (baseUrl.trimEnd('/').endsWith("/api")) baseUrl.trimEnd('/') else "${baseUrl.trimEnd('/')}/api"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return try { connection.responseCode == 200 } finally { connection.disconnect() }
    }

    fun missing(hashes: List<String>): Set<String> {
        if (hashes.isEmpty()) return emptySet()
        val result = request("/photos/missing", "POST", JSONObject().put("hashes", JSONArray(hashes)).toString())
        return (0 until result.getJSONArray("missingHashes").length()).map { result.getJSONArray("missingHashes").getString(it) }.toSet()
    }

    fun upload(uriName: String, capturedAt: String?, hash: String, size: Long, mimeType: String, read: (java.io.OutputStream) -> Unit) {
        val boundary = "AirPx-${System.nanoTime()}"
        val connection = open("/photos", "POST", "multipart/form-data; boundary=$boundary")
        connection.setChunkedStreamingMode(64 * 1024)
        DataOutputStream(connection.outputStream).use { out ->
            fun field(name: String, value: String) {
                out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
            }
            field("filename", uriName.substringAfterLast('/').ifBlank { "photo" })
            capturedAt?.let { field("capturedAt", it) }
            field("sha256", hash); field("size", size.toString()); field("mimeType", mimeType)
            out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo\"\r\nContent-Type: $mimeType\r\n\r\n")
            read(out)
            out.writeBytes("\r\n--$boundary--\r\n")
        }
        val response = connection.responseCode
        connection.disconnect()
        if (response !in 200..299) error("Upload fehlgeschlagen ($response)")
    }

    private fun request(path: String, method: String, body: String): JSONObject {
        val connection = open(path, method, "application/json")
        connection.outputStream.use { it.write(body.toByteArray()) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) error("Serverantwort $status")
        return JSONObject(text)
    }

    private fun open(path: String, method: String, contentType: String): HttpURLConnection {
        val prefix = if (baseUrl.trimEnd('/').endsWith("/api")) baseUrl.trimEnd('/') else "${baseUrl.trimEnd('/')}/api"
        return (URL("$prefix$path").openConnection() as HttpURLConnection).apply {
        requestMethod = method; connectTimeout = 20_000; readTimeout = 120_000; doInput = true
        setRequestProperty("Content-Type", contentType)
        if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        doOutput = method == "POST"
        }
    }
}
