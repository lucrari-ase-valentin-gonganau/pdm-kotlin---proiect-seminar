package ro.ase.smsbridge.services

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsSender {
    private const val TAG = "SmsSender"
    private const val SENT_ACTION = "RO_ASE_SMSBRIDGE_SMS_SENT"

    fun trimite(context: Context, numar: String, mesaj: String) {
        try {
            Log.d(TAG, "Pregătire trimitere SMS către $numar: $mesaj")
            
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val sentIntent = PendingIntent.getBroadcast(
                context, 
                numar.hashCode(), 
                Intent(SENT_ACTION).apply {
                    setPackage(context.packageName)
                    putExtra("dest", numar)
                }, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val dest = intent?.getStringExtra("dest") ?: "unknown"
                    val resultDescription = when (resultCode) {
                        Activity.RESULT_OK -> "SUCCESS (Mesaj trimis)"
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "ERROR: Generic Failure"
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "ERROR: No Service"
                        SmsManager.RESULT_ERROR_NULL_PDU -> "ERROR: Null PDU"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "ERROR: Radio Off"
                        else -> "ERROR: Unknown (code $resultCode)"
                    }
                    Log.i(TAG, "Rezultat sistem SMS către $dest: $resultDescription")
                    try {
                        context?.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            
            // Fix for Android 14+ receiver flags
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver, 
                    IntentFilter(SENT_ACTION), 
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(receiver, IntentFilter(SENT_ACTION))
            }

            if (mesaj.length > 160) {
                val piese = smsManager.divideMessage(mesaj)
                val sentIntents = ArrayList<PendingIntent>()
                for (i in piese.indices) sentIntents.add(sentIntent)
                smsManager.sendMultipartTextMessage(numar, null, piese, sentIntents, null)
            } else {
                smsManager.sendTextMessage(numar, null, mesaj, sentIntent, null)
            }
            
            Log.d(TAG, "Cerere trimisă către SmsManager pentru $numar")
        } catch (e: Exception) {
            Log.e(TAG, "Eroare fatală la trimiterea SMS: ${e.message}", e)
        }
    }
}
