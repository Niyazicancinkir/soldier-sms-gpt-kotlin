package com.example.soldier_sms_gpt

import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SmsQueueManager {

    private const val TAG = "AskerGPT_Queue"

    private const val SEND_DELAY_MS = 1500L

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val smsChannel =
        Channel<Pair<String, String>>(Channel.UNLIMITED)

    init {

        scope.launch {

            for (task in smsChannel) {

                val (phone, message) = task

                try {

                    sendSms(
                        phone,
                        message
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "[QUEUE_HATA]",
                        e
                    )
                }

                delay(SEND_DELAY_MS)
            }
        }
    }

    fun enqueue(
        phoneNumber: String,
        message: String
    ) {

        if (phoneNumber.isBlank()) {
            Log.w(
                TAG,
                "[QUEUE] Geçersiz telefon numarası."
            )
            return
        }

        if (message.isBlank()) {
            Log.w(
                TAG,
                "[QUEUE] Boş SMS gönderilmedi."
            )
            return
        }

        val result =
            smsChannel.trySend(
                Pair(phoneNumber, message)
            )

        if (result.isSuccess) {

            Log.d(
                TAG,
                "[QUEUE] Mesaj kuyruğa eklendi."
            )

        } else {

            Log.e(
                TAG,
                "[QUEUE] Mesaj kuyruğa eklenemedi."
            )
        }
    }

    private fun sendSms(
        phoneNumber: String,
        message: String
    ) {

        val smsManager =
            SmsManager.getDefault()

        val parts =
            smsManager.divideMessage(message)

        if (parts.size > 1) {

            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                null,
                null
            )

        } else {

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )
        }

        // SMS gönderimi başarılı olduktan sonra kaydet
        SmsLoopGuard.registerSentMessage(message)

        Log.d(
            TAG,
            "[SMS_BASARILI] -> ${TextSanitizer.maskPhoneNumber(phoneNumber)}"
        )
    }
}