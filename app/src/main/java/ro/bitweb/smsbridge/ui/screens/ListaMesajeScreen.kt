package ro.bitweb.smsbridge.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.bitweb.smsbridge.data.AppDatabase
import ro.bitweb.smsbridge.data.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Cate mesaje se incarca initial si cate se mai adauga la fiecare scroll (infinite scroll).
private const val PAGINA = 20

private val formatData = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaMesajeScreen(tip: String) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // Filtre active.
    var filtruNumar by remember { mutableStateOf("") }
    var fromTs by remember { mutableStateOf<Long?>(null) }
    var toTs by remember { mutableStateOf<Long?>(null) }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    // Cate randuri cerem din DB. Se reseteaza la 20 ori de cate ori se schimba un
    // filtru (altfel am ramane cu o limita mare pe un rezultat filtrat mic).
    var limit by remember(tip, filtruNumar, fromTs, toTs) { mutableStateOf(PAGINA) }

    val mesaje by remember(tip, filtruNumar, fromTs, toTs, limit) {
        database.messageDao().getMessagesFiltered(tip, filtruNumar.trim(), fromTs, toTs, limit)
    }.collectAsState(initial = emptyList())

    val listState = rememberLazyListState()

    // Cand ultimul element vizibil se apropie de finalul listei incarcate SI pagina
    // curenta e plina (mesaje.size >= limit, deci probabil mai sunt), cerem inca 20.
    val shouldLoadMore by remember {
        derivedStateOf {
            val ultimVizibil = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            ultimVizibil >= mesaje.size - 3 && mesaje.size >= limit
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) limit += PAGINA
    }

    // Export CSV prin Storage Access Framework: userul alege unde salveaza, nu ne
    // trebuie nicio permisiune de stocare. Exportam exact ce corespunde filtrelor.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val randuri = withContext(Dispatchers.IO) {
                    database.messageDao().getMessagesForExport(tip, filtruNumar.trim(), fromTs, toTs)
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(construiesteCsv(randuri).toByteArray(Charsets.UTF_8))
                    }
                }
                Toast.makeText(
                    context,
                    "Export finalizat (${randuri.size} mesaje)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = when (tip) {
                    "trimise" -> "Mesaje Trimise"
                    "receptionate" -> "Mesaje în Așteptare"
                    else -> "Toate mesajele"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = {
                val nume = "sms_export_" + formatData.format(Date()).replace(".", "-") + ".csv"
                exportLauncher.launch(nume)
            }) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text("  CSV")
            }
        }

        // Cautare in numar SAU in textul mesajului.
        OutlinedTextField(
            value = filtruNumar,
            onValueChange = { filtruNumar = it },
            label = { Text("Caută (număr sau text)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (filtruNumar.isNotEmpty()) {
                    IconButton(onClick = { filtruNumar = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Șterge")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        // Interval de date.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showFromPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Text("  " + (fromTs?.let { formatData.format(Date(it)) } ?: "De la"))
            }
            OutlinedButton(
                onClick = { showToPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Text("  " + (toTs?.let { formatData.format(Date(it)) } ?: "Până la"))
            }
        }

        if (filtruNumar.isNotEmpty() || fromTs != null || toTs != null) {
            TextButton(onClick = {
                filtruNumar = ""
                fromTs = null
                toTs = null
            }) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Text("  Șterge filtrele")
            }
        }

        if (mesaje.isEmpty()) {
            Text(
                text = "Niciun mesaj pentru filtrele curente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(mesaje, key = { it.id }) { mesaj ->
                MesajItem(mesaj)
            }
        }
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = fromTs)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Inceputul zilei selectate.
                    fromTs = state.selectedDateMillis
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Anulează") }
            }
        ) { DatePicker(state = state) }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = toTs)
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Includem toata ziua selectata (pana la ultima milisecunda).
                    toTs = state.selectedDateMillis?.let { it + 86_400_000L - 1 }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Anulează") }
            }
        ) { DatePicker(state = state) }
    }
}

// CSV minimal: nr_telefon, stare_mesaj, mesaj. Escapam campurile care contin
// virgula / ghilimele / newline conform RFC 4180.
private fun construiesteCsv(mesaje: List<Message>): String {
    val sb = StringBuilder()
    sb.append("nr_telefon,stare_mesaj,mesaj\r\n")
    for (m in mesaje) {
        sb.append(escapeCsv(m.sender)).append(',')
        sb.append(escapeCsv(m.status)).append(',')
        sb.append(escapeCsv(m.message)).append("\r\n")
    }
    return sb.toString()
}

private fun escapeCsv(camp: String): String {
    val trebuie = camp.contains(',') || camp.contains('"') ||
        camp.contains('\n') || camp.contains('\r')
    val escapat = camp.replace("\"", "\"\"")
    return if (trebuie) "\"$escapat\"" else escapat
}

@Composable
fun MesajItem(mesaj: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = mesaj.sender,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${mesaj.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = mesaj.message,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = mesaj.status,
                style = MaterialTheme.typography.labelMedium,
                // Statusurile de trimitere sunt acum in romana ("Trimis", "Livrat",
                // "Trimitere in curs...", "Esuat: ..."), nu mai era corect sa cautam
                // literal cuvantul englezesc "Sent".
                color = when {
                    mesaj.status.startsWith("Esuat") -> MaterialTheme.colorScheme.error
                    mesaj.status == "Trimis" || mesaj.status == "Livrat" ||
                        mesaj.status.contains("Sent") -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}
