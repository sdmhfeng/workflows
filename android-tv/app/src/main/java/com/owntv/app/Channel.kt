package com.owntv.app

/**
 * Represents an IPTV channel parsed from an M3U playlist.
 */
data class Channel(
    val id: Long = 0,
    val name: String,
    val url: String,
    val group: String = "",
    val logo: String = "",
    val tvgId: String = "",
    val tvgName: String = ""
) {
    companion object {
        const val DEFAULT_GROUP = "未分类"
    }

    /**
     * Returns a display-friendly group name.
     */
    fun getDisplayGroup(): String {
        return group.ifBlank { DEFAULT_GROUP }
    }

    /**
     * Returns channel name without any extra info tags (like $ resolution info).
     */
    fun getDisplayName(): String {
        // Strip everything after $ if present (IPTV-API adds info after $)
        val dollarIndex = name.indexOf('$')
        return if (dollarIndex > 0) {
            name.substring(0, dollarIndex).trim()
        } else {
            name.trim()
        }
    }
}
