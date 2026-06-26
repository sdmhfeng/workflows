package com.owntv.app

import android.util.Log

/**
 * Parser for M3U/M3U8 playlist files.
 * Handles standard #EXTINF tags and IPTV-API specific formats.
 */
object M3uParser {

    private const val TAG = "M3uParser"
    private const val EXTINF_PREFIX = "#EXTINF:"
    private const val EXTINF_TVG_ID = "tvg-id=\""
    private const val EXTINF_TVG_NAME = "tvg-name=\""
    private const val EXTINF_TVG_LOGO = "tvg-logo=\""
    private const val EXTINF_GROUP_TITLE = "group-title=\""

    /**
     * Parses an M3U playlist content string into a list of Channel objects.
     */
    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentExtInf: String? = null
        var idCounter = 0L

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            if (trimmed.startsWith(EXTINF_PREFIX)) {
                currentExtInf = trimmed
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
                trimmed.startsWith("rtmp://") || trimmed.startsWith("rtsp://") ||
                trimmed.startsWith("udp://") || trimmed.startsWith("rtp://") ||
                trimmed.startsWith("mms://") || trimmed.startsWith("rtmp://")
            ) {
                val extInf = currentExtInf
                if (extInf != null) {
                    val channel = parseExtInfLine(extInf, trimmed, idCounter++)
                    channels.add(channel)
                }
                currentExtInf = null
            }
            // Ignore other lines like #EXTGRP, #PLAYLIST, #KODIPROP, etc.
        }

        Log.d(TAG, "Parsed ${channels.size} channels from M3U")
        return channels
    }

    /**
     * Parses a single #EXTINF line with its following URL.
     */
    private fun parseExtInfLine(extInfLine: String, url: String, id: Long): Channel {
        // Extract channel name (everything after the last comma)
        val lastCommaIndex = extInfLine.lastIndexOf(',')
        val rawName = if (lastCommaIndex >= 0) {
            extInfLine.substring(lastCommaIndex + 1).trim()
        } else {
            "Unknown Channel"
        }

        // Parse attributes
        val attributes = extInfLine.substring(0, lastCommaIndex.coerceAtLeast(0))

        val tvgId = extractAttribute(attributes, EXTINF_TVG_ID)
        val tvgName = extractAttribute(attributes, EXTINF_TVG_NAME).ifBlank { rawName }
        val logo = extractAttribute(attributes, EXTINF_TVG_LOGO)
        val group = extractAttribute(attributes, EXTINF_GROUP_TITLE)

        return Channel(
            id = id,
            name = tvgName.ifBlank { rawName },
            url = url,
            group = group,
            logo = logo,
            tvgId = tvgId,
            tvgName = tvgName.ifBlank { rawName }
        )
    }

    /**
     * Extracts an attribute value from an EXTINF attributes string.
     * Handles both quoted ("value") and unquoted (value) attribute formats.
     */
    private fun extractAttribute(attributes: String, attrName: String): String {
        val startIndex = attributes.indexOf(attrName)
        if (startIndex < 0) return ""

        val valueStart = startIndex + attrName.length
        val inQuote = valueStart < attributes.length && attributes[valueStart] == '"'

        return if (inQuote) {
            val endIndex = attributes.indexOf('"', valueStart + 1)
            if (endIndex >= 0) {
                attributes.substring(valueStart + 1, endIndex)
            } else {
                ""
            }
        } else {
            val endIndex = attributes.indexOf(' ', valueStart)
            if (endIndex >= 0) {
                attributes.substring(valueStart, endIndex)
            } else {
                attributes.substring(valueStart)
            }
        }
    }

    /**
     * Groups parsed channels by their group-title.
     * Returns a map of group name -> list of channels.
     */
    fun groupByTitle(channels: List<Channel>): Map<String, List<Channel>> {
        return channels.groupBy { it.getDisplayGroup() }
    }
}
