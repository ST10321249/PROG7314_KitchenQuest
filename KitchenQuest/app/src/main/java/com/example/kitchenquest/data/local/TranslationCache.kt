package com.example.kitchenquest.data.local

import java.security.MessageDigest

// Remembers translations on the phone so recipe text that was translated once can be
// shown again offline. It is not tied to a user, because a translation of a sentence is
// the same for everybody.
//
// Typical use with the translation service:
//
//   val cached = cache.get("en", "af", text)
//   val shown = cached ?: service.translateOrOriginal(text, "en", "af").also {
//       if (it != text) cache.put("en", "af", text, it)
//   }
//
// Never use a translation as an id or to match ingredients. Keep the original text.
class TranslationCache(
    private val dao: TranslationCacheDao,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val clock: () -> Long = System::currentTimeMillis
) {

    suspend fun get(sourceLanguage: String, targetLanguage: String, text: String): String? {
        if (text.isBlank()) return null
        return dao.get(sourceLanguage, targetLanguage, hash(text))
    }

    suspend fun put(
        sourceLanguage: String,
        targetLanguage: String,
        text: String,
        translated: String
    ) {
        if (text.isBlank() || translated.isBlank()) return

        dao.put(
            TranslationCacheEntity(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                textHash = hash(text),
                translatedText = translated,
                createdAt = clock()
            )
        )

        val excess = dao.count() - maxEntries
        if (excess > 0) dao.deleteOldest(excess)
    }

    suspend fun clear() = dao.clear()

    // Surrounding spaces do not change the meaning, so they do not change the key.
    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 2_000
    }
}
