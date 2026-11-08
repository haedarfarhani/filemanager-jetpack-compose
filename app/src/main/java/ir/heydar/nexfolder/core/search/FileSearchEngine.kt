package ir.heydar.nexfolder.core.search

import ir.heydar.nexfolder.domain.model.DirectoryType
import ir.heydar.nexfolder.domain.model.FileFormat
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType
import ir.heydar.nexfolder.domain.model.SearchFilter
import ir.heydar.nexfolder.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cooperative, cancellable storage search: iterative DFS over the filesystem with
 * name/extension/category/size/date filters and optional grep inside text files.
 */
@Singleton
class FileSearchEngine @Inject constructor() {

    suspend fun search(
        rootPath: String,
        filter: SearchFilter,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        onProgress: suspend (filesScanned: Int) -> Unit = {}
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.isDirectory) return@withContext emptyList()

        val query = filter.query.trim()
        val contentQuery = filter.contentQuery?.trim()?.lowercase()?.takeIf { it.length >= 2 }
        val results = mutableListOf<SearchResult>()
        var scanned = 0
        val stack = ArrayDeque<File>()
        stack.addLast(root)

        try {
            while (stack.isNotEmpty() && results.size < maxResults) {
                ensureActive()
                val dir = stack.removeFirst()
                val children = dir.listFiles() ?: continue
                for (child in children) {
                    ensureActive()
                    scanned++
                    if (scanned % PROGRESS_EVERY == 0) onProgress(scanned)
                    if (!filter.includeHidden && child.name.startsWith(".")) continue

                    if (child.isDirectory) {
                        if (matchesName(child.name, query)) {
                            results += SearchResult(file = child.toFileItem())
                        }
                        stack.addLast(child)
                    } else {
                        matchFile(child, filter, query, contentQuery)?.let { result ->
                            results += result
                        }
                    }
                    if (results.size >= maxResults) break
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SecurityException) {
            // Unreadable branches are skipped; results collected so far are returned.
        }
        results
    }

    private fun matchesName(name: String, query: String): Boolean =
        query.isEmpty() || name.contains(query, ignoreCase = true)

    private fun matchFile(
        file: File,
        filter: SearchFilter,
        query: String,
        contentQuery: String?
    ): SearchResult? {
        val ext = file.extension.lowercase()
        val fileType = FileType.fromExtension(ext)

        // Extension filter
        if (filter.extensions.isNotEmpty() && ext !in filter.extensions) return null

        // Category filter
        if (filter.categories.isNotEmpty() && fileType !in filter.categories) return null

        // Size / date filters
        val length = file.length()
        if (filter.minSize != null && length < filter.minSize) return null
        if (filter.maxSize != null && length > filter.maxSize) return null
        val modified = file.lastModified()
        if (filter.dateFrom != null && modified < filter.dateFrom) return null
        if (filter.dateTo != null && modified > filter.dateTo) return null

        // Name filter (name or extension)
        val nameMatches = matchesName(file.name, query)
        if (contentQuery == null) {
            if (!nameMatches) return null
            return SearchResult(file = file.toFileItem())
        }

        // Content search: text files within size limit; name match still allows a hit
        if (fileType == FileType.TEXT && length in 1..SearchFilter.CONTENT_MAX_BYTES) {
            val match = findInContent(file, contentQuery)
            if (match != null) {
                return SearchResult(
                    file = file.toFileItem(),
                    contentMatch = match.first,
                    matchedLine = match.second
                )
            }
            return null
        }
        return if (nameMatches) SearchResult(file = file.toFileItem()) else null
    }

    /** Returns (matched line text, 1-based line number) or null. */
    private fun findInContent(file: File, queryLower: String): Pair<String, Int>? {
        return try {
            val lines = file.readLines(Charsets.UTF_8)
            val index = lines.indexOfFirst { it.lowercase().contains(queryLower) }
            if (index >= 0) {
                lines[index].trim().take(160) to (index + 1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun File.toFileItem(): FileItem {
        val ext = extension.lowercase()
        val fileType = if (isDirectory) FileType.OTHER else FileType.fromExtension(ext)
        return FileItem(
            name = name,
            path = absolutePath,
            size = if (isDirectory) 0L else length(),
            lastModified = lastModified(),
            isDirectory = isDirectory,
            fileType = fileType,
            fileFormat = if (isDirectory) FileFormat.UNKNOWN else FileFormat.fromExtension(ext),
            directoryType = if (isDirectory) DirectoryType.fromName(name) else DirectoryType.UNKNOWN,
            extension = ext
        )
    }

    companion object {
        const val DEFAULT_MAX_RESULTS = 2000
        private const val PROGRESS_EVERY = 500
    }
}
