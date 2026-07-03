package ro.bitweb.smsbridge.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ro.bitweb.smsbridge.data.AppDatabase
import ro.bitweb.smsbridge.data.Message

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val database = AppDatabase.getDatabase(context)
            
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.displayMessageBody
                
                Log.d("SmsReceiver", "SMS received from $sender: $body")
                
                // Salvare în Room
                CoroutineScope(Dispatchers.IO).launch {
                    database.messageDao().insert(
                        Message(
                            sender = sender ?: "Unknown",
                            message = body ?: "",
                            status = "Received"
                        )
                    )
                }
            }
        }
    }
}
