package ro.bitweb.smsbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ro.bitweb.smsbridge.services.WebSocketClient

// Bulina verde/rosu + text (Conectat / Deconectat) care arata starea conexiunii WebSocket.
// Foloseste aceeasi instanta WebSocketClient ca restul aplicatiei, deci se actualizeaza in timp real.
@Composable
fun WebSocketStatusIndicator(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webSocketClient = remember { WebSocketClient.getInstance(context) }
    val isConnected by webSocketClient.isConnected.collectAsState()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336))
        )
        Text(
            text = if (isConnected) "Conectat" else "Deconectat",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}
