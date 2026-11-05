package com.example.kitchenquest.data.translation

import java.util.Locale

/** Language tags used by KitchenQuest; keep displayed text separate from stored values. */
object TranslationLanguages {
    const val ENGLISH = "en"
    const val AFRIKAANS = "af"

    fun normalized(tag: String?): String? = when (tag?.trim()?.lowercase(Locale.ROOT)) {
        ENGLISH -> ENGLISH
        AFRIKAANS -> AFRIKAANS
        else -> null
    }

    /** Cache keys include both languages to avoid displaying a translation in the wrong locale. */
    fun cacheKey(text: String, source: String, target: String): String =
        "${source.length}:$source:${target.length}:$target:${text.length}:$text"

    fun requiresTranslation(text: String, source: String?, target: String?): Boolean {
        val sourceTag = normalized(source)
        val targetTag = normalized(target)
        return text.isNotBlank() && sourceTag != null && targetTag != null && sourceTag != targetTag
    }
}
