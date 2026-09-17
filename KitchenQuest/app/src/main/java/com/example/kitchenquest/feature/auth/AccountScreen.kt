package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.auth.AuthUser

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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Account"
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Name: ${displayedUser?.displayName ?: "Not available"}"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Email: ${displayedUser?.email ?: "Not available"}"
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sign out"
            )
        }
    }
}