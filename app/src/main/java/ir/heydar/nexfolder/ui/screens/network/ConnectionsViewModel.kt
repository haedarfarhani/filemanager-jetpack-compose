package ir.heydar.nexfolder.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.heydar.nexfolder.core.network.clients.ConnectionStore
import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class ConnectionsEvent {
    data object AddClicked : ConnectionsEvent()
    data object AddDismissed : ConnectionsEvent()
    data class SaveClicked(val connection: RemoteConnection) : ConnectionsEvent()
    data class DeleteClicked(val connection: RemoteConnection) : ConnectionsEvent()
    data class ConnectClicked(val connection: RemoteConnection) : ConnectionsEvent()
    data object BrowseHandled : ConnectionsEvent()
}

data class ConnectionsUiState(
    val connections: List<RemoteConnection> = emptyList(),
    val showAddDialog: Boolean = false,
    val openBrowseFor: String? = null
)

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val store: ConnectionStore
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionsUiState())
    val state: StateFlow<ConnectionsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: ConnectionsEvent) {
        when (event) {
            is ConnectionsEvent.AddClicked -> _state.update { it.copy(showAddDialog = true) }
            is ConnectionsEvent.AddDismissed -> _state.update { it.copy(showAddDialog = false) }
            is ConnectionsEvent.SaveClicked -> {
                store.add(event.connection)
                _state.update { it.copy(showAddDialog = false) }
                refresh()
            }
            is ConnectionsEvent.DeleteClicked -> {
                store.remove(event.connection.id)
                refresh()
            }
            is ConnectionsEvent.ConnectClicked -> {
                _state.update { it.copy(openBrowseFor = event.connection.id) }
            }
            is ConnectionsEvent.BrowseHandled -> {
                _state.update { it.copy(openBrowseFor = null) }
            }
        }
    }

    private fun refresh() {
        _state.update { it.copy(connections = store.load()) }
    }
}
