package ir.heydar.nexfolder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ir.heydar.nexfolder.core.media.FilePreviewLoader

@HiltAndroidApp
class NexFolderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FilePreviewLoader.init(this)
    }
}
