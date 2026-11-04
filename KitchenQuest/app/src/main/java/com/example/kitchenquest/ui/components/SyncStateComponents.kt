package com.example.kitchenquest.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.kitchenquest.R
import com.example.kitchenquest.data.sync.SyncState
import com.example.kitchenquest.ui.theme.KitchenAmberLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRed
import com.example.kitchenquest.ui.theme.KitchenRedLight
import com.example.kitchenquest.ui.theme.KitchenText

// Explains an item whose change has not reached the server yet. Nothing is shown once it has.
@Composable
fun SyncStateNote(state: SyncState, modifier: Modifier = Modifier) {
    if (state == SyncState.SYNCED) return

    val failed = state == SyncState.FAILED
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = if (failed) KitchenRedLight else KitchenAmberLight
    ) {
        Text(
            text = stringResource(if (failed) R.string.sync_note_failed else R.string.sync_note_pending),
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            style = MaterialTheme.typography.labelMedium,
            color = if (failed) KitchenRed else KitchenText
        )
    }
}