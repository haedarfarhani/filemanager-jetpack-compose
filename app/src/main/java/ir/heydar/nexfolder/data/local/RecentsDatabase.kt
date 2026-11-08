package ir.heydar.nexfolder.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One row per file-open event; keyed by path so re-opens bump [openedAt]. */
@Entity(
    tableName = "recent_files",
    indices = [Index(value = ["path"], unique = true)]
)
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val name: String,
    val extension: String,
    val fileType: String,
    val openedAt: Long
)

@Dao
interface RecentFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentFileEntity)

    /** Keeps only the newest [keep] distinct paths. */
    @Query(
        "DELETE FROM recent_files WHERE path NOT IN " +
            "(SELECT path FROM recent_files ORDER BY openedAt DESC LIMIT :keep)"
    )
    suspend fun prune(keep: Int)

    @Query("SELECT * FROM recent_files ORDER BY openedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentFileEntity>>

    @Query("DELETE FROM recent_files")
    suspend fun clear()

    companion object {
        const val MAX_ENTRIES = 200
    }
}

@Database(
    entities = [RecentFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RecentsDatabase : RoomDatabase() {
    abstract fun recentFileDao(): RecentFileDao

    companion object {
        fun build(context: Context): RecentsDatabase =
            Room.databaseBuilder(context, RecentsDatabase::class.java, "recents.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
