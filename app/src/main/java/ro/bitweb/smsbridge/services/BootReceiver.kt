package ro.bitweb.smsbridge.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * RECEIVE_BOOT_COMPLETED era deja declarata in manifest, dar nu exista niciun receiver
 * inregistrat pentru ea, deci dupa un restart de telefon (sau daca sistemul omoara
 * serviciul, frecvent pe Xiaomi/Huawei/Samsung cu optimizare agresiva de baterie),
 * WebSocketService nu mai pornea automat pana nu redeschideai manual aplicatia.
 *
 * Pornirea unui foreground service dintr-un receiver de BOOT_COMPLETED este una dintre
 * putinele exceptii documentate de Android de la restrictiile de "background start"
 * introduse in Android 12, asa ca startForegroundService() de aici e permis.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completat, pornesc WebSocketService")
            val serviceIntent = Intent(context, WebSocketService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
