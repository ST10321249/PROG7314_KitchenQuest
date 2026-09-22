package com.example.kitchenquest.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.kitchenquest.navigation.AppDestinations

private data class BottomNavigationItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavigationItems = listOf(
    BottomNavigationItem(
        title = "Home",
        route = AppDestinations.Home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavigationItem(
        title = "Recipes",
        route = AppDestinations.Recipes,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    BottomNavigationItem(
        title = "My Kitchen",
        route = AppDestinations.MyKitchen,
        selectedIcon = Icons.Filled.Kitchen,
        unselectedIcon = Icons.Outlined.Kitchen
    ),
    BottomNavigationItem(
        title = "Cook",
        route = AppDestinations.Cook,
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer
    ),
    BottomNavigationItem(
        title = "Profile",
        route = AppDestinations.Profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

internal fun bottomNavigationRootForRoute(
    route: String?
): String? {
    return when (route) {
        AppDestinations.Home ->
            AppDestinations.Home

        AppDestinations.Recipes,
        AppDestinations.WhatCanIMake,
        AppDestinations.SavedRecipes ->
            AppDestinations.Recipes

        AppDestinations.MyKitchen,
        AppDestinations.ShoppingList ->
            AppDestinations.MyKitchen

        AppDestinations.Cook,
        AppDestinations.KitchenTimer ->
            AppDestinations.Cook

        AppDestinations.Profile,
        AppDestinations.CookingHistory ->
            AppDestinations.Profile

        else -> null
    }
}

internal fun shouldShowBottomNavigation(
    route: String?
): Boolean {
    return bottomNavigationRootForRoute(route) != null
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val currentBackStackEntry =
        navController.currentBackStackEntryAsState().value

    val currentRoute =
        currentBackStackEntry?.destination?.route

    val selectedRoot =
        bottomNavigationRootForRoute(currentRoute)

    androidx.compose.foundation.layout.Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            bottomNavigationItems.forEach { item ->
                val selected = selectedRoot == item.route

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(AppDestinations.Home) {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector =
                                if (selected) {
                                    item.selectedIcon
                                } else {
                                    item.unselectedIcon
                                },
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
