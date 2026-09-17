package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.kitchenquest.data.auth.AuthUser
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun AccountScreen(
    user: AuthUser?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedUser by remember {
        mutableStateOf(user)
    }

    LaunchedEffect(user) {
        if (user != null) {
            displayedUser = user
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                KitchenQuestDimens.ScreenPadding
            ),
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "Account",
            style =
                MaterialTheme.typography
                    .headlineMedium
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SectionSpacing
            )
        )

        Text(
            text =
                "Name: ${displayedUser?.displayName ?: "Not available"}",
            style =
                MaterialTheme.typography
                    .bodyLarge
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SmallSpacing
            )
        )

        Text(
            text =
                "Email: ${displayedUser?.email ?: "Not available"}",
            style =
                MaterialTheme.typography
                    .bodyLarge
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SectionSpacing
            )
        )

        KitchenQuestPrimaryButton(
            text = "Sign out",
            onClick = onSignOut,
            modifier =
                Modifier.fillMaxWidth()
        )
    }
}