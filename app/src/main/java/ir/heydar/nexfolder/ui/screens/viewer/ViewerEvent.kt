package ir.heydar.nexfolder.ui.screens.viewer

sealed class ViewerEvent {
    data object Next : ViewerEvent()
    data object Previous : ViewerEvent()
    /** Pager swiped to [index]; keeps the position across navigation round-trips. */
    data class PageChanged(val index: Int) : ViewerEvent()
}
