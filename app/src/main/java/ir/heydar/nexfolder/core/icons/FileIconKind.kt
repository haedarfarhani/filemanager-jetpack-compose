package ir.heydar.nexfolder.core.icons

import androidx.annotation.DrawableRes

/**
 * Every icon family that has a dedicated custom drawable.
 *
 * [color] is the accent colour baked into the drawable and reused by the UI for the
 * icon's background tint, so a file always keeps the same visual identity.
 */
enum class FileIconKind(
    @DrawableRes val icon: Int,
    val color: Long
) {
    // Documents
    PDF(AppIcons.File.Format.pdf, 0xFFE53935),
    WORD(AppIcons.File.Format.word, 0xFF1E88E5),
    SPREADSHEET(AppIcons.File.Format.spreadsheet, 0xFF43A047),
    PRESENTATION(AppIcons.File.Format.presentation, 0xFFFB8C00),
    TEXT(AppIcons.File.Format.text, 0xFF546E7A),
    EBOOK(AppIcons.File.Format.ebook, 0xFF00897B),
    FONT(AppIcons.File.Format.font, 0xFF3949AB),

    // Data & code
    DATABASE(AppIcons.File.Format.database, 0xFF1565C0),
    CONFIG(AppIcons.File.Format.config, 0xFF455A64),
    JSON(AppIcons.File.Format.json, 0xFFF9A825),
    CODE(AppIcons.File.Format.code, 0xFF6D4C41),
    SCRIPT(AppIcons.File.Format.script, 0xFF37474F),
    MARKDOWN(AppIcons.File.Format.markdown, 0xFF546E7A),
    WEB(AppIcons.File.Format.web, 0xFF00838F),

    // Communication
    EMAIL(AppIcons.File.Format.email, 0xFF0277BD),
    CALENDAR(AppIcons.File.Format.calendar, 0xFFD81B60),
    CONTACT(AppIcons.File.Format.contact, 0xFF00796B),

    // Media & models
    PLAYLIST(AppIcons.File.Format.playlist, 0xFFC2185B),
    TORRENT(AppIcons.File.Format.torrent, 0xFF2E7D32),
    MAP(AppIcons.File.Format.map, 0xFF7CB342),
    MODEL_3D(AppIcons.File.Format.model3d, 0xFFFF7043),
    CAD(AppIcons.File.Format.cad, 0xFF0288D1),
    DESIGN(AppIcons.File.Format.design, 0xFF8E24AA),

    // Binary & system
    DISK(AppIcons.File.Format.disk, 0xFF546E7A),
    EXECUTABLE(AppIcons.File.Format.executable, 0xFF37474F),
    LIBRARY(AppIcons.File.Format.library, 0xFF5E35B1),
    BINARY(AppIcons.File.Format.binary, 0xFF795548),
    PACKAGE(AppIcons.File.Format.packageFile, 0xFF8D6E63),
    LINK(AppIcons.File.Format.link, 0xFF039BE5),
    BACKUP(AppIcons.File.Format.backup, 0xFF5C6BC0),

    // Audio
    AUDIO(AppIcons.File.audio, 0xFF9C27B0),
    AUDIO_MP3(AppIcons.File.Format.Audio.mp3, 0xFFF4511E),
    AUDIO_WAV(AppIcons.File.Format.Audio.wav, 0xFFFB8C00),
    AUDIO_FLAC(AppIcons.File.Format.Audio.flac, 0xFFFFB300),
    AUDIO_AAC(AppIcons.File.Format.Audio.aac, 0xFF8E24AA),
    AUDIO_OGG(AppIcons.File.Format.Audio.ogg, 0xFF6A1B9A),
    AUDIO_M4A(AppIcons.File.Format.Audio.m4a, 0xFFD81B60),
    AUDIO_WMA(AppIcons.File.Format.Audio.wma, 0xFF5E35B1),
    AUDIO_MID(AppIcons.File.Format.Audio.mid, 0xFF00897B),

    // Archives & packages
    ARCHIVE(AppIcons.File.archive, 0xFFF4511E),
    ARCHIVE_ZIP(AppIcons.File.Format.Archive.zip, 0xFFEF6C00),
    ARCHIVE_RAR(AppIcons.File.Format.Archive.rar, 0xFF5D4037),
    ARCHIVE_7Z(AppIcons.File.Format.Archive.sevenZip, 0xFF43A047),
    ARCHIVE_TAR(AppIcons.File.Format.Archive.tar, 0xFF795548),
    ARCHIVE_GZ(AppIcons.File.Format.Archive.gz, 0xFFAB47BC),

    // Generic media fallbacks for formats without their own artwork
    IMAGE(AppIcons.File.image, 0xFF4CAF50),
    VIDEO(AppIcons.File.video, 0xFFE91E63),
    APK(AppIcons.File.apk, 0xFF00BCD4)
}
