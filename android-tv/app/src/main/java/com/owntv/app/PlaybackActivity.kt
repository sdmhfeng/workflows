package com.owntv.app

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

/**
 * Full-screen video playback activity using ExoPlayer.
 * Designed for TV remote control (D-pad) interaction.
 */
class PlaybackActivity : FragmentActivity() {

    companion object {
        private const val TAG = "PlaybackActivity"
        private const val HIDE_CONTROLS_DELAY_MS = 5000L
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var channelNameText: TextView
    private lateinit var bufferingIndicator: ProgressBar

    private var channelName: String = ""
    private var channelUrl: String = ""
    private var isControlsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)

        channelName = intent.getStringExtra(MainActivity.EXTRA_CHANNEL_NAME) ?: "Unknown"
        channelUrl = intent.getStringExtra(MainActivity.EXTRA_CHANNEL_URL) ?: ""

        if (channelUrl.isBlank()) {
            Toast.makeText(this, R.string.error_playback, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        initPlayer()
    }

    private fun initViews() {
        playerView = findViewById(R.id.player_view)
        channelNameText = findViewById(R.id.channel_name)
        bufferingIndicator = findViewById(R.id.buffering_indicator)

        channelNameText.text = channelName

        // Hide system UI for immersive playback
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        // Click to toggle controls
        playerView.setOnClickListener {
            toggleControls()
        }
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
            setHandleAudioBecomingNoisy(true)

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            bufferingIndicator.visibility = View.VISIBLE
                        }
                        Player.STATE_READY -> {
                            bufferingIndicator.visibility = View.GONE
                        }
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Playback ended")
                            finish()
                        }
                        else -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error", error)
                    bufferingIndicator.visibility = View.GONE
                    Toast.makeText(
                        this@PlaybackActivity,
                        "${getString(R.string.error_playback)}: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    // Return to channel list after error
                    finish()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) {
                        bufferingIndicator.visibility = View.VISIBLE
                    }
                }
            })
        }

        playerView.player = player

        // Prepare the media source
        val uri = Uri.parse(channelUrl)
        val mediaItem = MediaItem.fromUri(uri)

        // Use appropriate media source factory based on URL scheme
        val mediaSourceFactory = when {
            channelUrl.contains(".m3u8") || channelUrl.contains("hls") -> {
                HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
            }
            channelUrl.contains(".mpd") || channelUrl.contains("dash") -> {
                DashMediaSource.Factory(DefaultHttpDataSource.Factory())
            }
            else -> null // Use default progressive download
        }

        if (mediaSourceFactory != null) {
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            player?.setMediaSource(mediaSource)
        } else {
            player?.setMediaItem(mediaItem)
        }

        player?.prepare()
    }

    /**
     * Toggle visibility of playback controls (channel name, etc.).
     */
    private fun toggleControls() {
        if (isControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        isControlsVisible = true
        channelNameText.animate().alpha(1f).duration = 200
        channelNameText.visibility = View.VISIBLE
    }

    private fun hideControls() {
        isControlsVisible = false
        channelNameText.animate().alpha(0f).duration = 500
        channelNameText.postDelayed({
            channelNameText.visibility = View.GONE
        }, 500)
    }

    /**
     * Handle D-pad controls for TV remote.
     * - D-pad center: toggle controls
     * - Back: exit playback
     * - Media play/pause: toggle play/pause
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                toggleControls()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                releasePlayer()
                finish()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // Seek backward 10 seconds
                player?.seekTo((player?.currentPosition ?: 0) - 10000)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Seek forward 10 seconds
                player?.seekTo((player?.currentPosition ?: 0) + 10000)
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                // Previous channel (placeholder for future channel list)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Next channel (placeholder for future channel list)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun releasePlayer() {
        player?.apply {
            stop()
            release()
        }
        player = null
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            releasePlayer()
        } else {
            player?.playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
