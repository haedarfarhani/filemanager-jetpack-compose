package ir.heydar.nexfolder.domain.model

/** Aggregated result of a storage scan for the analyzer screen. */
data class StorageAnalysis(
    val rootPath: String,
    val volumeCapacity: Long = 0,
    val totalFiles: Long = 0,
    val totalBytes: Long = 0,
    val categories: List<CategoryUsage> = emptyList(),
    val largestFiles: List<FileItem> = emptyList(),
    val folders: List<FolderUsage> = emptyList()
)

data class CategoryUsage(
    val type: FileType,
    val bytes: Long,
    val files: Long
)

data class FolderUsage(
    val path: String,
    val name: String,
    val bytes: Long,
    val files: Long
)
