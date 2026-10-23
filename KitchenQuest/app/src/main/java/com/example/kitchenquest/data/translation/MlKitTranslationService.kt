package com.example.kitchenquest.data.translation

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

/**
 * Shared on-device translation for API content and user-visible notes.
 * Static app UI always uses Android string resources; never translate record IDs,
 * quantities, search identifiers, or values written to MongoDB.
 *
 * Call downloadModels() from Settings before using translation offline. With no
 * model or network, translateOrOriginal() safely shows the original wording.
 */
class MlKitTranslationService(
    private val manager: RemoteModelManager = RemoteModelManager.getInstance()
) {
    private val modelLock = Any()
    private val cacheLock = Any()
    private val clients = mutableMapOf<String, Translator>()
    private val recentTranslations = object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > 256
    }

    suspend fun downloadedModels(): Set<String> = withContext(Dispatchers.IO) {
        manager.getDownloadedModels(TranslateRemoteModel::class.java)
            .await()
            .map { it.language }
            .toSet()
    }

    /** Default to Wi-Fi to avoid surprise mobile-data downloads (~30 MB/model). */
    suspend fun downloadModels(wifiOnly: Boolean = true): Set<String> {
        val conditions = DownloadConditions.Builder().apply {
            if (wifiOnly) requireWifi()
        }.build()
        for (language in listOf(TranslationLanguages.ENGLISH, TranslationLanguages.AFRIKAANS)) {
            val model = TranslateRemoteModel.Builder(language).build()
            manager.download(model, conditions).await()
        }
        return downloadedModels()
    }

    /** Pure display translation. Does not change the original source or database record. */
    suspend fun translateOrOriginal(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        if (!TranslationLanguages.requiresTranslation(text, sourceLanguage, targetLanguage)) return text
        val source = TranslationLanguages.normalized(sourceLanguage) ?: return text
        val target = TranslationLanguages.normalized(targetLanguage) ?: return text
        val key = TranslationLanguages.cacheKey(text, source, target)
        synchronized(cacheLock) { recentTranslations[key] }?.let { return it }
        return try {
            // Do not initiate a potentially large download while just displaying a recipe.
            val installed = downloadedModels()
            if (source !in installed || target !in installed) return text
            val client = translator(source, target)
            val result = client.translate(text).await()
            synchronized(cacheLock) { recentTranslations[key] = result }
            result
        } catch (error: Exception) {
            Log.w(TAG, "Translation unavailable, showing original content", error)
            text
        }
    }

    private fun translator(source: String, target: String): Translator = synchronized(modelLock) {
        val key = "$source->$target"
        clients.getOrPut(key) {
            val sourceCode = TranslateLanguage.fromLanguageTag(source)
                ?: error("Unsupported source language")
            val targetCode = TranslateLanguage.fromLanguageTag(target)
                ?: error("Unsupported target language")
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceCode)
                    .setTargetLanguage(targetCode)
                    .build()
            )
        }
    }

    fun close() {
        synchronized(modelLock) {
            clients.values.forEach { it.close() }
            clients.clear()
        }
        synchronized(cacheLock) { recentTranslations.clear() }
    }

    companion object { private const val TAG = "KitchenQuestTranslate" }
}

/** Process-wide service so a new screen doesn't have to construct its own ML Kit clients. */
object KitchenQuestTranslation {
    val service: MlKitTranslationService by lazy { MlKitTranslationService() }
}
