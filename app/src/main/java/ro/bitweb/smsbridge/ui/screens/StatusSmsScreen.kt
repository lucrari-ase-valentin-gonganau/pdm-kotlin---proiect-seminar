package ro.bitweb.smsbridge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ro.bitweb.smsbridge.data.AppDatabase
import ro.bitweb.smsbridge.services.AppPreferences
import ro.bitweb.smsbridge.services.WebSocketClient

@Composable
fun StatusSmsScreen(
    onTrimiteSmsClick: () -> Unit,
    onSetariClick: () -> Unit,
    onVeziMesajeClick: (String) -> Unit
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val webSocketClient = remember { WebSocketClient.getInstance(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    
    val isConnected by webSocketClient.isConnected.collectAsState()
    val wsUrl by appPreferences.get("ws_url").collectAsState(initial = null)
    
    val receivedCount by database.messageDao().getReceivedCount().collectAsState(initial = 0)
    val sentCount by database.messageDao().getSentCount().collectAsState(initial = 0)

    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = onTrimiteSmsClick,
                            icon = { Icon(Icons.Default.Email, contentDescription = null) },
                            text = { Text("Trimite SMS") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = onSetariClick,
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            text = { Text("Setări SMS") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sumar Serviciu SMS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (wsUrl.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "URL WebSocket neconfigurat. Accesează setările.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            StatusCard(
                label = "Total SMS-uri în așteptare",
                value = receivedCount.toString(),
                color = MaterialTheme.colorScheme.primary,
                onClick = { onVeziMesajeClick("receptionate") }
            )

            StatusCard(
                label = "Total SMS-uri trimise",
                value = sentCount.toString(),
                color = MaterialTheme.colorScheme.secondary,
                onClick = { onVeziMesajeClick("trimise") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Starea serviciului",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isConnected) "Conectat la WebSocket" else "Deconectat",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isConnected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
