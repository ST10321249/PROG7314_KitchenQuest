package com.example.kitchenquest.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import com.example.kitchenquest.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.kitchenquest.navigation.AppDestinations
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

private data class BottomNavigationItem(
    val titleResId: Int,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavigationItems = listOf(
    BottomNavigationItem(
        titleResId = R.string.nav_home,
        route = AppDestinations.Home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavigationItem(
        titleResId = R.string.nav_recipes,
        route = AppDestinations.Recipes,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    BottomNavigationItem(
        titleResId = R.string.kitchen_title,
        route = AppDestinations.MyKitchen,
        selectedIcon = Icons.Filled.Kitchen,
        unselectedIcon = Icons.Outlined.Kitchen
    ),
    BottomNavigationItem(
        titleResId = R.string.nav_cook,
        route = AppDestinations.Cook,
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer
    ),
    BottomNavigationItem(
        titleResId = R.string.nav_profile,
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

/**
 * Bottom tabs always return to the root screen for that section.
 *
 * This is intentionally not using saveState/restoreState. KitchenQuest has one
 * NavHost rather than separate nested graphs per tab, so restoring a tab state
 * could also restore a child destination such as Shopping List. That made the
 * Home tab appear selected incorrectly and made repeated tab presses feel
 * inconsistent. Popping to Home first gives each tab normal, predictable app
 * behaviour: Home means Home, My Kitchen means the My Kitchen root, etc.
 */
private fun navigateToBottomRoot(
    navController: NavHostController,
    route: String
) {
    if (route == AppDestinations.Home) {
        val popped = navController.popBackStack(
            route = AppDestinations.Home,
            inclusive = false
        )

        if (!popped && navController.currentDestination?.route != AppDestinations.Home) {
            navController.navigate(AppDestinations.Home) {
                launchSingleTop = true
            }
        }
        return
    }

    navController.navigate(route) {
        popUpTo(AppDestinations.Home) {
            inclusive = false
        }
        launchSingleTop = true
    }
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

    Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        )

        NavigationBar(
            modifier = Modifier.height(KitchenQuestDimens.BottomNavigationHeight),
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
                            navigateToBottomRoot(
                                navController = navController,
                                route = item.route
                            )
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
                            contentDescription = stringResource(item.titleResId),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(item.titleResId),
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
