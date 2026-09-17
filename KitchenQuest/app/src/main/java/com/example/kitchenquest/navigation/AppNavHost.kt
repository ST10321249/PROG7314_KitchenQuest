package com.example.kitchenquest.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kitchenquest.feature.auth.AuthViewModel
import com.example.kitchenquest.feature.auth.ForgotPasswordScreen
import com.example.kitchenquest.feature.auth.LoginScreen
import com.example.kitchenquest.feature.auth.RegisterScreen
import com.example.kitchenquest.feature.onboarding.OnboardingScreen
import com.example.kitchenquest.ui.components.AppScaffold
import com.example.kitchenquest.ui.screens.PlaceholderScreen

@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    val authViewModel: AuthViewModel = viewModel()

    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authState.user) {
        if (authState.user != null) {

            navController.navigate(
                AppDestinations.Home
            ) {
                popUpTo(
                    AppDestinations.Onboarding
                ) {
                    inclusive = true
                }

                launchSingleTop = true
            }
        }
    }

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
                        authViewModel.clearFeedback()

                        navController.navigate(
                            AppDestinations.Register
                        )
                    },
                    onSkip = {
                        authViewModel.clearFeedback()

                        navController.navigate(
                            AppDestinations.Register
                        )
                    }
                )
            }

            composable(AppDestinations.Login) {
                LoginScreen(
                    onLogin = { email, password ->
                        authViewModel.login(
                            email = email,
                            password = password
                        )
                    },
                    onForgotPassword = {
                        authViewModel.clearFeedback()

                        navController.navigate(
                            AppDestinations.ForgotPassword
                        )
                    },
                    onCreateAccount = {
                        authViewModel.clearFeedback()

                        navController.navigate(
                            AppDestinations.Register
                        )
                    },
                    isLoading = authState.isLoading,
                    errorMessage = authState.errorMessage
                )
            }

            composable(AppDestinations.Register) {
                RegisterScreen(
                    onRegister = {
                            displayName,
                            email,
                            password ->

                        authViewModel.register(
                            displayName = displayName,
                            email = email,
                            password = password
                        )
                    },
                    onBackToLogin = {
                        authViewModel.clearFeedback()

                        navController.navigate(
                            AppDestinations.Login
                        )
                    },
                    isLoading = authState.isLoading,
                    errorMessage = authState.errorMessage
                )
            }

            composable(AppDestinations.ForgotPassword) {
                ForgotPasswordScreen(
                    onSendResetLink = { email ->
                        authViewModel.sendPasswordReset(
                            email = email
                        )
                    },
                    onBackToLogin = {
                        authViewModel.clearFeedback()

                        navController.navigate(
                            AppDestinations.Login
                        )
                    },
                    isLoading = authState.isLoading,
                    resetRequested =
                        authState.passwordResetSent,
                    errorMessage =
                        authState.errorMessage
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