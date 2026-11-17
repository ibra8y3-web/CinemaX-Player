package com.example.player

enum class NetworkHealthStatus {
    EXCELLENT,
    GOOD,
    POOR,
    DISCONNECTED
}

data class NetworkStreamQualityInfo(
    val status: NetworkHealthStatus = NetworkHealthStatus.EXCELLENT,
    val bitrateEstimateKbps: Long = 0L,
    val currentResolution: String = "",
    val isAutoQualityDowngraded: Boolean = false,
    val alertMessage: String? = null
)
