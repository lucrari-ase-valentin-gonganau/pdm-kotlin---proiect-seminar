package ro.ase.smsbridge.ui.screens

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import ro.ase.smsbridge.services.AppPreferences
import ro.ase.smsbridge.services.WebSocketService

@Composable
fun SetariScreen() {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val savedWsUrl by preferences.get("ws_url").collectAsState(initial = "")
    val runInBackground by preferences.getBoolean("run_in_background", false).collectAsState(initial = false)
    
    var wsUrlInput by remember { mutableStateOf("") }

    LaunchedEffect(savedWsUrl) {
        if (savedWsUrl != null) {
            wsUrlInput = savedWsUrl!!
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result.contents != null) {
                val scannnedUrl = result.contents.trim()
                wsUrlInput = scannnedUrl
                scope.launch {
                    preferences.save("ws_url", scannnedUrl)
                    Toast.makeText(context, "URL scanat și conectat!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configurare Conexiune",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = wsUrlInput,
            onValueChange = { wsUrlInput = it },
            label = { Text("URL WebSocket (ex: ws://server:port)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val finalUrl = wsUrlInput.trim()
                        preferences.save("ws_url", finalUrl)
                        Toast.makeText(context, "Setări salvate!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, null)
                Text(" Salvează")
            }

            Button(
                onClick = { 
                    scanLauncher.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("Scanează codul QR pentru server")
                            .setBeepEnabled(true)
                            .setOrientationLocked(false)
                    ) 
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Info, null)
                Text(" QR")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Servicii Background",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rulează în fundal",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Menține conexiunea activă chiar și când închizi aplicația.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = runInBackground,
                onCheckedChange = { isChecked ->
                    scope.launch {
                        preferences.saveBoolean("run_in_background", isChecked)
                        val intent = Intent(context, WebSocketService::class.java)
                        if (isChecked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else {
                            context.stopService(intent)
                        }
                    }
                }
            )
        }
    }
}
