package com.example

import android.net.Uri

/**
 * One user-selected share request sent to CinemaX through Android's Sharesheet.
 * URI grants are temporary and must be consumed while the receiving Activity is alive.
 */
data class IncomingShareRequest(
    val id: Long,
    val text: String? = null,
    val uris: List<Uri> = emptyList(),
    val mimeType: String? = null
)
