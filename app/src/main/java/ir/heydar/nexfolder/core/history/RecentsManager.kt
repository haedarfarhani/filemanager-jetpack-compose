package ir.heydar.nexfolder.core.history

import ir.heydar.nexfolder.data.local.RecentFileDao
import ir.heydar.nexfolder.data.local.RecentFileEntity
import ir.heydar.nexfolder.domain.model.FileType
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks which files the user opens inside the app, powering the
 * "Recently opened" list. MediaStore powers "Recently added"; this powers opens.
 */
@Singleton
class RecentsManager @Inject constructor(
    private val dao: RecentFileDao
) {

    suspend fun record(path: String) {
        try {
            val file = File(path)
            if (!file.isFile) return
            val ext = file.extension.lowercase()
            dao.upsert(
                RecentFileEntity(
                    path = file.absolutePath,
                    name = file.name,
                    extension = ext,
                    fileType = FileType.fromExtension(ext).name,
                    openedAt = System.currentTimeMillis()
                )
            )
            dao.prune(RecentFileDao.MAX_ENTRIES)
        } catch (e: Exception) {
            // History tracking must never break opening a file
        }
    }

    fun observeRecentOpened(limit: Int = 100): Flow<List<RecentFileEntity>> =
        dao.observeRecent(limit)

    suspend fun clear() {
        dao.clear()
    }
}
