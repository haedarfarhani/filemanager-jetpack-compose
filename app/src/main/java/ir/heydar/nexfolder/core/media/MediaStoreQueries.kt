package ir.heydar.nexfolder.core.media

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import ir.heydar.nexfolder.domain.model.DirectoryType
import ir.heydar.nexfolder.domain.model.FileFormat
import ir.heydar.nexfolder.domain.model.FileItem
import ir.heydar.nexfolder.domain.model.FileType

/** Reads "recently added" media/documents straight from MediaStore — no persistence needed. */
object MediaStoreQueries {

    private val PROJECTION = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DATA,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED
    )

    /** Newest files first, across all media + document collections. */
    fun queryRecentlyAdded(context: Context, limit: Int = 100): List<FileItem> {
        val resolver: ContentResolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val items = mutableListOf<FileItem>()
        try {
            resolver.query(
                collection,
                PROJECTION,
                null,
                null,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC LIMIT $limit"
            )?.use { cursor ->
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val addedIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: path.substringAfterLast('/')
                    val ext = name.substringAfterLast('.', "").lowercase()
                    items += FileItem(
                        name = name,
                        path = path,
                        size = cursor.getLong(sizeIdx),
                        lastModified = cursor.getLong(addedIdx) * 1000L,
                        isDirectory = false,
                        fileType = FileType.fromExtension(ext),
                        fileFormat = FileFormat.fromExtension(ext),
                        directoryType = DirectoryType.UNKNOWN,
                        extension = ext
                    )
                }
            }
        } catch (e: Exception) {
            // Return whatever was collected before the failure
        }
        return items
    }
}
