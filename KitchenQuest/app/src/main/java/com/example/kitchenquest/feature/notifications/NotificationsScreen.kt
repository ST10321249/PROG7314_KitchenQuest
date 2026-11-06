package com.example.kitchenquest.feature.notifications

import com.example.kitchenquest.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRedLight
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.notifications.ExpiryAlertNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onOpenKitchen: () -> Unit,
    onOpenShoppingList: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        KitchenQuestTopBar(
            title = stringResource(R.string.notifications_notifications_a274f4),
            onBack = onBack,
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding)
        )

        PushTestCard(
            modifier = Modifier.padding(
                start = KitchenQuestDimens.ScreenPadding,
                end = KitchenQuestDimens.ScreenPadding,
                bottom = KitchenQuestDimens.SmallSpacing
            )
        )

        ExpiryCheckCard(
            modifier = Modifier.padding(
                start = KitchenQuestDimens.ScreenPadding,
                end = KitchenQuestDimens.ScreenPadding,
                bottom = KitchenQuestDimens.SmallSpacing
            )
        )

        when {
            state.isLoading -> {
                KitchenQuestLoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KitchenQuestDimens.ScreenPadding),
                    message = stringResource(R.string.notifications_checking_your_kitchen_78e0a8)
                )
            }

            state.errorMessage != null -> {
                KitchenQuestErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            state.expiringCount == 0 && state.shoppingCount == 0 -> {
                KitchenQuestEmptyState(
                    title = stringResource(R.string.notifications_you_re_all_caught_up_61ef64),
                    message = stringResource(R.string.notifications_kitchen_reminders_and_shopping_updates_will_appear_here_f3a5e5),
                    modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.MediumSpacing)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.notifications_today_03219b),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.expiringCount > 0) {
                        item {
                            NotificationCard(
                                emoji = "🥕",
                                title = if (state.expiringCount == 1) stringResource(R.string.notif_expiring_one, state.expiringCount) else stringResource(R.string.notif_expiring_many, state.expiringCount),
                                message = stringResource(R.string.notifications_open_my_kitchen_to_use_them_before_they_expire_73fbbd),
                                containerColor = KitchenRedLight,
                                onClick = onOpenKitchen
                            )
                        }
                    }

                    if (state.shoppingCount > 0) {
                        item {
                            NotificationCard(
                                emoji = "🛒",
                                title = stringResource(R.string.notifications_shopping_list_reminder_fdccb4),
                                message = if (state.shoppingCount == 1) stringResource(R.string.notif_shopping_one, state.shoppingCount) else stringResource(R.string.notif_shopping_many, state.shoppingCount),
                                containerColor = KitchenOrangeLight,
                                onClick = onOpenShoppingList
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    emoji: String,
    title: String,
    message: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(modifier = Modifier.size(KitchenQuestDimens.MediumSpacing))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PushTestCard(
    modifier: Modifier = Modifier,
    viewModel: PushTestViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    KitchenQuestCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.push_test_title),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = when (val current = state) {
                PushTestState.Idle -> stringResource(R.string.push_test_message)
                PushTestState.Sending -> stringResource(R.string.push_test_sending)
                PushTestState.Sent -> stringResource(R.string.push_test_sent)
                is PushTestState.Failed -> stringResource(R.string.push_test_failed, current.reason)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(KitchenQuestDimens.SmallSpacing))
        KitchenQuestSecondaryButton(
            text = stringResource(R.string.push_test_button),
            onClick = viewModel::send,
            enabled = state != PushTestState.Sending,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
private fun ExpiryCheckCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<ExpiryAlertNotifier.Outcome?>(null) }
    var failed by remember { mutableStateOf(false) }

    KitchenQuestCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.expiry_check_title),
            style = MaterialTheme.typography.titleSmall
        )
        val status = when {
            checking -> stringResource(R.string.expiry_check_running)
            failed -> stringResource(R.string.expiry_check_error)
            outcome == ExpiryAlertNotifier.Outcome.POSTED -> stringResource(R.string.expiry_check_posted)
            outcome == ExpiryAlertNotifier.Outcome.ALREADY_NOTIFIED -> stringResource(R.string.expiry_check_already_posted)
            outcome == ExpiryAlertNotifier.Outcome.NOTHING_EXPIRING -> stringResource(R.string.expiry_check_none)
            outcome == ExpiryAlertNotifier.Outcome.TURNED_OFF -> stringResource(R.string.expiry_check_disabled)
            outcome == ExpiryAlertNotifier.Outcome.SIGNED_OUT -> stringResource(R.string.expiry_check_signed_out)
            outcome == ExpiryAlertNotifier.Outcome.NOT_ALLOWED -> stringResource(R.string.expiry_check_blocked)
            else -> stringResource(R.string.expiry_check_description)
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(KitchenQuestDimens.SmallSpacing))
        KitchenQuestSecondaryButton(
            text = stringResource(R.string.expiry_check_button),
            onClick = {
                if (!checking) {
                    checking = true
                    failed = false
                    scope.launch {
                        try {
                            outcome = withContext(Dispatchers.IO) {
                                ExpiryAlertNotifier(
                                    context,
                                    KitchenQuestDatabase.getInstance(context),
                                    AppRepositories::currentUserId
                                ).run()
                            }
                        } catch (error: Exception) {
                            failed = true
                        } finally {
                            checking = false
                        }
                    }
                }
            },
            enabled = !checking,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
