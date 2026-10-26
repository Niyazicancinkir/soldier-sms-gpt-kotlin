package com.example.soldier_sms_gpt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TelegramClient {

    private const val TAG = "AskerGPT_Telegram"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // ------------------------------------------------
    // TELEGRAM -> MESAJ GÖNDER
    // ------------------------------------------------

    suspend fun sendMessage(
        context: Context,
        message: String
    ): Boolean = withContext(Dispatchers.IO) {

        val token =
            AppPreferences.getTelegramBotToken(context)

        val chatId =
            AppPreferences.getTelegramChatId(context)

        if (token.isBlank() || chatId.isBlank()) {
            Log.e(
                TAG,
                "[HATA] Telegram ayarları eksik."
            )

            return@withContext false
        }

        if (message.isBlank()) {
            return@withContext false
        }

        try {

            val url =
                "https://api.telegram.org/bot" +
                        "$token/sendMessage"

            val body =
                FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", message)
                    .build()

            val request =
                Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

            client
                .newCall(request)
                .execute()
                .use { response ->

                    val responseBody =
                        response.body
                            ?.string()
                            .orEmpty()

                    if (!response.isSuccessful) {

                        Log.e(
                            TAG,
                            "[SEND_HTTP_HATA] " +
                                    "${response.code}: $responseBody"
                        )

                        return@withContext false
                    }

                    return@withContext JSONObject(
                        responseBody
                    ).optBoolean(
                        "ok",
                        false
                    )
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "[SEND_EXCEPTION]",
                e
            )

            return@withContext false
        }
    }

    // ------------------------------------------------
    // TELEGRAM -> UPDATE AL
    // ------------------------------------------------

    suspend fun getUpdates(
        context: Context,
        offset: Long
    ): List<TelegramUpdate> =
        withContext(Dispatchers.IO) {

            val token =
                AppPreferences
                    .getTelegramBotToken(context)

            if (token.isBlank()) {
                return@withContext emptyList()
            }

            try {

                val url =
                    "https://api.telegram.org/bot" +
                            "$token/getUpdates" +
                            "?offset=$offset" +
                            "&limit=10" +
                            "&timeout=25"

                val request =
                    Request.Builder()
                        .url(url)
                        .get()
                        .build()

                client
                    .newCall(request)
                    .execute()
                    .use { response ->

                        val responseBody =
                            response.body
                                ?.string()
                                .orEmpty()

                        if (!response.isSuccessful) {

                            Log.e(
                                TAG,
                                "[GET_UPDATES_HATA] " +
                                        "${response.code}"
                            )

                            return@withContext emptyList()
                        }

                        val json =
                            JSONObject(
                                responseBody
                            )

                        if (
                            !json.optBoolean(
                                "ok",
                                false
                            )
                        ) {

                            return@withContext emptyList()
                        }

                        val result =
                            json.optJSONArray(
                                "result"
                            )
                                ?: return@withContext emptyList()

                        val updates =
                            mutableListOf<TelegramUpdate>()

                        for (
                        i in 0 until result.length()
                        ) {

                            val update =
                                result.getJSONObject(i)

                            val updateId =
                                update.getLong(
                                    "update_id"
                                )

                            val message =
                                update.optJSONObject(
                                    "message"
                                )
                                    ?: continue

                            val chat =
                                message.optJSONObject(
                                    "chat"
                                )
                                    ?: continue

                            val chatId =
                                chat.getLong("id")

                            val text =
                                message.optString(
                                    "text",
                                    ""
                                )

                            val from =
                                message.optJSONObject(
                                    "from"
                                )

                            val senderName =
                                from?.optString(
                                    "first_name",
                                    ""
                                )
                                    ?: ""

                            updates.add(
                                TelegramUpdate(
                                    updateId = updateId,
                                    chatId = chatId,
                                    text = text,
                                    senderName = senderName
                                )
                            )
                        }

                        return@withContext updates
                    }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "[GET_UPDATES_EXCEPTION]",
                    e
                )

                return@withContext emptyList()
            }
        }
}

data class TelegramUpdate(
    val updateId: Long,
    val chatId: Long,
    val text: String,
    val senderName: String
)