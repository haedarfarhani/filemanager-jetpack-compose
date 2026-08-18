package com.heydar.filemanager.domain.model

data class StorageVolumeInfo(
    val id: String,
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val isPrimary: Boolean,
    val isRemovable: Boolean
) {
    val usageProgress: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
