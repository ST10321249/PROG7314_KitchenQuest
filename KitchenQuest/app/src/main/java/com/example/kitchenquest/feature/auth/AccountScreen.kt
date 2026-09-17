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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.auth.AuthUser

@Composable
fun AccountScreen(
    user: AuthUser?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = "Name: ${user?.displayName ?: "Not available"}"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Email: ${user?.email ?: "Not available"}"
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