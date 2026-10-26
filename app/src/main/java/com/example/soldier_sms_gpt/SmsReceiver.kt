package com.example.soldier_sms_gpt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {

        private const val TAG =
            "AskerGPT_Receiver"

        private const val FAMILY_PREFIX =
            "AILE."

        private val receiverScope =
            CoroutineScope(
                SupervisorJob() +
                        Dispatchers.IO
            )
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action !=
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {
            return
        }

        val pendingResult =
            goAsync()

        val appContext =
            context.applicationContext

        receiverScope.launch {

            try {

                val messages =
                    Telephony.Sms.Intents
                        .getMessagesFromIntent(intent)

                val allowedSender =
                    AppPreferences
                        .getAllowedSender(
                            appContext
                        )

                if (allowedSender.isBlank()) {

                    Log.w(
                        TAG,
                        "[BLOKE] Allowed sender tanımlanmamış."
                    )

                    return@launch
                }

                for (sms in messages) {

                    val sender =
                        sms.displayOriginatingAddress
                            ?: continue

                    val messageBody =
                        sms.displayMessageBody
                            ?: continue

                    Log.d(
                        TAG,
                        "[SMS_IN] Sender=${TextSanitizer.maskPhoneNumber(sender)}"
                    )

                    /*
                     * 1 — Yetkili numara
                     */

                    if (
                        !isAllowedSender(
                            sender,
                            allowedSender
                        )
                    ) {

                        Log.w(
                            TAG,
                            "[BLOKE] Yetkisiz numara: ${TextSanitizer.maskPhoneNumber(sender)}"
                        )

                        continue
                    }

                    /*
                     * 2 — Kill Switch
                     */

                    if (
                        messageBody
                            .trim()
                            .equals(
                                "[KILL_SWITCH]",
                                ignoreCase = true
                            )
                    ) {

                        disableReceiver(
                            appContext
                        )

                        Log.w(
                            TAG,
                            "[KILL_SWITCH] Receiver kapatıldı."
                        )

                        continue
                    }

                    /*
                     * 3 — Kendi gönderdiğimiz
                     * SMS tekrar geldiyse ignore et
                     */

                    if (
                        SmsLoopGuard
                            .isRecentlySent(
                                messageBody
                            )
                    ) {

                        Log.w(
                            TAG,
                            "[LOOP_BREAKER] " +
                                    "Kendi SMS'imiz tekrar yakalandı."
                        )

                        continue
                    }

                    /*
                     * 4 — TELEGRAM ROUTING
                     *
                     * Kabul edilen formatlar:
                     *
                     * AILE. Mesaj
                     * Aile. Mesaj
                     * aile. Mesaj
                     * AİLE. Mesaj
                     * AiLe. Mesaj
                     *
                     * Büyük/küçük harf fark etmez.
                     *
                     * "." zorunludur.
                     */

                    val normalizedMessage =
                        messageBody
                            .trim()
                            .uppercase()
                            .replace("İ", "I")

                    if (
                        normalizedMessage
                            .startsWith(FAMILY_PREFIX)
                    ) {

                        val telegramMessage =
                            messageBody
                                .trim()
                                .substringAfter(".")
                                .trim()

                        if (
                            telegramMessage.isBlank()
                        ) {

                            Log.w(
                                TAG,
                                "[TELEGRAM] Boş aile mesajı."
                            )

                            continue
                        }

                        Log.d(
                            TAG,
                            "[TELEGRAM] Aile mesajı yakalandı."
                        )

                        val success =
                            TelegramClient.sendMessage(
                                appContext,
                                "📱 Asker telefonu:\n\n" +
                                        telegramMessage
                            )

                        if (success) {

                            Log.d(
                                TAG,
                                "[TELEGRAM] Mesaj gönderildi."
                            )

                        } else {

                            Log.e(
                                TAG,
                                "[TELEGRAM] Mesaj gönderilemedi."
                            )
                        }

                        /*
                         * Aile mesajı olduğu için
                         * Gemini'ye kesinlikle gitme.
                         */

                        continue
                    }

                    /*
                     * 5 — GEMINI
                     */

                    Log.d(
                        TAG,
                        "[AI] Gemini isteği başlatılıyor."
                    )

                    val response =
                        GeminiClient.askAskerGpt(
                            appContext,
                            messageBody
                        )

                    if (
                        response.isNullOrBlank()
                    ) {

                        Log.w(
                            TAG,
                            "[AI] Boş cevap."
                        )

                        continue
                    }

                    /*
                     * 6 — ASCII
                     */

                    val cleanResponse =
                        TextSanitizer
                            .toAscii(response)

                    if (
                        cleanResponse.isBlank()
                    ) {
                        continue
                    }

                    /*
                     * 7 — SMS QUEUE
                     */

                    SmsQueueManager.enqueue(
                        sender,
                        cleanResponse
                    )

                    Log.d(
                        TAG,
                        "[PIPELINE] Gemini → SMS queue"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "[RECEIVER_HATA]",
                    e
                )

            } finally {

                pendingResult.finish()

                Log.d(
                    TAG,
                    "[RECEIVER] Broadcast tamamlandı."
                )
            }
        }
    }

    private fun isAllowedSender(
        sender: String,
        allowed: String
    ): Boolean {

        return normalizePhone(sender) ==
                normalizePhone(allowed)
    }

    private fun normalizePhone(
        number: String
    ): String {

        return number
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
    }

    private fun disableReceiver(
        context: Context
    ) {

        context.packageManager
            .setComponentEnabledSetting(

                android.content.ComponentName(
                    context,
                    SmsReceiver::class.java
                ),

                PackageManager
                    .COMPONENT_ENABLED_STATE_DISABLED,

                PackageManager
                    .DONT_KILL_APP
            )
    }
}