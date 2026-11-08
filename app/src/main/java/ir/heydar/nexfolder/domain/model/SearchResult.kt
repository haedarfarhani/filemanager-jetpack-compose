package ir.heydar.nexfolder.domain.model

/** One hit of a storage search: the file plus optional in-content match info. */
data class SearchResult(
    val file: FileItem,
    /** First matching line content when the hit came from a content search. */
    val contentMatch: String? = null,
    /** 1-based line number of [contentMatch] inside the file. */
    val matchedLine: Int? = null
)
