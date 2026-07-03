package ro.bitweb.smsbridge.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ro.bitweb.smsbridge.SmsSender
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
            statusMesaj = trimiteSiFormateazaStatus(context, numar, mesaj)
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
                        statusMesaj = trimiteSiFormateazaStatus(context, numar, mesaj)
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
                color = if (statusMesaj.startsWith("SMS trimis"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                )
        }
    }

}

// Apeleaza SmsSender si transforma RezultatTrimitere intr-un text de status pentru UI.
// Inainte, "SMS trimis catre X" aparea mereu, chiar daca trimiterea a esuat intern
// (SmsSender.trimite() nu intorcea nimic, deci UI-ul afisa mereu succes).
private fun trimiteSiFormateazaStatus(
    context: android.content.Context,
    numar: String,
    mesaj: String
): String {
    return when (val rezultat = SmsSender.trimite(context, numar, mesaj)) {
        is SmsSender.RezultatTrimitere.Trimis -> "SMS trimis către $numar"
        is SmsSender.RezultatTrimitere.PermisiuneLipsa -> "Permisiunea SEND_SMS a fost refuzată"
        is SmsSender.RezultatTrimitere.Eroare -> "Eroare la trimitere: ${rezultat.exceptie.message}"
    }
}