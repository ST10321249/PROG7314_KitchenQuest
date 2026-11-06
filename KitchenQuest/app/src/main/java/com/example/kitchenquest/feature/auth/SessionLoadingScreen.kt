package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.R
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun SessionLoadingScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorResource(R.color.kitchenquest_brand_orange)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The in-app loading state uses the same artwork as the Android launch splash.
            Image(
                painter = painterResource(R.drawable.kitchenquest_splash_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(254.dp)
            )
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
            Text(
                text = stringResource(R.string.auth_slogan),
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(Modifier.height(KitchenQuestDimens.LargeSpacing))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = androidx.compose.ui.graphics.Color.White,
                strokeWidth = 3.dp
            )
        }
    }
}
