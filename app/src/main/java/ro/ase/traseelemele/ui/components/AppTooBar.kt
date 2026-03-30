package ro.ase.traseelemele.ui.components

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
import ro.ase.traseelemele.ui.Despre
import ro.ase.traseelemele.ui.DetaliiTraseu
import ro.ase.traseelemele.ui.Setari
import ro.ase.traseelemele.ui.StatusSms
import ro.ase.traseelemele.ui.TraseeleMele
import ro.ase.traseelemele.ui.TraseulNou

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    ecranCurent: NavKey?,
    onBackClick: () -> Unit,
    onMeniuClick: (NavKey) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val estePaginaPrincipala = ecranCurent is TraseeleMele
    
    val titlu = when(ecranCurent) {
        is TraseeleMele -> "Traseele Mele"
        is Despre -> "Despre aplicație"
        is DetaliiTraseu -> "Detalii Traseu"
        is Setari -> "Setări"
        is TraseulNou -> "Traseu nou"
        is StatusSms -> "Serviciu SMS"
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
