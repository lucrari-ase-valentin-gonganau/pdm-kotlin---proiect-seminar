package ro.ase.traseelemele.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import ro.ase.traseelemele.ui.Despre
import ro.ase.traseelemele.ui.DetaliiTraseu
import ro.ase.traseelemele.ui.Setari
import ro.ase.traseelemele.ui.StatusSms
import ro.ase.traseelemele.ui.TraseeleMele
import ro.ase.traseelemele.ui.TraseulNou

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(ecranCurent: NavKey?, onBackClick: () -> Unit) {
    val estePaginaPrincipala = ecranCurent is TraseeleMele
    val titlu = when(ecranCurent) {
        is Despre -> "Despre aplicatie"
        is DetaliiTraseu -> "Despre Traseu"
        is Setari -> "Setari"
        is TraseulNou -> "Traseu nou"
        is StatusSms -> "Stare mesaje"
        else -> ""
    }
    TopAppBar(
        title = {
            Text(titlu)
        },
        navigationIcon = {
            if(!estePaginaPrincipala) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Inapoi")
                }
            }
        }
    )
}
