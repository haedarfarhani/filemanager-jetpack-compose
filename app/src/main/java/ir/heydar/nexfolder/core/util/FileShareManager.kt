package ir.heydar.nexfolder.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Central place for all file-sharing logic.
 * Uses FileProvider (content:// URI) — never raw file:// URIs.
 */
object FileShareManager {

    /**
     * Share a single file via the Android Share Sheet.
     * Must be called from a context that has FLAG_ACTIVITY_NEW_TASK available
     * (Application context is fine).
     */
    fun shareFile(context: Context, filePath: String): ShareResult {
        return try {
            val file = File(filePath)

            if (!file.exists()) return ShareResult.FileNotFound
            if (!file.canRead()) return ShareResult.PermissionDenied

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val mimeType = MimeTypeHelper.getMimeType(file.name)
                .takeIf { it != "*/*" } ?: "application/octet-stream"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(sendIntent, "Share ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            ShareResult.Success
        } catch (e: SecurityException) {
            ShareResult.PermissionDenied
        } catch (e: IllegalArgumentException) {
            ShareResult.ProviderError(e.message ?: "FileProvider error")
        } catch (e: Exception) {
            ShareResult.UnknownError(e.message ?: "Unknown error")
        }
    }
}

sealed interface ShareResult {
    object Success : ShareResult
    object FileNotFound : ShareResult
    object PermissionDenied : ShareResult
    data class ProviderError(val message: String) : ShareResult
    data class UnknownError(val message: String) : ShareResult

    /** Human-readable error message; null on success. */
    fun errorMessage(): String? = when (this) {
        is Success         -> null
        is FileNotFound    -> "File not found"
        is PermissionDenied -> "Permission denied"
        is ProviderError   -> "Cannot share this file"
        is UnknownError    -> "Cannot share this file"
    }
}
