package com.example.kitchenquest.navigation

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kitchenquest.data.auth.GoogleSignInClient
import com.example.kitchenquest.data.preferences.OnboardingPreferences
import com.example.kitchenquest.feature.auth.AccountScreen
import com.example.kitchenquest.feature.auth.AuthViewModel
import com.example.kitchenquest.feature.auth.ForgotPasswordScreen
import com.example.kitchenquest.feature.auth.LoginScreen
import com.example.kitchenquest.feature.auth.RegisterScreen
import com.example.kitchenquest.feature.auth.SessionLoadingScreen
import com.example.kitchenquest.feature.onboarding.OnboardingScreen
import com.example.kitchenquest.feature.onboarding.OnboardingSelection
import com.example.kitchenquest.ui.components.AppScaffold
import com.example.kitchenquest.ui.screens.PlaceholderScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost() {

    val navController =
        rememberNavController()

    val authViewModel: AuthViewModel =
        viewModel()

    val authState by
    authViewModel
        .uiState
        .collectAsState()

    val context =
        LocalContext.current

    val activity =
        context as? Activity

    val coroutineScope =
        rememberCoroutineScope()

    val googleSignInClient =
        remember {
            GoogleSignInClient()
        }

    val onboardingPreferences =
        remember {
            OnboardingPreferences(
                context.applicationContext
            )
        }

    LaunchedEffect(
        authState.isAuthChecked,
        authState.user
    ) {

        if (
            !authState.isAuthChecked
        ) {
            return@LaunchedEffect
        }

        val currentRoute =
            navController
                .currentDestination
                ?.route

        val currentUser =
            authState.user

        if (
            currentUser != null
        ) {

            val existingPreferences =
                onboardingPreferences
                    .getUserPreferences(
                        currentUser.uid
                    )

            val pendingSelection =
                onboardingPreferences
                    .getPendingSelection()

            val firstAuthenticationOnInstall =
                !onboardingPreferences
                    .hasAuthenticatedBefore()

            when {

                existingPreferences != null &&
                        firstAuthenticationOnInstall -> {

                    if (
                        currentRoute !=
                        AppDestinations.Onboarding
                    ) {

                        navController.navigate(
                            AppDestinations.Onboarding
                        ) {

                            popUpTo(0) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    }
                }

                existingPreferences != null -> {

                    onboardingPreferences
                        .markAuthenticatedBefore()

                    onboardingPreferences
                        .clearPendingSelection()

                    if (
                        currentRoute !=
                        AppDestinations.Home
                    ) {

                        navController.navigate(
                            AppDestinations.Home
                        ) {

                            popUpTo(0) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    }
                }

                pendingSelection != null -> {

                    onboardingPreferences
                        .saveUserPreferences(
                            uid =
                                currentUser.uid,
                            selection =
                                pendingSelection
                        )

                    onboardingPreferences
                        .markAuthenticatedBefore()

                    onboardingPreferences
                        .clearPendingSelection()

                    navController.navigate(
                        AppDestinations.Home
                    ) {

                        popUpTo(0) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }

                else -> {

                    if (
                        currentRoute !=
                        AppDestinations.Onboarding
                    ) {

                        navController.navigate(
                            AppDestinations.Onboarding
                        ) {

                            popUpTo(0) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    }
                }
            }
        }

        else if (
            currentRoute ==
            AppDestinations.Splash
        ) {

            val destination =
                if (
                    onboardingPreferences
                        .hasAuthenticatedBefore()
                ) {

                    AppDestinations.Login

                } else {

                    AppDestinations.Onboarding
                }

            navController.navigate(
                destination
            ) {

                popUpTo(
                    AppDestinations.Splash
                ) {
                    inclusive = true
                }

                launchSingleTop = true
            }
        }
    }

    AppScaffold(
        navController =
            navController
    ) { innerPadding ->

        NavHost(
            navController =
                navController,
            startDestination =
                AppDestinations.Splash,
            modifier =
                Modifier.padding(
                    innerPadding
                )
        ) {

            composable(
                AppDestinations.Splash
            ) {

                SessionLoadingScreen()
            }

            composable(
                AppDestinations.Onboarding
            ) {

                val currentUser =
                    authState.user

                val existingUserPreferences =
                    currentUser?.let {
                            user ->

                        onboardingPreferences
                            .getUserPreferences(
                                user.uid
                            )
                    }

                val initialSelection =
                    when {

                        existingUserPreferences != null ->
                            existingUserPreferences

                        currentUser == null ->
                            onboardingPreferences
                                .getPendingSelection()
                                ?: OnboardingSelection()

                        else ->
                            OnboardingSelection()
                    }

                val existingMessage =
                    if (
                        currentUser != null &&
                        existingUserPreferences != null
                    ) {

                        "We found dietary preferences already saved for this account. Review them below and continue to keep or update them."

                    } else {

                        null
                    }

                OnboardingScreen(
                    initialSelection =
                        initialSelection,

                    existingPreferencesMessage =
                        existingMessage,

                    onContinue = {
                            selection ->

                        if (
                            currentUser == null
                        ) {

                            onboardingPreferences
                                .savePendingSelection(
                                    selection
                                )

                            authViewModel
                                .clearFeedback()

                            navController.navigate(
                                AppDestinations.Register
                            )
                        }

                        else {

                            onboardingPreferences
                                .saveUserPreferences(
                                    uid =
                                        currentUser.uid,
                                    selection =
                                        selection
                                )

                            onboardingPreferences
                                .markAuthenticatedBefore()

                            onboardingPreferences
                                .clearPendingSelection()

                            navController.navigate(
                                AppDestinations.Home
                            ) {

                                popUpTo(0) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }
                        }
                    },

                    onSkip = {

                        if (
                            currentUser == null
                        ) {

                            onboardingPreferences
                                .savePendingSelection(
                                    OnboardingSelection()
                                )

                            authViewModel
                                .clearFeedback()

                            navController.navigate(
                                AppDestinations.Register
                            )
                        }

                        else {

                            if (
                                existingUserPreferences ==
                                null
                            ) {

                                onboardingPreferences
                                    .saveUserPreferences(
                                        uid =
                                            currentUser.uid,
                                        selection =
                                            OnboardingSelection()
                                    )
                            }

                            onboardingPreferences
                                .markAuthenticatedBefore()

                            onboardingPreferences
                                .clearPendingSelection()

                            navController.navigate(
                                AppDestinations.Home
                            ) {

                                popUpTo(0) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(
                AppDestinations.Login
            ) {

                LoginScreen(

                    onLogin = {
                            email,
                            password ->

                        authViewModel.login(
                            email =
                                email,
                            password =
                                password
                        )
                    },

                    onGoogleSignIn = {

                        if (
                            activity == null
                        ) {

                            authViewModel
                                .googleSignInFailed(
                                    "Google sign-in is unavailable."
                                )
                        }

                        else {

                            authViewModel
                                .startGoogleSignIn()

                            coroutineScope.launch {

                                try {

                                    val idToken =
                                        googleSignInClient
                                            .getGoogleIdToken(
                                                activity
                                            )

                                    authViewModel
                                        .signInWithGoogle(
                                            idToken
                                        )

                                } catch (
                                    error:
                                    GetCredentialCancellationException
                                ) {

                                    authViewModel
                                        .googleSignInFailed(
                                            "Google sign-in was cancelled."
                                        )

                                } catch (
                                    error: Exception
                                ) {

                                    authViewModel
                                        .googleSignInFailed(
                                            "Google sign-in failed. Please try again."
                                        )
                                }
                            }
                        }
                    },

                    onForgotPassword = {

                        authViewModel
                            .clearFeedback()

                        navController.navigate(
                            AppDestinations
                                .ForgotPassword
                        )
                    },

                    onCreateAccount = {

                        authViewModel
                            .clearFeedback()

                        navController.navigate(
                            AppDestinations.Register
                        )
                    },

                    onEditDietaryPreferences =
                        if (
                            !onboardingPreferences
                                .hasAuthenticatedBefore()
                        ) {
                            {

                                /*
                                 * Normally Onboarding is
                                 * already behind Login in
                                 * the back stack.
                                 */
                                val returnedToOnboarding =
                                    navController
                                        .popBackStack(
                                            AppDestinations
                                                .Onboarding,
                                            false
                                        )

                                /*
                                 * Fallback in case it is
                                 * not currently present.
                                 */
                                if (
                                    !returnedToOnboarding
                                ) {

                                    navController.navigate(
                                        AppDestinations
                                            .Onboarding
                                    ) {
                                        launchSingleTop =
                                            true
                                    }
                                }
                            }
                        } else {
                            null
                        },

                    isLoading =
                        authState.isLoading,

                    errorMessage =
                        authState.errorMessage
                )
            }

            composable(
                AppDestinations.Register
            ) {

                RegisterScreen(

                    onRegister = {
                            displayName,
                            email,
                            password ->

                        authViewModel.register(
                            displayName =
                                displayName,
                            email =
                                email,
                            password =
                                password
                        )
                    },

                    onBackToLogin = {

                        authViewModel
                            .clearFeedback()

                        navController.navigate(
                            AppDestinations.Login
                        )
                    },

                    onEditDietaryPreferences =
                        if (
                            !onboardingPreferences
                                .hasAuthenticatedBefore()
                        ) {
                            {

                                val returnedToOnboarding =
                                    navController
                                        .popBackStack(
                                            AppDestinations
                                                .Onboarding,
                                            false
                                        )

                                if (
                                    !returnedToOnboarding
                                ) {

                                    navController.navigate(
                                        AppDestinations
                                            .Onboarding
                                    ) {
                                        launchSingleTop =
                                            true
                                    }
                                }
                            }
                        } else {
                            null
                        },

                    isLoading =
                        authState.isLoading,

                    errorMessage =
                        authState.errorMessage
                )
            }

            composable(
                AppDestinations.ForgotPassword
            ) {

                ForgotPasswordScreen(

                    onSendResetLink = {
                            email ->

                        authViewModel
                            .sendPasswordReset(
                                email
                            )
                    },

                    onBackToLogin = {

                        authViewModel
                            .clearFeedback()

                        navController.navigate(
                            AppDestinations.Login
                        )
                    },

                    isLoading =
                        authState.isLoading,

                    resetRequested =
                        authState
                            .passwordResetSent,

                    errorMessage =
                        authState.errorMessage
                )
            }

            composable(
                AppDestinations.Home
            ) {

                PlaceholderScreen(
                    title = "Home"
                )
            }

            composable(
                AppDestinations.Recipes
            ) {

                PlaceholderScreen(
                    title = "Recipes"
                )
            }

            composable(
                AppDestinations.MyKitchen
            ) {

                PlaceholderScreen(
                    title = "My Kitchen"
                )
            }

            composable(
                AppDestinations.Cook
            ) {

                PlaceholderScreen(
                    title = "Cook"
                )
            }

            composable(
                AppDestinations.Profile
            ) {

                PlaceholderScreen(
                    title = "Profile",
                    actionText =
                        "Settings",
                    onAction = {

                        navController.navigate(
                            AppDestinations.Settings
                        )
                    }
                )
            }

            composable(
                AppDestinations.WhatCanIMake
            ) {

                PlaceholderScreen(
                    title =
                        "What Can I Make?"
                )
            }

            composable(
                AppDestinations.RecipeDetails
            ) {

                PlaceholderScreen(
                    title =
                        "Recipe Details"
                )
            }

            composable(
                AppDestinations.SavedRecipes
            ) {

                PlaceholderScreen(
                    title =
                        "Saved Recipes"
                )
            }

            composable(
                AppDestinations
                    .IngredientDetails
            ) {

                PlaceholderScreen(
                    title =
                        "Ingredient Details"
                )
            }

            composable(
                AppDestinations
                    .IngredientEditor
            ) {

                PlaceholderScreen(
                    title =
                        "Ingredient Editor"
                )
            }

            composable(
                AppDestinations.ShoppingList
            ) {

                PlaceholderScreen(
                    title =
                        "Shopping List"
                )
            }

            composable(
                AppDestinations.CookingMode
            ) {

                PlaceholderScreen(
                    title =
                        "Cooking Mode"
                )
            }

            composable(
                AppDestinations.ActiveTimers
            ) {

                PlaceholderScreen(
                    title =
                        "Active Timers"
                )
            }

            composable(
                AppDestinations.KitchenTimer
            ) {

                PlaceholderScreen(
                    title =
                        "Kitchen Timer"
                )
            }

            composable(
                AppDestinations.RecipeComplete
            ) {

                PlaceholderScreen(
                    title =
                        "Recipe Complete"
                )
            }

            composable(
                AppDestinations.CookingHistory
            ) {

                PlaceholderScreen(
                    title =
                        "Cooking History"
                )
            }

            composable(
                AppDestinations.Settings
            ) {

                AccountScreen(
                    user =
                        authState.user,

                    onSignOut = {

                        authViewModel
                            .signOut()

                        navController.navigate(
                            AppDestinations.Login
                        ) {

                            popUpTo(0) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }

                        coroutineScope.launch {

                            try {

                                googleSignInClient
                                    .clearCredentialState(
                                        context
                                    )

                            } catch (
                                error: Exception
                            ) {

                                /*
                                 * Firebase has already
                                 * signed out successfully.
                                 */
                            }
                        }
                    }
                )
            }
        }
    }
}