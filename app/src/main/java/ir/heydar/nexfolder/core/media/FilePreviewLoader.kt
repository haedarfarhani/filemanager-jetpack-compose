package ir.heydar.nexfolder.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders rich previews for special file types:
 * - PDF  : first page rendered with the built-in [PdfRenderer]
 * - APK  : the app icon embedded inside the (possibly uninstalled) archive
 *
 * Results are memory-cached keyed by path + lastModified, so list scrolling and
 * re-entries do not re-render. Call [init] once from the Application class.
 */
object FilePreviewLoader {

    private lateinit var appContext: Context

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    /** First page of a PDF as a bitmap, or null when rendering is impossible. */
    suspend fun pdfThumbnail(path: String, renderWidth: Int = 512): Bitmap? =
        load("pdf:$path:${stamp(path)}") {
            renderPdfPage(path, renderWidth)
        }

    /** Icon embedded inside an APK archive (works for uninstalled packages). */
    suspend fun apkIcon(path: String, size: Int = 128): Bitmap? =
        load("apk:$path:${stamp(path)}") {
            loadApkIcon(path, size)
        }

    /** Pixel dimensions of an image file (bounds-only decode, cheap). */
    suspend fun imageDimensions(path: String): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                options.outWidth to options.outHeight
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend inline fun load(key: String, crossinline render: suspend () -> Bitmap?): Bitmap? {
        cache.get(key)?.let { return it }
        val value = render() ?: return null
        cache.put(key, value)
        return value
    }

    private fun stamp(path: String): Long = try {
        File(path).lastModified()
    } catch (e: Exception) {
        0L
    }

    private fun cacheSizeKb(): Int {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        return (maxKb / 16).coerceAtLeast(8 * 1024)
    }

    private suspend fun renderPdfPage(path: String, renderWidth: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.isFile || file.length() == 0L) return@withContext null
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        if (renderer.pageCount == 0) return@use null
                        renderer.openPage(0).use { page ->
                            val ratio = if (page.width > 0) {
                                page.height.toFloat() / page.width.toFloat()
                            } else 1.4f
                            val width = renderWidth.coerceIn(64, 1024)
                            val height = (width * ratio).toInt().coerceAtLeast(64)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            } catch (e: Exception) {
                null // corrupted or password-protected PDFs fall back to the static icon
            }
        }

    private suspend fun loadApkIcon(path: String, size: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val apkFile = File(path)
            if (!apkFile.isFile || apkFile.length() == 0L) return@withContext null
            if (!::appContext.isInitialized) return@withContext null
            try {
                val pm = appContext.packageManager
                val packageInfo = pm.getPackageArchiveInfo(path, 0)
                val appInfo = packageInfo?.applicationInfo ?: return@withContext null
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                val drawable = pm.getApplicationIcon(appInfo)
                drawableToBitmap(drawable, size)
            } catch (e: Exception) {
                null
            }
        }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
