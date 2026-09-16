package com.example.kitchenquest.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kitchenquest.feature.auth.LoginScreen
import com.example.kitchenquest.feature.onboarding.OnboardingScreen
import com.example.kitchenquest.ui.components.AppScaffold
import com.example.kitchenquest.ui.screens.PlaceholderScreen

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    AppScaffold(
        navController = navController
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = AppDestinations.Onboarding,
            modifier = Modifier.padding(innerPadding)
        ) {

            // Authentication and onboarding
            composable(AppDestinations.Onboarding) {
                OnboardingScreen(
                    onContinue = {
                        navController.navigate(AppDestinations.Register)
                    },
                    onSkip = {
                        navController.navigate(AppDestinations.Register)
                    }
                )
            }

            composable(AppDestinations.Login) {
                LoginScreen(
                    onLogin = { _, _ ->
                        // Firebase login will be connected in a later commit.
                    },
                    onForgotPassword = {
                        navController.navigate(
                            AppDestinations.ForgotPassword
                        )
                    },
                    onCreateAccount = {
                        navController.navigate(
                            AppDestinations.Register
                        )
                    }
                )
            }

            composable(AppDestinations.Register) {
                PlaceholderScreen(
                    title = "Register"
                )
            }

            composable(AppDestinations.ForgotPassword) {
                PlaceholderScreen(
                    title = "Forgot Password"
                )
            }

            // Main application destinations
            composable(AppDestinations.Home) {
                PlaceholderScreen(
                    title = "Home"
                )
            }

            composable(AppDestinations.Recipes) {
                PlaceholderScreen(
                    title = "Recipes"
                )
            }

            composable(AppDestinations.MyKitchen) {
                PlaceholderScreen(
                    title = "My Kitchen"
                )
            }

            composable(AppDestinations.Cook) {
                PlaceholderScreen(
                    title = "Cook"
                )
            }

            composable(AppDestinations.Profile) {
                PlaceholderScreen(
                    title = "Profile"
                )
            }

            // Recipe features
            composable(AppDestinations.WhatCanIMake) {
                PlaceholderScreen(
                    title = "What Can I Make?"
                )
            }

            composable(AppDestinations.RecipeDetails) {
                PlaceholderScreen(
                    title = "Recipe Details"
                )
            }

            composable(AppDestinations.SavedRecipes) {
                PlaceholderScreen(
                    title = "Saved Recipes"
                )
            }

            // Pantry and shopping features
            composable(AppDestinations.IngredientDetails) {
                PlaceholderScreen(
                    title = "Ingredient Details"
                )
            }

            composable(AppDestinations.IngredientEditor) {
                PlaceholderScreen(
                    title = "Ingredient Editor"
                )
            }

            composable(AppDestinations.ShoppingList) {
                PlaceholderScreen(
                    title = "Shopping List"
                )
            }

            // Cooking features
            composable(AppDestinations.CookingMode) {
                PlaceholderScreen(
                    title = "Cooking Mode"
                )
            }

            composable(AppDestinations.ActiveTimers) {
                PlaceholderScreen(
                    title = "Active Timers"
                )
            }

            composable(AppDestinations.KitchenTimer) {
                PlaceholderScreen(
                    title = "Kitchen Timer"
                )
            }

            composable(AppDestinations.RecipeComplete) {
                PlaceholderScreen(
                    title = "Recipe Complete"
                )
            }

            // User features
            composable(AppDestinations.CookingHistory) {
                PlaceholderScreen(
                    title = "Cooking History"
                )
            }

            composable(AppDestinations.Settings) {
                PlaceholderScreen(
                    title = "Settings"
                )
            }
        }
    }
}