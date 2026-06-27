package com.owntv.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Main activity for the ownTV Android TV app.
 * Displays channel list using Leanback BrowseFragment pattern.
 */
class MainActivity : FragmentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_CHANNEL_URL = "channel_url"
    }

    private lateinit var apiClient: ApiClient
    private var channels: List<Channel> = emptyList()
    private var isDataLoaded = false
    private var localServer: LocalServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiClient = ApiClient(this)

        // Start the embedded HTTP server that serves bundled M3U data.
        // The app connects to this local server to get channel lists,
        // making the APK fully self-contained.
        startLocalServer()

        if (savedInstanceState == null) {
            showChannelListFragment()
        }

        loadChannels()
    }

    /**
     * Starts the embedded NanoHTTPD server on a background thread.
     * This server serves the bundled M3U/TXT channel data from assets.
     */
    private fun startLocalServer() {
        localServer = LocalServer(this, LocalServer.DEFAULT_PORT).apply {
            try {
                start()
                Log.d(TAG, "Local server started on port ${LocalServer.DEFAULT_PORT}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start local server", e)
            }
        }
    }

    /**
     * Shows the main channel browsing fragment.
     */
    private fun showChannelListFragment() {
        val fragment = ChannelListFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }

    /**
     * Loads channel data from the configured IPTV API server.
     * Shows loading state and error handling.
     */
    private fun loadChannels() {
        lifecycleScope.launch {
            try {
                val fragment = supportFragmentManager
                    .findFragmentById(R.id.main_fragment_container) as? ChannelListFragment
                fragment?.showLoading(true)

                val result = apiClient.fetchM3uPlaylist()

                result.onSuccess { m3uContent ->
                    channels = M3uParser.parse(m3uContent)
                    isDataLoaded = true

                    if (channels.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.no_channels,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    val groupedChannels = M3uParser.groupByTitle(channels)
                    fragment?.updateChannels(groupedChannels)

                    Log.d(TAG, "Loaded ${channels.size} channels in ${groupedChannels.size} groups")
                }

                result.onFailure { error ->
                    Log.e(TAG, "Failed to load channels", error)
                    Toast.makeText(
                        this@MainActivity,
                        "${getString(R.string.error_network)}: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    fragment?.showLoading(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading channels", e)
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.error_parse)}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Handles a channel being selected — launches playback.
     */
    fun onChannelSelected(channel: Channel) {
        val intent = Intent(this, PlaybackActivity::class.java).apply {
            putExtra(EXTRA_CHANNEL_NAME, channel.getDisplayName())
            putExtra(EXTRA_CHANNEL_URL, channel.url)
        }
        startActivity(intent)
    }

    /**
     * Reload channel list (called on refresh action).
     */
    fun refreshChannels() {
        loadChannels()
    }

    /**
     * Opens settings.
     */
    fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    /**
     * Handle D-pad and remote control keys for TV navigation.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                openSettings()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                refreshChannels()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload channels when returning from settings
        // (server URL may have changed)
        if (isDataLoaded) {
            refreshChannels()
        }
    }

    override fun onDestroy() {
        localServer?.stop()
        localServer = null
        super.onDestroy()
    }
}
