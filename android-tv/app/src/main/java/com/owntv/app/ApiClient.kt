package com.owntv.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for communicating with the IPTV-API Flask server.
 * Fetches M3U playlists and other data from the backend.
 */
class ApiClient(context: Context) {

    companion object {
        private const val TAG = "ApiClient"
        private const val PREF_NAME = "owntv_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "http://127.0.0.1:8080"
        private const val CONNECT_TIMEOUT = 10L
        private const val READ_TIMEOUT = 30L
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Gets the configured server URL.
     */
    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    /**
     * Saves a new server URL.
     */
    fun setServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url.trimEnd('/')).apply()
    }

    /**
     * Fetches the M3U playlist from the server.
     * @return The raw M3U content string, or null on failure.
     */
    suspend fun fetchM3uPlaylist(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            val url = "$serverUrl/m3u"
            Log.d(TAG, "Fetching M3U from: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ownTV-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP error: ${response.code}")
                return@withContext Result.failure(
                    IOException("服务器返回错误: ${response.code}")
                )
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                return@withContext Result.failure(
                    IOException("服务器返回空数据")
                )
            }

            Log.d(TAG, "Fetched ${body.length} bytes of M3U data")
            Result.success(body)
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching M3U", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching M3U", e)
            Result.failure(e)
        }
    }

    /**
     * Tests connection to the configured server.
     * @return true if server is reachable and returns valid data.
     */
    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            val request = Request.Builder()
                .url(serverUrl)
                .header("User-Agent", "ownTV-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
