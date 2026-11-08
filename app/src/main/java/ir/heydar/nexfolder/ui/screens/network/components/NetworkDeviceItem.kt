package ir.heydar.nexfolder.ui.screens.network.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.heydar.nexfolder.domain.model.network.NetworkProtocol
import ir.heydar.nexfolder.domain.model.network.NetworkShareInfo
import ir.heydar.nexfolder.ui.theme.Spacing

@Composable
fun NetworkDeviceItem(
    device: NetworkShareInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getProtocolEmoji(device.protocol),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ProtocolBadge(protocol = device.protocol)
        }
    }
}

@Composable
private fun ProtocolBadge(protocol: NetworkProtocol) {
    val (backgroundColor, textColor) = when (protocol) {
        NetworkProtocol.HTTP -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        NetworkProtocol.WEBDAV -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        NetworkProtocol.SMB -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        NetworkProtocol.FTP -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        NetworkProtocol.SFTP -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = protocol.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor
        )
    }
}

private fun getProtocolEmoji(protocol: NetworkProtocol): String {
    return when (protocol) {
        NetworkProtocol.HTTP -> "🌐"
        NetworkProtocol.WEBDAV -> "📁"
        NetworkProtocol.SMB -> "💾"
        NetworkProtocol.FTP -> "📡"
        NetworkProtocol.SFTP -> "🔐"
    }
}
