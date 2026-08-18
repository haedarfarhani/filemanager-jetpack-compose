package com.heydar.filemanager.domain.model.network

data class NetworkClientInfo(
    val ip: String,
    val userAgent: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val requestedPath: String = ""
)
