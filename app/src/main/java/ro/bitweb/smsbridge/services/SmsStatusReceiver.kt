package ro.bitweb.smsbridge.services

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ro.bitweb.smsbridge.SmsSender
import ro.bitweb.smsbridge.data.AppDatabase

/**
 * Prinde broadcast-urile ACTION_SMS_SENT / ACTION_SMS_DELIVERED trimise de SmsManager
 * dupa ce radio-ul a incercat REALMENTE sa trimita mesajul. Pana acum aceste rezultate
 * erau ignorate complet (sentIntent/deliveryIntent erau null in SmsSender), asa ca un
 * esec real (fara semnal, SIM lipsa, blocaj operator) nu se vedea nicaieri.
 */
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val externalId = intent.getStringExtra(SmsSender.EXTRA_EXTERNAL_ID) ?: return

        val status = when (intent.action) {
            SmsSender.ACTION_SMS_SENT -> statusDupaTrimitere(resultCode)
            SmsSender.ACTION_SMS_DELIVERED -> {
                if (resultCode == Activity.RESULT_OK) "Livrat" else "Livrare esuata"
            }
            else -> return
        }

        Log.d("SmsStatusReceiver", "externalId=$externalId action=${intent.action} status=$status")

        // goAsync() tine receiver-ul (si procesul) in viata pana apelam finish().
        // Fara el, onReceive() se termina imediat si Android poate omori procesul
        // inainte ca scrierea in Room sa se finalizeze -> status pierdut.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(context).messageDao()
                    .updateStatusByExternalId(externalId, status)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun statusDupaTrimitere(resultCode: Int): String = when (resultCode) {
        Activity.RESULT_OK -> "Trimis"
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Esuat: eroare generica"
        SmsManager.RESULT_ERROR_NO_SERVICE -> "Esuat: fara semnal/serviciu"
        SmsManager.RESULT_ERROR_NULL_PDU -> "Esuat: eroare interna (PDU null)"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "Esuat: modul avion / radio oprit"
        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Esuat: limita de SMS-uri depasita"
        SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> "Esuat: blocat de lista FDN"
        else -> "Esuat: cod necunoscut ($resultCode)"
    }
}
