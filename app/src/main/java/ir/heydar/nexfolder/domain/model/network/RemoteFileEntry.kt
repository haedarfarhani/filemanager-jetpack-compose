package ir.heydar.nexfolder.domain.model.network

/** One entry of a remote directory listing, protocol-agnostic. */
data class RemoteFileEntry(
    val name: String,
    /** Protocol-specific absolute path (for SMB includes the share name). */
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)
