package ro.bitweb.smsbridge.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ro.bitweb.smsbridge.data.AppDatabase
import ro.bitweb.smsbridge.services.AppPreferences
import ro.bitweb.smsbridge.ui.components.WebSocketStatusIndicator

// Verde "acordat". colorScheme nu are un verde semantic, asa ca il definim explicit;
// rosul de "lipsa" il luam din colorScheme.error.
private val VerdeAcordat = Color(0xFF2E7D32)

@Composable
fun StatusSmsScreen(
    onVeziMesajeClick: (String) -> Unit
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val database = remember { AppDatabase.getDatabase(context) }

    val wsUrl by appPreferences.get("ws_url").collectAsState(initial = null)

    val sentCount by database.messageDao().getSentCount().collectAsState(initial = 0)
    val totalCount by database.messageDao().getTotalCount().collectAsState(initial = 0)

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WebSocketStatusIndicator()

            PermisiuniCard(wsConfigurat = !wsUrl.isNullOrBlank())

            // Un singur buton de sumar: (trimise / total). Restul (total - trimise)
            // sunt mesajele inca in asteptare. Apasarea deschide lista completa.
            StatusCard(
                label = "SMS-uri (trimise / total)",
                value = "$sentCount/$totalCount",
                color = MaterialTheme.colorScheme.primary,
                onClick = { onVeziMesajeClick("toate") }
            )
        }
    }
}

// O permisiune runtime de care depinde functionarea aplicatiei.
//  - minSdk: permisiunea e ceruta la runtime doar de la acest nivel de API in sus.
//    Sub el, e acordata automat la instalare -> o consideram "OK" (bulina verde).
private data class PermisiuneNecesara(
    val eticheta: String,
    val descriere: String,
    val permission: String,
    val minSdk: Int = 0
)

@Composable
fun PermisiuniCard(wsConfigurat: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permisiunea se poate schimba cat timp app-ul e in fundal (userul o poate acorda
    // sau revoca din Setari). Reverificam la fiecare ON_RESUME.
    var refresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permisiuni = remember {
        listOf(
            PermisiuneNecesara(
                eticheta = "Trimitere SMS",
                descriere = "Necesară pentru a trimite SMS-urile primite de la server",
                permission = Manifest.permission.SEND_SMS
            ),
            PermisiuneNecesara(
                eticheta = "Notificări",
                descriere = "Pentru notificarea serviciului activ în fundal",
                permission = Manifest.permission.POST_NOTIFICATIONS,
                minSdk = Build.VERSION_CODES.TIRAMISU
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Stare aplicație",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Configurarea URL-ului WebSocket (mutata aici din cardul rosu separat).
            StareRow(
                eticheta = "Conexiune WebSocket",
                descriere = if (wsConfigurat) {
                    "Server configurat"
                } else {
                    "URL neconfigurat. Accesează Setările din meniu."
                },
                ok = wsConfigurat
            )

            permisiuni.forEach { p ->
                PermisiuneRow(p, refresh)
            }
        }
    }
}

@Composable
private fun PermisiuneRow(p: PermisiuneNecesara, refresh: Int) {
    val context = LocalContext.current

    // Bump local dupa raspunsul dialogului de permisiune (in plus fata de ON_RESUME).
    var localRefresh by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { localRefresh++ }

    val acordata = remember(refresh, localRefresh) {
        if (Build.VERSION.SDK_INT < p.minSdk) {
            true
        } else {
            ContextCompat.checkSelfPermission(context, p.permission) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    StareRow(eticheta = p.eticheta, descriere = p.descriere, ok = acordata) {
        if (!acordata) {
            Button(onClick = { launcher.launch(p.permission) }) {
                Text("Permite")
            }
        }
    }
}

// Un rand de stare: bulina verde/rosie + eticheta + descriere, cu un slot trailing
// optional (ex. butonul "Permite" pentru permisiuni).
@Composable
private fun StareRow(
    eticheta: String,
    descriere: String,
    ok: Boolean,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (ok) VerdeAcordat else MaterialTheme.colorScheme.error)
        ) {}
        Column(modifier = Modifier.weight(1f)) {
            Text(text = eticheta, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = descriere,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        trailing()
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
