package com.example.soldier_sms_gpt

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SMS_PERMISSION_CODE = 100
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        checkPermissions()

        val etApiKey =
            findViewById<EditText>(
                R.id.etApiKey
            )

        val etAllowedSender =
            findViewById<EditText>(
                R.id.etAllowedSender
            )

        val etTelegramToken =
            findViewById<EditText>(
                R.id.etTelegramToken
            )

        val etTelegramChatId =
            findViewById<EditText>(
                R.id.etTelegramChatId
            )

        val btnSave =
            findViewById<Button>(
                R.id.btnSave
            )

        val btnKillSwitch =
            findViewById<Button>(
                R.id.btnKillSwitch
            )

        // ----------------------------------------
        // Kayıtlı ayarları yükle
        // ----------------------------------------

        etApiKey.setText(
            AppPreferences.getApiKey(this)
        )

        val savedSender = AppPreferences.getAllowedSender(this)
        etAllowedSender.setText(
            if (savedSender.isNotBlank()) TextSanitizer.maskPhoneNumber(savedSender) else ""
        )

        etTelegramToken.setText(
            AppPreferences.getTelegramBotToken(this)
        )

        etTelegramChatId.setText(
            AppPreferences.getTelegramChatId(this)
        )

        // ----------------------------------------
        // AYARLARI KAYDET
        // ----------------------------------------

        btnSave.setOnClickListener {

            val apiKey =
                etApiKey.text
                    .toString()
                    .trim()

            var sender =
                etAllowedSender.text
                    .toString()
                    .trim()

            val telegramToken =
                etTelegramToken.text
                    .toString()
                    .trim()

            val telegramChatId =
                etTelegramChatId.text
                    .toString()
                    .trim()

            // ------------------------------------
            // VALIDATION
            // ------------------------------------

            if (apiKey.isBlank()) {

                showMessage(
                    "Gemini API Key boş bırakılamaz."
                )

                return@setOnClickListener
            }

            if (sender.contains("*")) {
                sender = AppPreferences.getAllowedSender(this)
            } else {
                if (!isValidPhone(sender)) {
                    showMessage(
                        "Geçerli bir telefon numarası girin."
                    )
                    return@setOnClickListener
                }
            }

            if (telegramToken.isBlank()) {

                showMessage(
                    "Telegram Bot Token boş bırakılamaz."
                )

                return@setOnClickListener
            }

            if (telegramChatId.isBlank()) {

                showMessage(
                    "Telegram Chat ID boş bırakılamaz."
                )

                return@setOnClickListener
            }

            // ------------------------------------
            // AYARLARI KAYDET
            // ------------------------------------

            AppPreferences.setApiKey(
                this,
                apiKey
            )

            AppPreferences.setAllowedSender(
                this,
                sender
            )
            etAllowedSender.setText(TextSanitizer.maskPhoneNumber(sender))

            AppPreferences.setTelegramBotToken(
                this,
                telegramToken
            )

            AppPreferences.setTelegramChatId(
                this,
                telegramChatId
            )

            // ------------------------------------
            // TELEGRAM SERVİSİNİ BAŞLAT
            // ------------------------------------

            val serviceIntent =
                Intent(
                    this,
                    TelegramPollingService::class.java
                )

            ContextCompat.startForegroundService(
                this,
                serviceIntent
            )

            showMessage(
                "Ayarlar kaydedildi. Telegram aktif."
            )
        }

        // ----------------------------------------
        // KILL SWITCH
        // ----------------------------------------

        btnKillSwitch.setOnClickListener {

            // SMS Receiver kapat
            packageManager.setComponentEnabledSetting(

                ComponentName(
                    this,
                    SmsReceiver::class.java
                ),

                PackageManager
                    .COMPONENT_ENABLED_STATE_DISABLED,

                PackageManager
                    .DONT_KILL_APP
            )

            // Telegram servisini durdur
            val serviceIntent =
                Intent(
                    this,
                    TelegramPollingService::class.java
                )

            stopService(serviceIntent)

            showMessage(
                "SMS ve Telegram sistemi devre dışı bırakıldı."
            )
        }
    }

    // --------------------------------------------
    // TELEFON NUMARASI KONTROLÜ
    // --------------------------------------------

    private fun isValidPhone(
        phone: String
    ): Boolean {

        return phone.matches(
            Regex(
                "^\\+?[0-9]{10,15}$"
            )
        )
    }

    // --------------------------------------------
    // TOAST
    // --------------------------------------------

    private fun showMessage(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // --------------------------------------------
    // SMS PERMISSIONS
    // --------------------------------------------

    private fun checkPermissions() {

        val permissions =
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
            )

        val missing =
            permissions.filter {

                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (missing.isNotEmpty()) {

            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                SMS_PERMISSION_CODE
            )
        }
    }
}