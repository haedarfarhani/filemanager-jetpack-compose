package com.heydar.filemanager.domain.model

enum class DirectoryType(val dirName: String) {
    DOWNLOAD("Download"),
    DOCUMENTS("Documents"),
    PICTURES("Pictures"),
    DCIM("DCIM"),
    CAMERA("Camera"),
    MOVIES("Movies"),
    MUSIC("Music"),
    ANDROID("Android"),
    WHATSAPP("WhatsApp"),
    TELEGRAM("Telegram"),
    SCREENSHOTS("Screenshots"),
    RINGTONES("Ringtones"),
    NOTIFICATIONS("Notifications"),
    ALARMS("Alarms"),
    UNKNOWN("");

    companion object {
        fun fromName(name: String): DirectoryType {
            return entries.find { it.dirName.equals(name, ignoreCase = true) }
                ?: UNKNOWN
        }
    }
}
