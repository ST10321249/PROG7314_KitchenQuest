package com.example.kitchenquest.feature.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

// This is a visual shell only. Timer, expiry and shopping alerts land here once
// push notifications (Firebase Cloud Messaging) are built — that's a later POE item,
// not part of Part 2.
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {

        KitchenQuestTopBar(
            title = "Notifications",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        Column(modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding)) {
            KitchenQuestEmptyState(
                title = "No notifications yet",
                message = "Timer, expiry and shopping alerts will appear here."
            )
        }
    }
}
