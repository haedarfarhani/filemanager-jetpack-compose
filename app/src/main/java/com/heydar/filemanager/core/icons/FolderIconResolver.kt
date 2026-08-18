package com.heydar.filemanager.core.icons

import androidx.annotation.DrawableRes
import com.heydar.filemanager.domain.model.DirectoryType

/**
 * Centralized resolver that maps DirectoryType to drawable resource IDs.
 * Single source of truth for folder icon resolution.
 */
object FolderIconResolver {

    @DrawableRes
    fun resolve(directoryType: DirectoryType): Int {
        return when (directoryType) {
            DirectoryType.DOWNLOAD -> AppIcons.Folder.download
            DirectoryType.DOCUMENTS -> AppIcons.Folder.documents
            DirectoryType.PICTURES, DirectoryType.SCREENSHOTS -> AppIcons.Folder.pictures
            DirectoryType.DCIM, DirectoryType.CAMERA -> AppIcons.Folder.dcim
            DirectoryType.MUSIC, DirectoryType.RINGTONES,
            DirectoryType.NOTIFICATIONS, DirectoryType.ALARMS -> AppIcons.Folder.music
            DirectoryType.MOVIES -> AppIcons.Folder.movies
            DirectoryType.ANDROID -> AppIcons.Folder.android
            DirectoryType.WHATSAPP, DirectoryType.TELEGRAM -> AppIcons.Folder.default
            DirectoryType.UNKNOWN -> AppIcons.Folder.default
        }
    }
}
