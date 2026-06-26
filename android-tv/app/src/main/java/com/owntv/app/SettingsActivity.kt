package com.owntv.app

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Settings activity for configuring the IPTV API server connection.
 * Simple TV-optimized settings with D-pad navigation support.
 */
class SettingsActivity : FragmentActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private lateinit var apiClient: ApiClient
    private lateinit var serverUrlEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var testButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        apiClient = ApiClient(this)

        initViews()
        loadCurrentSettings()
    }

    private fun initViews() {
        serverUrlEditText = findViewById(R.id.server_url_input)
        saveButton = findViewById(R.id.save_button)
        testButton = findViewById(R.id.test_button)
        statusTextView = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.settings_progress)

        // Save button
        saveButton.setOnClickListener {
            val url = serverUrlEditText.text.toString().trim()
            if (url.isNotBlank()) {
                apiClient.setServerUrl(url)
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = "✅ 服务器地址已保存"
                statusTextView.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
            }
        }

        // Test connection button
        testButton.setOnClickListener {
            testServerConnection()
        }

        // Focus handling — ensure save button gets focus for TV navigation
        saveButton.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                saveButton.setBackgroundColor(
                    resources.getColor(android.R.color.holo_blue_light, null)
                )
            } else {
                saveButton.setBackgroundColor(
                    resources.getColor(android.R.color.darker_gray, null)
                )
            }
        }

        testButton.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                testButton.setBackgroundColor(
                    resources.getColor(android.R.color.holo_blue_light, null)
                )
            } else {
                testButton.setBackgroundColor(
                    resources.getColor(android.R.color.darker_gray, null)
                )
            }
        }
    }

    private fun loadCurrentSettings() {
        val currentUrl = apiClient.getServerUrl()
        serverUrlEditText.setText(currentUrl)
        serverUrlEditText.setSelection(currentUrl.length)
    }

    private fun testServerConnection() {
        val url = serverUrlEditText.text.toString().trim()
        if (url.isBlank()) {
            statusTextView.visibility = View.VISIBLE
            statusTextView.text = "⚠️ 请输入服务器地址"
            statusTextView.setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
            return
        }

        // Save URL first
        apiClient.setServerUrl(url)

        statusTextView.visibility = View.VISIBLE
        statusTextView.text = "⏳ 正在测试连接…"
        statusTextView.setTextColor(resources.getColor(android.R.color.white, null))
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = apiClient.testConnection()

            progressBar.visibility = View.GONE

            result.onSuccess { success ->
                if (success) {
                    statusTextView.text = "✅ 连接成功！服务器正常运行"
                    statusTextView.setTextColor(
                        resources.getColor(android.R.color.holo_green_light, null)
                    )
                } else {
                    statusTextView.text = "⚠️ 服务器返回异常，请检查地址是否正确"
                    statusTextView.setTextColor(
                        resources.getColor(android.R.color.holo_orange_light, null)
                    )
                }
            }

            result.onFailure { error ->
                Log.e(TAG, "Connection test failed", error)
                statusTextView.text = "❌ 连接失败: ${error.message}"
                statusTextView.setTextColor(
                    resources.getColor(android.R.color.holo_red_light, null)
                )
            }
        }
    }

    /**
     * Handle D-pad back button to return to main screen.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
