
package com.example.kitchenquest.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.kitchenquest.R
import com.example.kitchenquest.data.sync.SyncOverview
import com.example.kitchenquest.data.sync.SyncState
import com.example.kitchenquest.ui.theme.KitchenAmber
import com.example.kitchenquest.ui.theme.KitchenAmberLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRed
import com.example.kitchenquest.ui.theme.KitchenRedLight
import com.example.kitchenquest.ui.theme.KitchenText
import androidx.compose.ui.unit.dp

// Explains an item whose change has not reached the server yet.
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
            text = stringResource(
                if (failed) R.string.sync_note_failed
                else R.string.sync_note_pending
            ),
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            style = MaterialTheme.typography.labelMedium,
            color = if (failed) KitchenRed else KitchenText
        )
    }
}

// A small mark next to an item. Synced items show nothing.
@Composable
fun SyncStateBadge(state: SyncState, modifier: Modifier = Modifier) {
    when (state) {
        SyncState.SYNCED -> Unit

        SyncState.PENDING -> SyncBadgeContent(
            icon = Icons.Outlined.Schedule,
            text = stringResource(R.string.sync_badge_pending),
            color = KitchenAmber,
            modifier = modifier
        )

        SyncState.FAILED -> SyncBadgeContent(
            icon = Icons.Outlined.ErrorOutline,
            text = stringResource(R.string.sync_badge_failed),
            color = KitchenRed,
            modifier = modifier
        )
    }
}

@Composable
private fun SyncBadgeContent(
    icon: ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.TinySpacing)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// Shown when the phone is offline, or when changes are still waiting to be sent.
@Composable
fun OfflineBanner(
    overview: SyncOverview,
    modifier: Modifier = Modifier
) {
    if (overview.isOnline && overview.waitingChanges == 0) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = KitchenAmberLight
    ) {
        Column(modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing)) {
            if (!overview.isOnline) {
                Text(
                    text = stringResource(R.string.sync_offline_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = KitchenText
                )
            }

            if (overview.waitingChanges > 0) {
                if (!overview.isOnline) {
                    Spacer(Modifier.height(KitchenQuestDimens.TinySpacing))
                }

                Text(
                    text = if (overview.waitingChanges == 1) {
                        stringResource(R.string.sync_waiting_one)
                    } else {
                        stringResource(
                            R.string.sync_waiting_many,
                            overview.waitingChanges
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = KitchenText
                )
            }
        }
    }
}

@Composable
fun FailedChangeDialog(
    itemName: String,
    reason: String?,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.sync_failed_title, itemName))
        },
        text = {
            Column {
                Text(
                    stringResource(
                        if (onRetry != null) {
                            R.string.sync_transfer_failed_explanation
                        } else if (onEdit != null) {
                            R.string.sync_failed_explanation
                        } else {
                            R.string.sync_failed_explanation_delete_only
                        }
                    )
                )

                if (!reason.isNullOrBlank()) {
                    Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                    Text(
                        text = stringResource(R.string.sync_failed_reason, reason),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.sync_retry_transfer))
                }
            } else if (onEdit != null) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.sync_failed_edit))
                }
            } else {
                TextButton(onClick = onDelete) {
                    Text(
                        stringResource(R.string.sync_failed_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            Row {
                if (onEdit != null && onRetry == null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            stringResource(R.string.sync_failed_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.sync_failed_close))
                }
            }
        }
    )
}
