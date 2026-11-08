package ir.heydar.nexfolder.core.files

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ir.heydar.nexfolder.core.util.MimeTypeHelper
import java.io.File

/** Opens a file with an external viewer app via FileProvider. */
object FileOpener {

    fun openExternal(context: Context, path: String): Boolean {
        return try {
            val file = File(path)
            if (!file.isFile) return false
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, MimeTypeHelper.getMimeType(file.name))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(
                Intent.createChooser(intent, "Open with").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
