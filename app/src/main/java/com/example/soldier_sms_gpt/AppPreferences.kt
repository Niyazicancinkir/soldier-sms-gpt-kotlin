package com.example.soldier_sms_gpt

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object AppPreferences {

    private const val PREF_NAME = "soldiers_secure_prefs"

    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_ALLOWED_SENDER = "allowed_sender"

    private const val KEY_TELEGRAM_TOKEN = "telegram_bot_token"
    private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"

    private fun getPrefs(context: Context): SharedPreferences {

        val masterKeyAlias = MasterKeys.getOrCreate(
            MasterKeys.AES256_GCM_SPEC
        )

        return EncryptedSharedPreferences.create(
            PREF_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // -------------------------
    // GEMINI
    // -------------------------

    fun getApiKey(context: Context): String {
        return getPrefs(context)
            .getString(KEY_API_KEY, "")
            ?: ""
    }

    fun setApiKey(
        context: Context,
        key: String
    ) {
        getPrefs(context)
            .edit()
            .putString(KEY_API_KEY, key.trim())
            .apply()
    }

    // -------------------------
    // ALLOWED SMS SENDER
    // -------------------------

    fun getAllowedSender(context: Context): String {
        return getPrefs(context)
            .getString(KEY_ALLOWED_SENDER, "")
            ?: ""
    }

    fun setAllowedSender(
        context: Context,
        sender: String
    ) {
        getPrefs(context)
            .edit()
            .putString(KEY_ALLOWED_SENDER, sender.trim())
            .apply()
    }

    // -------------------------
    // TELEGRAM BOT TOKEN
    // -------------------------

    fun getTelegramBotToken(
        context: Context
    ): String {

        return getPrefs(context)
            .getString(KEY_TELEGRAM_TOKEN, "")
            ?: ""
    }

    fun setTelegramBotToken(
        context: Context,
        token: String
    ) {

        getPrefs(context)
            .edit()
            .putString(
                KEY_TELEGRAM_TOKEN,
                token.trim()
            )
            .apply()
    }

    // -------------------------
    // TELEGRAM CHAT ID
    // -------------------------

    fun getTelegramChatId(
        context: Context
    ): String {

        return getPrefs(context)
            .getString(
                KEY_TELEGRAM_CHAT_ID,
                ""
            )
            ?: ""
    }
    private const val KEY_TELEGRAM_LAST_UPDATE_ID =
        "telegram_last_update_id"

    fun getTelegramLastUpdateId(
        context: Context
    ): Long {

        return getPrefs(context)
            .getLong(
                KEY_TELEGRAM_LAST_UPDATE_ID,
                0L
            )
    }

    fun setTelegramLastUpdateId(
        context: Context,
        updateId: Long
    ) {

        getPrefs(context)
            .edit()
            .putLong(
                KEY_TELEGRAM_LAST_UPDATE_ID,
                updateId
            )
            .apply()
    }
    fun setTelegramChatId(
        context: Context,
        chatId: String
    ) {

        getPrefs(context)
            .edit()
            .putString(
                KEY_TELEGRAM_CHAT_ID,
                chatId.trim()
            )
            .apply()
    }
}
