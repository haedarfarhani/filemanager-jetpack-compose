package com.heydar.filemanager.ui.screens.network

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.heydar.filemanager.core.icons.AppIcons
import com.heydar.filemanager.core.network.QrCodeGenerator
import com.heydar.filemanager.ui.screens.network.components.ServerStatusCard
import com.heydar.filemanager.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkShareScreen(
    onNavigateBack: () -> Unit,
    viewModel: NetworkShareViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(NetworkShareEvent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Share Files") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = AppIcons.Nav.back),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Server Status Card
            ServerStatusCard(
                isRunning = state.isRunning,
                serverUrl = state.serverUrl,
                clientCount = state.connectedClients.size,
                onCopyUrl = { viewModel.onEvent(NetworkShareEvent.CopyUrl) }
            )

            // Start/Stop Button
            if (state.isRunning) {
                Button(
                    onClick = { viewModel.onEvent(NetworkShareEvent.StopServer) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        painter = painterResource(id = AppIcons.Nav.close),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Stop Server")
                }
            } else {
                Button(
                    onClick = { viewModel.onEvent(NetworkShareEvent.StartServer) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = AppIcons.Action.share),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Start Server")
                }
            }

            // Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Port
                    OutlinedTextField(
                        value = state.port.toString(),
                        onValueChange = { port ->
                            port.toIntOrNull()?.let {
                                viewModel.onEvent(NetworkShareEvent.SetPort(it))
                            }
                        },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isRunning
                    )

                    // Root Path
                    OutlinedTextField(
                        value = state.rootPath ?: "External Storage",
                        onValueChange = {},
                        label = { Text("Root Path") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )

                    // Password Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Password",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Protect access with a password",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.passwordEnabled,
                            onCheckedChange = { viewModel.onEvent(NetworkShareEvent.TogglePassword) },
                            enabled = !state.isRunning
                        )
                    }

                    // Password Field
                    if (state.passwordEnabled) {
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { viewModel.onEvent(NetworkShareEvent.SetPassword(it)) },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isRunning
                        )
                    }
                }
            }

            // QR Code
            if (state.isRunning && state.serverUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan to connect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        val qrBitmap = remember(state.serverUrl) {
                            QrCodeGenerator.generate(state.serverUrl!!)
                        }
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = state.serverUrl!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Warning
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "⚠️ Files are accessible to anyone on the same network. Use a password for security.",
                    modifier = Modifier.padding(Spacing.cardPadding),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
