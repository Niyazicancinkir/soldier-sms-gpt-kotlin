package com.example.soldier_sms_gpt

import java.util.concurrent.ConcurrentHashMap

object SmsLoopGuard {

    private const val MESSAGE_TTL_MS = 60_000L

    private val sentMessages =
        ConcurrentHashMap<String, Long>()

    fun registerSentMessage(message: String) {

        cleanup()

        val key = normalize(message)

        sentMessages[key] =
            System.currentTimeMillis()
    }

    fun isRecentlySent(message: String): Boolean {

        cleanup()

        val key = normalize(message)

        val timestamp =
            sentMessages[key]
                ?: return false

        return System.currentTimeMillis() - timestamp <
                MESSAGE_TTL_MS
    }

    private fun normalize(
        message: String
    ): String {

        return message
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    private fun cleanup() {

        val now =
            System.currentTimeMillis()

        sentMessages.entries.removeIf {

            now - it.value >
                    MESSAGE_TTL_MS
        }
    }
}