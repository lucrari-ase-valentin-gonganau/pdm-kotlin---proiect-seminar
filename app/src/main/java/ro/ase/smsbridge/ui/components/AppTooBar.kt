package ro.ase.smsbridge.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import ro.ase.smsbridge.ui.Despre
import ro.ase.smsbridge.ui.Setari
import ro.ase.smsbridge.ui.StatusSms
import ro.ase.smsbridge.ui.TrimiteSms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    ecranCurent: NavKey?,
    onBackClick: () -> Unit,
    onMeniuClick: (NavKey) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val estePaginaPrincipala = ecranCurent is StatusSms
    
    val titlu = when(ecranCurent) {
        is Despre -> "Despre aplicație"
        is Setari -> "Setări"
        is StatusSms -> "Serviciu SMS"
        is TrimiteSms -> "Trimite SMS"
        else -> ""
    }

    TopAppBar(
        title = {
            Text(titlu)
        },
        navigationIcon = {
            if(!estePaginaPrincipala) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Înapoi")
                }
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Meniu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Trimite SMS") },
                    onClick = {
                        menuExpanded = false
                        onMeniuClick(TrimiteSms)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Setări") },
                    onClick = {
                        menuExpanded = false
                        onMeniuClick(Setari)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Despre") },
                    onClick = {
                        menuExpanded = false
                        onMeniuClick(Despre)
                    }
                )
            }
        }
    )
}
