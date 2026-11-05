package com.example.kitchenquest.data.preferences

import java.util.Locale

/** Supported application languages (English and Afrikaans, both official SA languages). */
object AppLanguage {
    const val ENGLISH = "en"
    const val AFRIKAANS = "af"

    fun isSupported(code: String?): Boolean {
        val normalized = code?.trim()?.lowercase(Locale.ROOT)
        return normalized == ENGLISH || normalized == AFRIKAANS
    }

    fun normalized(code: String?): String =
        if (code?.trim()?.lowercase(Locale.ROOT) == AFRIKAANS) AFRIKAANS else ENGLISH
}
