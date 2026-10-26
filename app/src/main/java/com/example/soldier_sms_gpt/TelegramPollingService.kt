package com.example.soldier_sms_gpt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelegramPollingService : Service() {

    companion object {

        private const val TAG =
            "AskerGPT_TelegramService"

        private const val CHANNEL_ID =
            "asker_gpt_telegram"

        private const val NOTIFICATION_ID =
            1001

        private const val COMMAND_PREFIX =
            "/asker"
    }

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        Log.d(
            TAG,
            "[SERVICE] Telegram servisi başladı."
        )

        startPolling()
    }

    private fun startPolling() {

        serviceScope.launch {

            var offset =
                AppPreferences
                    .getTelegramLastUpdateId(
                        applicationContext
                    )

            while (isActive) {

                try {

                    val updates =
                        TelegramClient.getUpdates(
                            applicationContext,
                            offset + 1
                        )

                    for (update in updates) {

                        // Update'i tüket
                        offset =
                            update.updateId

                        AppPreferences
                            .setTelegramLastUpdateId(
                                applicationContext,
                                offset
                            )

                        processUpdate(update)
                    }

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "[POLLING_HATA]",
                        e
                    )

                    delay(5000)
                }
            }
        }
    }

    private suspend fun processUpdate(
        update: TelegramUpdate
    ) {

        val configuredChatId =
            AppPreferences
                .getTelegramChatId(
                    applicationContext
                )

        /*
         * Sadece bizim kayıtlı aile grubumuz.
         */

        if (
            update.chatId.toString() !=
            configuredChatId
        ) {

            Log.w(
                TAG,
                "[BLOKE] Yetkisiz Telegram chat: " +
                        update.chatId
            )

            return
        }

        val text =
            update.text.trim()

        if (text.isBlank()) {
            return
        }

        /*
         * Sadece /asker komutu.
         */

        if (
            !text.startsWith(
                COMMAND_PREFIX,
                ignoreCase = true
            )
        ) {

            return
        }

        val smsText =
            text
                .substring(
                    COMMAND_PREFIX.length
                )
                .trim()

        if (smsText.isBlank()) {

            Log.w(
                TAG,
                "[ASKER] Boş komut."
            )

            return
        }

        /*
         * SMS boyutunu şimdilik
         * güvenli bir seviyede tut.
         */

        val finalSms =
            TextSanitizer
                .toAscii(
                    smsText.take(500)
                )

        if (finalSms.isBlank()) {
            return
        }

        Log.d(
            TAG,
            "[ASKER] SMS kuyruğa gönderiliyor: " +
                    finalSms
        )

        /*
         * Mevcut SMS Queue sistemini
         * aynen kullanıyoruz.
         */

        SmsQueueManager.enqueue(
            configuredChatIdToPhone(),
            finalSms
        )
    }

    private fun configuredChatIdToPhone(): String {

        /*
         * Telegram'dan SMS göndereceğimiz
         * telefon numarası mevcut
         * Allowed Sender numarası.
         */

        return AppPreferences
            .getAllowedSender(
                applicationContext
            )
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "Asker GPT"
            )
            .setContentText(
                "Telegram bağlantısı aktif"
            )
            .setSmallIcon(
                android.R.drawable.stat_notify_sync
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Asker GPT Telegram",
                    NotificationManager
                        .IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "[SERVICE] Telegram servisi kapandı."
        )

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}