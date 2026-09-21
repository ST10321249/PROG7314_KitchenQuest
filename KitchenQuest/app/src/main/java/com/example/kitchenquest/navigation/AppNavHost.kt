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
import com.example.kitchenquest.data.user.DefaultUserRepository
import com.example.kitchenquest.data.user.syncSignedInUser
import com.example.kitchenquest.feature.auth.AuthViewModel
import com.example.kitchenquest.feature.auth.ForgotPasswordScreen
import com.example.kitchenquest.feature.auth.LoginScreen
import com.example.kitchenquest.feature.auth.RegisterScreen
import com.example.kitchenquest.feature.auth.SessionLoadingScreen
import com.example.kitchenquest.feature.onboarding.OnboardingScreen
import com.example.kitchenquest.feature.onboarding.OnboardingSelection
import com.example.kitchenquest.feature.settings.SettingsScreen
import com.example.kitchenquest.feature.settings.SettingsViewModel
import com.example.kitchenquest.ui.components.AppScaffold
import com.example.kitchenquest.ui.screens.PlaceholderScreen
import kotlinx.coroutines.launch
import com.example.kitchenquest.feature.pantry.PantryViewModel
import com.example.kitchenquest.feature.pantry.MyKitchenScreen
import com.example.kitchenquest.feature.pantry.IngredientEditorScreen
import com.example.kitchenquest.feature.shopping.ShoppingListScreen
import com.example.kitchenquest.feature.shopping.ShoppingViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.kitchenquest.feature.pantry.IngredientDetailScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
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

    val userRepository =
        remember {
            DefaultUserRepository()
        }

    val preferencesVersion by
    onboardingPreferences
        .userPreferencesVersion
        .collectAsState()

    // Keeps the server profile in step with the signed-in user and with the
    // choices saved on this device (onboarding or Settings).
    LaunchedEffect(
        authState.user?.uid,
        preferencesVersion
    ) {

        val user =
            authState.user
                ?: return@LaunchedEffect

        val localChoices =
            onboardingPreferences
                .getUserPreferences(
                    user.uid
                )
                ?: onboardingPreferences
                    .getPendingSelection()

        userRepository
            .syncSignedInUser(
                user,
                localChoices
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
            composable(AppDestinations.MyKitchen) {
                val pantryViewModel: PantryViewModel = viewModel()
                val pantryState by pantryViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { pantryViewModel.loadPantry() }

                MyKitchenScreen(
                    state = pantryState,
                    onAddIngredient = { navController.navigate(AppDestinations.ingredientEditorRoute()) },
                    onIngredientClick = { navController.navigate(AppDestinations.ingredientDetailsRoute(it.id)) },
                    onFindRecipes = { navController.navigate(AppDestinations.WhatCanIMake) },
                    onRetry = pantryViewModel::loadPantry
                )
            }

            composable(
                route = AppDestinations.IngredientDetails,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val pantryViewModel: PantryViewModel = viewModel()
                val itemId = backStackEntry.arguments?.getString("itemId")
                val item = itemId?.let { pantryViewModel.findItem(it) }

                if (item == null) {
                    // Pantry hasn't loaded into this ViewModel instance yet (e.g. deep link or process death).
                    LaunchedEffect(Unit) { pantryViewModel.loadPantry() }
                    CircularProgressIndicator()
                } else {
                    IngredientDetailScreen(
                        item = item,
                        onEdit = { navController.navigate(AppDestinations.ingredientEditorRoute(item.id)) },
                        onMarkFinished = {
                            pantryViewModel.markFinished(item.id)
                            navController.popBackStack()
                        },
                        onFindRecipes = { navController.navigate(AppDestinations.WhatCanIMake) }
                    )
                }
            }

            composable(
                route = AppDestinations.IngredientEditor,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val pantryViewModel: PantryViewModel = viewModel()
                val pantryState by pantryViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { pantryViewModel.loadPantry() }

                val itemId = backStackEntry.arguments?.getString("itemId")
                val existing = itemId?.let { pantryViewModel.findItem(it) }
                val stillLoadingExistingItem = itemId != null && existing == null && !pantryState.hasLoaded

                if (stillLoadingExistingItem) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    IngredientEditorScreen(
                        initialName = existing?.ingredientName ?: "",
                        initialQuantity = existing?.quantity?.toString() ?: "",
                        initialUnit = existing?.unit ?: "",
                        initialCategory = existing?.category ?: "",
                        initialExpiryDate = existing?.expiryDate,
                        isEditing = existing != null,
                        knownIngredients = pantryState.items,
                        onSave = { name, quantity, unit, category, expiryDate ->
                            if (existing != null) {
                                pantryViewModel.updateItem(existing.id, name, quantity, unit, category, expiryDate)
                            } else {
                                pantryViewModel.addItem(name, quantity, unit, category, expiryDate)
                            }
                            navController.popBackStack()
                        },
                        onDelete = existing?.let {
                            {
                                pantryViewModel.markFinished(it.id)
                                navController.popBackStack()
                            }
                        }
                    )
                }
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


            composable(AppDestinations.ShoppingList) {
                val shoppingViewModel: ShoppingViewModel = viewModel()
                val shoppingState by shoppingViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { shoppingViewModel.loadList() }

                ShoppingListScreen(
                    state = shoppingState,
                    onAdd = shoppingViewModel::addItem,
                    onTogglePurchased = shoppingViewModel::togglePurchased,
                    onRemove = shoppingViewModel::removeItem
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

                val settingsViewModel: SettingsViewModel =
                    viewModel()

                val settingsState by
                settingsViewModel
                    .uiState
                    .collectAsState()

                LaunchedEffect(Unit) {
                    settingsViewModel
                        .loadProfile()
                }

                // Keep this device's saved choices, which drive the app's
                // routing, in step with what was just saved to the server.
                LaunchedEffect(
                    settingsState.saveSucceeded
                ) {

                    val user =
                        authState.user

                    if (
                        settingsState.saveSucceeded &&
                        user != null
                    ) {

                        onboardingPreferences
                            .saveUserPreferences(
                                uid =
                                    user.uid,
                                selection =
                                    OnboardingSelection(
                                        dietaryPreferences =
                                            settingsState
                                                .dietaryPreferences,
                                        avoidedIngredients =
                                            settingsState
                                                .avoidedIngredients
                                    )
                            )
                    }
                }

                SettingsScreen(
                    state =
                        settingsState,

                    onBack = {
                        navController
                            .popBackStack()
                    },

                    onDisplayNameChange =
                        settingsViewModel::onDisplayNameChange,

                    onDietaryPreferencesChange =
                        settingsViewModel::onDietaryPreferencesChange,

                    onAvoidedIngredientsChange =
                        settingsViewModel::onAvoidedIngredientsChange,

                    onSave =
                        settingsViewModel::save,

                    onRetry =
                        settingsViewModel::loadProfile,

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
