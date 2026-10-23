package com.example.kitchenquest.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.R
import com.example.kitchenquest.data.translation.KitchenQuestTranslation
import com.example.kitchenquest.data.translation.TranslationLanguages
import kotlinx.coroutines.launch

/** Manage and demonstrate on-device English/Afrikaans translation models. */
@Composable
fun TranslationModelsPanel(modifier: Modifier = Modifier) {
    val service = remember { KitchenQuestTranslation.service }
    val scope = rememberCoroutineScope()
    var downloaded by remember { mutableStateOf<Set<String>>(emptySet()) }
    var checking by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(service) {
        try { downloaded = service.downloadedModels() }
        catch (_: Exception) { error = "model_status_unavailable" }
        finally { checking = false }
    }

    val ready = TranslationLanguages.ENGLISH in downloaded &&
        TranslationLanguages.AFRIKAANS in downloaded

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.translation_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.translation_description), style = MaterialTheme.typography.bodySmall)
            Text(
                when {
                    checking -> stringResource(R.string.translation_checking)
                    ready -> stringResource(R.string.translation_ready)
                    else -> stringResource(R.string.translation_not_downloaded)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (error != null) Text(
                stringResource(R.string.translation_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !checking && !downloading && !ready,
                    onClick = {
                        downloading = true
                        error = null
                        scope.launch {
                            try { downloaded = service.downloadModels(wifiOnly = true) }
                            catch (_: Exception) { error = "model_download_failed" }
                            finally { downloading = false }
                        }
                    }
                ) {
                    Text(stringResource(if (downloading) R.string.translation_downloading else R.string.translation_download))
                }
                OutlinedButton(
                    enabled = !checking && !downloading && ready,
                    onClick = {
                        preview = null
                        error = null
                        scope.launch {
                            preview = service.translateOrOriginal(
                                "Cook the pasta in boiling water until tender.",
                                TranslationLanguages.ENGLISH,
                                TranslationLanguages.AFRIKAANS
                            )
                        }
                    }
                ) { Text(stringResource(R.string.translation_test)) }
            }
            if (preview != null) {
                Text(stringResource(R.string.translation_test_result), style = MaterialTheme.typography.labelMedium)
                Text(preview.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.translation_google_attribution), style = MaterialTheme.typography.labelSmall)
            }
            Text(stringResource(R.string.translation_wifi_note), style = MaterialTheme.typography.labelSmall)
        }
    }
}
