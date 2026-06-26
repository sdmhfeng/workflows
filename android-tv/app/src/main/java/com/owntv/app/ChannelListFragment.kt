package com.owntv.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*

/**
 * Leanback BrowseFragment for displaying IPTV channel groups and channels.
 * Provides a TV-optimized browsing experience with rows per channel group.
 */
class ChannelListFragment : BrowseSupportFragment() {

    companion object {
        private const val TAG = "ChannelListFragment"
    }

    private var channelsByGroup: Map<String, List<Channel>> = emptyMap()
    private var allChannels: List<Channel> = emptyList()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private var cardPresenterSelector: ClassPresenterSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure browse fragment
        title = getString(R.string.app_name)
        headersState = HEADERS_DISABLED

        // Brand color (used for the orb icon and row separators)
        brandColor = resources.getColor(android.R.color.holo_blue_light, null)

        // Search support
        isHeadersTransitionOnBackEnabled = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupEventListeners()
    }

    private fun setupUI() {
        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        onItemViewClickedListener = OnItemViewClickedListener { itemViewHolder, item, _, _ ->
            if (item is Channel) {
                (activity as? MainActivity)?.onChannelSelected(item)
            }
        }

        onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, _ ->
            // Could show channel preview/description here
        }
    }

    /**
     * Updates the channel list display with grouped channels.
     */
    fun updateChannels(groupedChannels: Map<String, List<Channel>>) {
        channelsByGroup = groupedChannels
        allChannels = groupedChannels.values.flatten()

        rowsAdapter.clear()

        if (allChannels.isEmpty()) {
            return
        }

        // Total channel count as subtitle
        subtitle = resources.getString(R.string.channel_count, allChannels.size)

        // Create a presenter for channel cards
        val cardPresenter = CardPresenter()

        // Add a row for each channel group
        for ((groupName, channels) in groupedChannels) {
            if (channels.isEmpty()) continue

            val rowAdapter = ArrayObjectAdapter(cardPresenter)
            channels.forEach { channel ->
                rowAdapter.add(channel)
            }

            val header = HeaderItem(groupName)
            val listRow = ListRow(header, rowAdapter)
            rowsAdapter.add(listRow)
        }

        Log.d(TAG, "Displaying ${groupedChannels.size} groups with ${allChannels.size} channels")
    }

    /**
     * Shows or hides a loading indicator.
     */
    fun showLoading(show: Boolean) {
        // The browse fragment shows a built-in spinner while loading
        // We could add a custom ProgressBar if needed
    }

    /**
     * CardPresenter renders each channel as a card in the browse rows.
     * Shows channel logo/name on a card background.
     */
    inner class CardPresenter : Presenter() {

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(androidx.leanback.R.layout.lb_image_card_view, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val channel = item as Channel
            val cardView = viewHolder.view as ImageCardView

            cardView.titleText = channel.getDisplayName()
            cardView.contentText = if (channel.group.isNotBlank()) {
                channel.group
            } else {
                "IPTV 直播源"
            }
            cardView.mainImageScaleType = ImageCardView.MAIN_IMAGE_SCALE_TYPE_CENTER_INSIDE

            // Set card dimensions for TV
            cardView.findViewById<View>(androidx.leanback.R.id.info_field)?.apply {
                visibility = View.VISIBLE
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val cardView = viewHolder.view as ImageCardView
            cardView.mainImage = null
            cardView.titleText = ""
            cardView.contentText = ""
        }
    }
}
