package ir.heydar.nexfolder.ui.screens.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.heydar.nexfolder.core.icons.AppIcons
import ir.heydar.nexfolder.domain.model.network.NetworkProtocol
import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import ir.heydar.nexfolder.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    onNavigateBack: () -> Unit,
    onConnect: (String) -> Unit = {},
    viewModel: ConnectionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.openBrowseFor) {
        state.openBrowseFor?.let { id ->
            onConnect(id)
            viewModel.onEvent(ConnectionsEvent.BrowseHandled)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote connections") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(id = AppIcons.Nav.back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(ConnectionsEvent.AddClicked) }) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add connection")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.connections.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Computer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.padding(top = Spacing.md))
                    Text(
                        text = "No saved connections.\nUse + to add an FTP, SFTP, SMB or WebDAV server.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.lg)
                ) {
                    items(state.connections, key = { it.id }) { connection ->
                        ConnectionRow(
                            connection = connection,
                            onClick = { viewModel.onEvent(ConnectionsEvent.ConnectClicked(connection)) },
                            onDelete = { viewModel.onEvent(ConnectionsEvent.DeleteClicked(connection)) }
                        )
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddConnectionDialog(
            onConfirm = { viewModel.onEvent(ConnectionsEvent.SaveClicked(it)) },
            onDismiss = { viewModel.onEvent(ConnectionsEvent.AddDismissed) }
        )
    }
}

@Composable
private fun ConnectionRow(
    connection: RemoteConnection,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Computer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connection.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${connection.protocol.displayName} · ${connection.host}:${connection.port}" +
                    if (connection.username.isNotBlank()) " · ${connection.username}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete connection",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AddConnectionDialog(
    onConfirm: (RemoteConnection) -> Unit,
    onDismiss: () -> Unit
) {
    var protocol by remember { mutableStateOf(NetworkProtocol.FTP) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(NetworkProtocol.FTP.defaultPort.toString()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var domain by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New connection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    NetworkProtocol.entries
                        .filter { it != NetworkProtocol.HTTP }
                        .forEach { candidate ->
                            FilterChip(
                                selected = protocol == candidate,
                                onClick = {
                                    protocol = candidate
                                    port = candidate.defaultPort.toString()
                                },
                                label = { Text(candidate.displayName.substringBefore('/')) }
                            )
                        }
                }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                TextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host / IP") },
                    singleLine = true
                )
                TextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Port") },
                    singleLine = true
                )
                if (protocol == NetworkProtocol.SMB) {
                    TextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Domain (optional)") },
                        singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = anonymous,
                        onCheckedChange = { anonymous = it }
                    )
                    Text("Anonymous / guest", style = MaterialTheme.typography.bodyMedium)
                }
                if (!anonymous) {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        RemoteConnection(
                            name = name.ifBlank { "$host (${protocol.displayName})" },
                            protocol = protocol,
                            host = host.trim(),
                            port = port.toIntOrNull() ?: protocol.defaultPort,
                            username = username.trim(),
                            password = password,
                            anonymous = anonymous,
                            domain = domain.trim()
                        )
                    )
                },
                enabled = host.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
