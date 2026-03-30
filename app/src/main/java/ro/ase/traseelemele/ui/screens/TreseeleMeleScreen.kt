package ro.ase.traseelemele.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Meniu(val nume: String, val descriere: String)

@Composable
fun TraseeleMeleScreen(onItemClick: (Meniu) -> Unit, onStatusSmsClick: () -> Unit) {
    val listaMeniuri = listOf(
        Meniu("Traseu Munte", "Un traseu spectaculos prin Carpati"),
        Meniu("Plimbare Delta", "Explorarea canalelor Deltei Dunarii"),
        Meniu("Traseu Transfagarasan", "Drumul printre nori"),
        Meniu("City Break Brasov", "Descoperirea centrului vechi"),
        Meniu("Drumetie Cheile Nerei", "Cascada Bigar si tunelele"),
        Meniu("Traseu Transfagarasan", "Drumul printre nori"),
        Meniu("City Break Brasov", "Descoperirea centrului vechi"),
        Meniu("Drumetie Cheile Nerei", "Cascada Bigar si tunelele")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            items(listaMeniuri) { meniu ->
                MeniuItem(meniu, onClick = { onItemClick(meniu) })
            }
        }

        // Link discret către StatusSmsScreen (ex: versiunea aplicației) mutat în dreapta sus
        Text(
            text = "v1.0.4",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clickable { onStatusSmsClick() }
        )
    }
}

@Composable
fun MeniuItem(meniu: Meniu, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = meniu.nume,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = meniu.descriere,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
