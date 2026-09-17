package com.example.kitchenquest.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.kitchenquest.navigation.AppDestinations

@Composable
fun AppScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = currentBackStackEntry?.destination?.route

    val mainRoutes = listOf(
        AppDestinations.Home,
        AppDestinations.Recipes,
        AppDestinations.MyKitchen,
        AppDestinations.Cook,
        AppDestinations.Profile
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in mainRoutes) {
                BottomNavigationBar(
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}