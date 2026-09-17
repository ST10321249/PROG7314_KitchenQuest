package com.example.kitchenquest.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.kitchenquest.navigation.AppDestinations

data class BottomNavigationItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val navigationItems = listOf(
        BottomNavigationItem(
            title = "Home",
            route = AppDestinations.Home,
            icon = Icons.Default.Home
        ),
        BottomNavigationItem(
            title = "Recipes",
            route = AppDestinations.Recipes,
            icon = Icons.Default.MenuBook
        ),
        BottomNavigationItem(
            title = "My Kitchen",
            route = AppDestinations.MyKitchen,
            icon = Icons.Default.Kitchen
        ),
        BottomNavigationItem(
            title = "Cook",
            route = AppDestinations.Cook,
            icon = Icons.Default.Timer
        ),
        BottomNavigationItem(
            title = "Profile",
            route = AppDestinations.Profile,
            icon = Icons.Default.Person
        )
    )

    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar {
        navigationItems.forEach { item ->

            NavigationBarItem(
                selected = currentRoute == item.route,

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
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },

                label = {
                    Text(text = item.title)
                }
            )
        }
    }
}