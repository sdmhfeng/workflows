package com.owntv.app

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

/**
 * Embedded HTTP server that serves bundled M3U/TXT channel data from assets.
 * This allows the APK to be self-contained — the player connects to its own
 * local server instead of requiring an external IPTV API server.
 *
 * Endpoints:
 *   GET /     — simple status page
 *   GET /m3u  — M3U playlist from assets/result.m3u
 *   GET /txt  — TXT channel list from assets/result.txt
 */
class LocalServer(
    private val context: Context,
    port: Int = DEFAULT_PORT
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "LocalServer"
        const val DEFAULT_PORT = 8080
        private const val ASSET_M3U = "result.m3u"
        private const val ASSET_TXT = "result.txt"
        private const val MIME_M3U = "audio/x-mpegurl"
        private const val MIME_TXT = "text/plain; charset=utf-8"
        private const val MIME_HTML = "text/html; charset=utf-8"
    }

    /**
     * Reads a file from the app's assets folder.
     * @return file content as String, or null if not found / error.
     */
    private fun readAsset(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read asset: $fileName", e)
            null
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d(TAG, "Request: $uri")

        return when {
            uri == "/" || uri == "" -> serveHomePage()
            uri == "/m3u" -> serveAsset(ASSET_M3U, MIME_M3U)
            uri == "/txt" -> serveAsset(ASSET_TXT, MIME_TXT)
            uri == "/favicon.ico" -> NanoHTTPD.newFixedLengthResponse(
                Response.Status.NOT_FOUND, MIME_TXT, ""
            )
            else -> NanoHTTPD.newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_TXT,
                "404 Not Found: $uri"
            )
        }
    }

    /**
     * Serves the root page with basic status info.
     */
    private fun serveHomePage(): Response {
        val m3uContent = readAsset(ASSET_M3U)
        val channelCount = m3uContent?.lines()?.count { line ->
            line.startsWith("#EXTINF:")
        } ?: 0

        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><title>ownTV Server</title></head>
            <body style="font-family: sans-serif; padding: 20px;">
                <h2>📺 ownTV 内置服务器</h2>
                <p>服务器运行正常 ✅</p>
                <ul>
                    <li><a href="/m3u">M3U 播放列表</a> — ${channelCount} 个频道</li>
                    <li><a href="/txt">TXT 频道列表</a></li>
                </ul>
                <p><small>ownTV Embedded Server v1.0.0</small></p>
            </body>
            </html>
        """.trimIndent()

        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_HTML, html)
    }

    /**
     * Serves an asset file with the given MIME type.
     * Returns 503 if the asset file is not available.
     */
    private fun serveAsset(fileName: String, mimeType: String): Response {
        val content = readAsset(fileName)
        return if (content != null) {
            NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeType, content)
        } else {
            NanoHTTPD.newFixedLengthResponse(
                Response.Status.SERVICE_UNAVAILABLE,
                MIME_TXT,
                "频道数据未加载，请稍后再试"
            )
        }
    }
}
