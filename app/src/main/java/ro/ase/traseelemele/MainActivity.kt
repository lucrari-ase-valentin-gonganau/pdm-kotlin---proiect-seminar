package ro.ase.traseelemele

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ro.ase.traseelemele.services.WebSocketService
import ro.ase.traseelemele.ui.AppNavGraph
import ro.ase.traseelemele.ui.theme.TraseeleenteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pornim serviciul WebSocket pentru a procesa mesajele in fundal
        val serviceIntent = Intent(this, WebSocketService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        enableEdgeToEdge()
        setContent {
            TraseeleenteTheme {
                AppNavGraph()
            }
        }
    }
}
