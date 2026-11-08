package ir.heydar.nexfolder.ui.screens.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.ui.screens.network.components.NetworkDeviceItem
import ir.heydar.nexfolder.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkBrowserScreen(
    onNavigateBack: () -> Unit,
    onDeviceClick: (String) -> Unit,
    viewModel: NetworkBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Network") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = AppIcons.Nav.back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(NetworkBrowserEvent.Refresh) }) {
                        Icon(
                            painter = painterResource(id = AppIcons.Action.sort),
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(NetworkBrowserEvent.ShowManualConnect) }
            ) {
                Icon(
                    painter = painterResource(id = AppIcons.Action.more),
                    contentDescription = "Manual Connect"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isScanning) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Scanning network...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.discoveredDevices.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Icon(
                        painter = painterResource(id = AppIcons.Status.noResults),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No devices found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Make sure other devices are sharing files on the same network",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    OutlinedButton(onClick = { viewModel.onEvent(NetworkBrowserEvent.StartDiscovery) }) {
                        Text("Start Scanning")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(state.discoveredDevices, key = { it.id }) { device ->
                        NetworkDeviceItem(
                            device = device,
                            onClick = { onDeviceClick(device.url) }
                        )
                    }
                }
            }
        }

        // Manual Connect Dialog
        if (state.showManualConnect) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(NetworkBrowserEvent.DismissManualConnect) },
                title = { Text("Manual Connect") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        OutlinedTextField(
                            value = state.manualIp,
                            onValueChange = { viewModel.onEvent(NetworkBrowserEvent.UpdateManualIp(it)) },
                            label = { Text("IP Address") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("192.168.1.100") }
                        )
                        OutlinedTextField(
                            value = state.manualPort,
                            onValueChange = { viewModel.onEvent(NetworkBrowserEvent.UpdateManualPort(it)) },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("8080") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val port = state.manualPort.toIntOrNull() ?: 8080
                            if (state.manualIp.isNotEmpty()) {
                                val url = "http://${state.manualIp}:$port"
                                onDeviceClick(url)
                                viewModel.onEvent(NetworkBrowserEvent.DismissManualConnect)
                            }
                        }
                    ) {
                        Text("Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(NetworkBrowserEvent.DismissManualConnect) }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
