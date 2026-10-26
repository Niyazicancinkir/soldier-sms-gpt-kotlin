package com.example.soldier_sms_gpt

object TextSanitizer {

    private val turkishMapping = mapOf(
        'ç' to 'c',
        'Ç' to 'C',
        'ğ' to 'g',
        'Ğ' to 'G',
        'ı' to 'i',
        'İ' to 'I',
        'ö' to 'o',
        'Ö' to 'O',
        'ş' to 's',
        'Ş' to 'S',
        'ü' to 'u',
        'Ü' to 'U'
    )

    fun maskPhoneNumber(phone: String): String {
        val clean = phone.trim()
        if (clean.length <= 4) return "*".repeat(clean.length)
        return clean.take(4) + "*".repeat(clean.length - 4)
    }

    fun toAscii(text: String): String {

        var sanitized = text

        turkishMapping.forEach { (turkish, ascii) ->
            sanitized = sanitized.replace(turkish, ascii)
        }

        sanitized = sanitized
            .replace("\u0000", "")
            .replace("\r", " ")
            .replace("\n", " ")

        sanitized = sanitized
            .replace(Regex("\\s+"), " ")
            .trim()

        return sanitized
    }
}