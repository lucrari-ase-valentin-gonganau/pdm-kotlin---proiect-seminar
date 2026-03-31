package ro.ase.smsbridge.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ro.ase.smsbridge.services.SmsSender
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


@Composable
fun TrimiteSmsScreen() {
    val context = LocalContext.current

    var numar by remember { mutableStateOf("") }
    var mesaj by remember { mutableStateOf("") }

    var statusMesaj by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { esteAcordata ->
        if (esteAcordata) {
            SmsSender.trimite(context, numar, mesaj)
            statusMesaj = "SMS trimis către $numar"
        } else {
            statusMesaj = "Permisiunea SEND_SMS a fost refuzată"
        }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = numar,
            onValueChange = { numar = it },
            label = { Text("Număr telefon") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = mesaj,
            onValueChange = { mesaj = it },
            label = { Text("Mesaj") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = {
                if (numar.isNotBlank() && mesaj.isNotBlank()) {

                    val permisiuneAcordata = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (permisiuneAcordata) {
                        // trimite direct, fara sa mai intrebe
                        SmsSender.trimite(context, numar, mesaj)
                        statusMesaj = "SMS trimis către $numar"
                    } else {
                        // prima data, cere permisiunea
                        permissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }

                } else {
                    statusMesaj = "Completează numărul și mesajul"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Trimite SMS")
        }

        if (statusMesaj.isNotBlank()) {
            Text(
                text = statusMesaj,
                color = if (statusMesaj.contains("a fost trimis"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                )
        }
    }

}