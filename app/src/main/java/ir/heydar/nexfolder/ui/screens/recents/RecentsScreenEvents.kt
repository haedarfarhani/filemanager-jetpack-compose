package ir.heydar.nexfolder.ui.screens.recents

sealed class RecentsEvent {
    data class TabChanged(val tab: RecentsTab) : RecentsEvent()
    data object Refresh : RecentsEvent()
    data class ItemClicked(val fileItem: ir.heydar.nexfolder.domain.model.FileItem) : RecentsEvent()
    data class ItemLongClicked(val fileItem: ir.heydar.nexfolder.domain.model.FileItem) : RecentsEvent()
    data object EditorHandled : RecentsEvent()
    data object DetailsHandled : RecentsEvent()
    data object ViewerHandled : RecentsEvent()
    data object PlayerHandled : RecentsEvent()
}
