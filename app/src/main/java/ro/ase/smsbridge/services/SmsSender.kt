package ro.ase.smsbridge.services

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

object SmsSender {
    fun trimite(context: Context, numar: String, mesaj: String) {
        val smsMananger = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault();
        }

        smsMananger.sendTextMessage(numar, null, mesaj, null, null);
    }
}
