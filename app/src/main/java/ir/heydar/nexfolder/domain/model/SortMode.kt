package ir.heydar.nexfolder.domain.model

/** Sort order for file lists. Folders always come first except when sorting by name descending. */
enum class SortMode(val label: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    SIZE_ASC("Smallest first"),
    SIZE_DESC("Largest first"),
    DATE_ASC("Oldest first"),
    DATE_DESC("Newest first"),
    TYPE_ASC("By type");

    companion object {
        fun fromNameOrDefault(name: String?): SortMode =
            entries.firstOrNull { it.name == name } ?: NAME_ASC
    }
}
