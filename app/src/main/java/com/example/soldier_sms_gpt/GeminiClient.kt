package com.example.soldier_sms_gpt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val TAG = "AskerGPT_Gemini"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE =
        "application/json; charset=utf-8".toMediaType()

    private val modelsToTry = listOf(
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash-lite"
    )

    suspend fun askAskerGpt(
        context: Context,
        prompt: String
    ): String? = withContext(Dispatchers.IO) {

        val apiKey = AppPreferences.getApiKey(context)

        if (apiKey.isBlank()) {
            Log.e(TAG, "[AI_HATA] API Key bulunamadı.")
            return@withContext "HATA: API Key eksik."
        }

        val cleanPrompt = prompt
            .trim()
            .take(1000)

        if (cleanPrompt.isBlank()) {
            return@withContext "HATA: Bos mesaj."
        }

        val jsonPayload = JSONObject().apply {

            put(
                "contents",
                org.json.JSONArray().apply {

                    put(
                        JSONObject().apply {

                            put(
                                "parts",
                                org.json.JSONArray().apply {

                                    put(
                                        JSONObject().apply {
                                            put(
                                                "text",
                                                """
                                                Sen Asker GPT'sin.
                                                Askerde olan kullanıcılar seni kullanıyor soruları için.    
                                                Kullanıcı SMS üzerinden sana ulaşıyor.
                                                Kısa, pratik ve samimi cevap ver.

                                                Cevap 150 karakteri geçmesin.
                                                Gereksiz açıklama yapma.
                                                Cevapların doğru olsun.
                                                Türkçe cevap ver.

                                                Kullanıcı mesajı:
                                                $cleanPrompt
                                                """.trimIndent()
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )

            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 100)
                }
            )
        }.toString()

        for (modelName in modelsToTry) {

            try {

                Log.d(
                    TAG,
                    "[AI_DENENIYOR] Model: $modelName"
                )

                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/" +
                            "$modelName:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(
                        jsonPayload.toRequestBody(
                            JSON_MEDIA_TYPE
                        )
                    )
                    .build()

                client.newCall(request).execute().use { response ->

                    val responseBody =
                        response.body?.string().orEmpty()

                    if (!response.isSuccessful) {

                        Log.w(
                            TAG,
                            "[AI_HTTP_HATA] $modelName -> ${response.code}"
                        )

                        when (response.code) {

                            401, 403 -> {
                                Log.e(
                                    TAG,
                                    "[AI_KRITIK] API Key geçersiz veya yetkisiz."
                                )

                                return@withContext "HATA: API Key gecersiz."
                            }

                            400 -> {
                                Log.e(
                                    TAG,
                                    "[AI_KRITIK] Geçersiz Gemini isteği."
                                )

                                return@withContext "HATA: Gecersiz AI istegi."
                            }

                            429, 500, 502, 503, 504 -> {
                                Log.w(
                                    TAG,
                                    "[AI_FALLBACK] $modelName -> ${response.code}"
                                )

                            }

                            else -> {
                                Log.w(
                                    TAG,
                                    "[AI_FALLBACK] Beklenmeyen HTTP ${response.code}"
                                )
                            }
                        }

                        return@use
                    }

                    if (responseBody.isBlank()) {
                        Log.w(
                            TAG,
                            "[AI_UYARI] Bos response."
                        )
                        return@use
                    }

                    val jsonObject =
                        JSONObject(responseBody)

                    val candidates =
                        jsonObject.optJSONArray("candidates")

                    if (candidates == null || candidates.length() == 0) {

                        Log.w(
                            TAG,
                            "[AI_UYARI] Candidate bulunamadı."
                        )

                        return@use
                    }

                    val text =
                        candidates
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .optString("text")

                    if (text.isNotBlank()) {

                        Log.d(
                            TAG,
                            "[AI_BASARILI] $modelName"
                        )

                        return@withContext text
                            .trim()
                            .take(150)
                    }
                }

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "[AI_EXCEPTION] $modelName -> ${e.message}"
                )
            }
        }

        Log.e(
            TAG,
            "[AI_KRITIK] Tüm modeller başarısız."
        )

        return@withContext "HATA: AI yanit vermedi."
    }
}