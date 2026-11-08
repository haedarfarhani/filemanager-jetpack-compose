package ir.heydar.nexfolder.domain.model

/**
 * Filters for a storage-wide search. Every field is optional; an empty [query]
 * with no filters matches everything (browse mode).
 */
data class SearchFilter(
    /** Matches against file and folder names (and extensions). */
    val query: String = "",
    /** Restrict to specific file extensions (without dots, lowercase). */
    val extensions: Set<String> = emptySet(),
    /** Restrict to file-type categories (IMAGE, VIDEO, AUDIO, DOCUMENT, APK, ARCHIVE, TEXT). */
    val categories: Set<FileType> = emptySet(),
    val minSize: Long? = null,
    val maxSize: Long? = null,
    /** Last-modified range in epoch millis. */
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    /** Text to find inside text files (min 2 chars, files up to [CONTENT_MAX_BYTES]). */
    val contentQuery: String? = null,
    val includeHidden: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || extensions.isNotEmpty() || categories.isNotEmpty() ||
            minSize != null || maxSize != null || dateFrom != null || dateTo != null ||
            !contentQuery.isNullOrBlank()

    companion object {
        /** Files larger than this are skipped during content search. */
        const val CONTENT_MAX_BYTES: Long = 2 * 1024 * 1024
    }
}
