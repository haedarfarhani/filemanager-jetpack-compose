package ir.heydar.nexfolder.domain.model

/** Pending paste operation for the file list clipboard. */
enum class ClipboardMode {
    COPY, MOVE;

    val actionLabel: String get() = if (this == COPY) "Copy" else "Move"
}
