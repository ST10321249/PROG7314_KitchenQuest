package com.example.kitchenquest.navigation

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import com.example.kitchenquest.feature.recipes.RecipeDetailViewModel
import com.example.kitchenquest.feature.recipes.RecipeDetailScreen
import com.example.kitchenquest.feature.recipes.WhatCanIMakeViewModel
import com.example.kitchenquest.feature.recipes.WhatCanIMakeScreen
import com.example.kitchenquest.feature.recipes.SavedRecipesViewModel
import com.example.kitchenquest.feature.recipes.SavedRecipesScreen
import com.example.kitchenquest.feature.cooking.CookingViewModel
import com.example.kitchenquest.feature.cooking.CookScreen
import com.example.kitchenquest.feature.cooking.CookingModeScreen
import com.example.kitchenquest.feature.cooking.ActiveTimersScreen
import com.example.kitchenquest.feature.cooking.KitchenTimerScreen
import com.example.kitchenquest.feature.cooking.RecipeCompleteScreen
import com.example.kitchenquest.feature.cooking.CookingHistoryScreen
import com.example.kitchenquest.feature.cooking.CookingHistoryViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.kitchenquest.data.user.toOnboardingSelection
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
import com.example.kitchenquest.feature.recipes.RecipesScreen
import com.example.kitchenquest.feature.recipes.RecipesViewModel
import com.example.kitchenquest.feature.home.HomeScreen
import com.example.kitchenquest.feature.home.HomeViewModel
import com.example.kitchenquest.feature.notifications.NotificationsScreen
import com.example.kitchenquest.feature.profile.ProfileScreen
import com.example.kitchenquest.feature.profile.ProfileViewModel

@Composable


fun AppNavHost() {

    val navController =
        rememberNavController()

    val authViewModel: AuthViewModel =
        viewModel()

    val cookingViewModel: CookingViewModel =
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

    var showExistingPreferencesMessage by
    remember {
        mutableStateOf(false)
    }

    // Authentication and profile synchronisation are handled together so the
    // server can decide whether this is a new or returning KitchenQuest user
    // before navigation continues. Existing server preferences always win.
    LaunchedEffect(
        authState.isAuthChecked,
        authState.user?.uid
    ) {

        if (!authState.isAuthChecked) {
            return@LaunchedEffect
        }

        val currentRoute =
            navController
                .currentDestination
                ?.route

        val currentUser =
            authState.user

        if (currentUser == null) {
            showExistingPreferencesMessage = false

            if (currentRoute == AppDestinations.Splash) {
                val destination =
                    if (
                        onboardingPreferences
                            .hasAuthenticatedBefore()
                    ) {
                        AppDestinations.Login
                    } else {
                        AppDestinations.Onboarding
                    }

                navController.navigate(destination) {
                    popUpTo(AppDestinations.Splash) {
                        inclusive = true
                    }

                    launchSingleTop = true
                }
            }

            return@LaunchedEffect
        }

        val firstAuthenticationOnInstall =
            !onboardingPreferences
                .hasAuthenticatedBefore()

        val localChoices =
            onboardingPreferences
                .getUserPreferences(currentUser.uid)
                ?: onboardingPreferences
                    .getPendingSelection()

        userRepository
            .syncSignedInUser(
                user = currentUser,
                local = localChoices
            )
            .fold(
                onSuccess = { syncResult ->
                    val serverSelection =
                        syncResult.profile
                            .toOnboardingSelection()

                    // The server is the source of truth for an existing user.
                    // For a new user, the server profile was created from the
                    // onboarding choices supplied above.
                    onboardingPreferences
                        .saveUserPreferences(
                            uid = currentUser.uid,
                            selection = serverSelection
                        )

                    showExistingPreferencesMessage =
                        !syncResult.created &&
                                firstAuthenticationOnInstall

                    when {
                        // A returning account on a fresh installation should
                        // review the preferences recovered from the server.
                        !syncResult.created &&
                                firstAuthenticationOnInstall -> {
                            navController.navigate(
                                AppDestinations.Onboarding
                            ) {
                                popUpTo(0) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }
                        }

                        // A genuinely new account that did not pass through
                        // onboarding first still needs to choose preferences.
                        syncResult.created &&
                                localChoices == null -> {
                            navController.navigate(
                                AppDestinations.Onboarding
                            ) {
                                popUpTo(0) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }
                        }

                        else -> {
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
                },
                onFailure = { failure ->
                    showExistingPreferencesMessage = false

                    Toast
                        .makeText(
                            context,
                            failure.message
                                ?: "Unable to sync your profile. Please try again.",
                            Toast.LENGTH_LONG
                        )
                        .show()

                    // Keep the app usable if the profile request temporarily
                    // fails. No local value is pushed to the server here.
                    val fallbackDestination =
                        if (localChoices != null) {
                            AppDestinations.Home
                        } else {
                            AppDestinations.Onboarding
                        }

                    navController.navigate(fallbackDestination) {
                        popUpTo(0) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
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
                        showExistingPreferencesMessage
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
                            coroutineScope.launch {
                                userRepository
                                    .updateProfile(
                                        dietaryPreferences =
                                            selection
                                                .dietaryPreferences
                                                .toList(),
                                        avoidedIngredients =
                                            selection
                                                .avoidedIngredients
                                                .toList()
                                    )
                                    .fold(
                                        onSuccess = { profile ->
                                            onboardingPreferences
                                                .saveUserPreferences(
                                                    uid = currentUser.uid,
                                                    selection =
                                                        profile
                                                            .toOnboardingSelection()
                                                )

                                            onboardingPreferences
                                                .markAuthenticatedBefore()

                                            onboardingPreferences
                                                .clearPendingSelection()

                                            showExistingPreferencesMessage = false

                                            navController.navigate(
                                                AppDestinations.Home
                                            ) {
                                                popUpTo(0) {
                                                    inclusive = true
                                                }

                                                launchSingleTop = true
                                            }
                                        },
                                        onFailure = { failure ->
                                            Toast
                                                .makeText(
                                                    context,
                                                    failure.message
                                                        ?: "Unable to save your preferences. Please try again.",
                                                    Toast.LENGTH_LONG
                                                )
                                                .show()
                                        }
                                    )
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

                            showExistingPreferencesMessage = false

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

                val homeViewModel: HomeViewModel = viewModel()
                val homeState by homeViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    homeViewModel.load()
                }

                HomeScreen(
                    state = homeState,
                    onNotifications = {
                        navController.navigate(AppDestinations.Notifications)
                    },
                    onKitchenTimer = {
                        navController.navigate(AppDestinations.KitchenTimer)
                    },
                    onWhatCanIMake = {
                        navController.navigate(AppDestinations.WhatCanIMake)
                    },
                    onShoppingList = {
                        navController.navigate(AppDestinations.ShoppingList)
                    },
                    onRecipes = {
                        navController.navigate(AppDestinations.Recipes)
                    },
                    onRecipeClick = { recipeId ->
                        navController.navigate(AppDestinations.recipeDetailsRoute(recipeId))
                    },
                    onRetry = {
                        homeViewModel.load()
                    }
                )
            }

            composable(
                AppDestinations.Recipes
            ) {
                val recipesViewModel: RecipesViewModel = viewModel()
                val recipesState by recipesViewModel.uiState.collectAsState()

                RecipesScreen(
                    state = recipesState,
                    onQueryChange = recipesViewModel::onQueryChange,
                    onSearch = recipesViewModel::search,
                    onDietSelected = recipesViewModel::onDietSelected,
                    onCuisineSelected = recipesViewModel::onCuisineSelected,
                    onMaxReadyTimeSelected = recipesViewModel::onMaxReadyTimeSelected,
                    onWhatCanIMake = { navController.navigate(AppDestinations.WhatCanIMake) },
                    onSavedRecipes = { navController.navigate(AppDestinations.SavedRecipes) },
                    onRecipeClick = { recipe ->
                        navController.navigate(AppDestinations.recipeDetailsRoute(recipe.recipeSourceId))
                    }
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
                        onBack = { navController.popBackStack() },
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
                        onBack = { navController.popBackStack() },
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
                val cookHubState by cookingViewModel.uiState.collectAsState()

                CookScreen(
                    recipeTitle = cookHubState.recipe?.title,
                    activeTimerCount = cookHubState.timers.size,
                    onContinueCooking = {
                        navController.navigate(AppDestinations.CookingMode)
                    },
                    onKitchenTimer = {
                        navController.navigate(AppDestinations.KitchenTimer)
                    },
                    onActiveTimers = {
                        navController.navigate(AppDestinations.ActiveTimers)
                    },
                    onCookingHistory = {
                        navController.navigate(AppDestinations.CookingHistory)
                    }
                )
            }

            composable(
                AppDestinations.Profile
            ) {

                val profileViewModel: ProfileViewModel = viewModel()
                val profileState by profileViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    profileViewModel.load()
                }

                ProfileScreen(
                    state = profileState,
                    onSettings = {
                        navController.navigate(AppDestinations.Settings)
                    },
                    onCookingHistory = {
                        navController.navigate(AppDestinations.CookingHistory)
                    },
                    onRetry = {
                        profileViewModel.load()
                    }
                )
            }

            composable(
                AppDestinations.WhatCanIMake
            ) {
                val whatCanIMakeViewModel: WhatCanIMakeViewModel = viewModel()
                val whatCanIMakeState by whatCanIMakeViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { whatCanIMakeViewModel.load() }

                WhatCanIMakeScreen(
                    state = whatCanIMakeState,
                    onBack = { navController.popBackStack() },
                    onRecipeClick = { recommendation ->
                        navController.navigate(AppDestinations.recipeDetailsRoute(recommendation.recipeSourceId))
                    },
                    onAddMissingToShoppingList = whatCanIMakeViewModel::addMissingToShoppingList,
                    onRetry = whatCanIMakeViewModel::load
                )
            }

            composable(
                route = AppDestinations.RecipeDetails,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                val detailViewModel: RecipeDetailViewModel = viewModel()
                val detailState by detailViewModel.uiState.collectAsState()

                LaunchedEffect(recipeId) { detailViewModel.load(recipeId) }

                RecipeDetailScreen(
                    state = detailState,
                    onBack = { navController.popBackStack() },
                    onIncreaseServings = detailViewModel::increaseServings,
                    onDecreaseServings = detailViewModel::decreaseServings,
                    onAddMissingToList = detailViewModel::addMissingIngredientsToList,
                    onToggleFavourite = detailViewModel::toggleFavourite,
                    onStartCooking = {
                        cookingViewModel.loadRecipe(recipeId)
                        navController.navigate(AppDestinations.CookingMode)
                    },
                    onRetry = { detailViewModel.load(recipeId) }
                )
            }

            composable(
                AppDestinations.SavedRecipes
            ) {
                val savedRecipesViewModel: SavedRecipesViewModel = viewModel()
                val savedRecipesState by savedRecipesViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { savedRecipesViewModel.load() }

                SavedRecipesScreen(
                    state = savedRecipesState,
                    onBack = { navController.popBackStack() },
                    onQueryChange = savedRecipesViewModel::onQueryChange,
                    onRecipeClick = { favourite ->
                        navController.navigate(AppDestinations.recipeDetailsRoute(favourite.recipeSourceId))
                    },
                    onRemove = { favourite -> savedRecipesViewModel.removeFavourite(favourite.recipeSourceId) },
                    onRetry = savedRecipesViewModel::load
                )
            }


            composable(AppDestinations.ShoppingList) {
                val shoppingViewModel: ShoppingViewModel = viewModel()
                val shoppingState by shoppingViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { shoppingViewModel.loadList() }

                ShoppingListScreen(
                    state = shoppingState,
                    onBack = { navController.popBackStack() },
                    onAdd = shoppingViewModel::addItem,
                    onTogglePurchased = shoppingViewModel::togglePurchased,
                    onRemove = shoppingViewModel::removeItem
                )
            }

            composable(
                AppDestinations.CookingMode
            ) {
                val cookingState by cookingViewModel.uiState.collectAsState()

                CookingModeScreen(
                    state = cookingState,
                    onExit = { navController.popBackStack() },
                    onNextStep = cookingViewModel::nextStep,
                    onPreviousStep = cookingViewModel::previousStep,
                    onStartStepTimer = cookingViewModel::startTimerForCurrentStep,
                    onViewAllTimers = { navController.navigate(AppDestinations.ActiveTimers) },
                    onFinish = { navController.navigate(AppDestinations.RecipeComplete) }
                )
            }

            composable(
                AppDestinations.ActiveTimers
            ) {
                val cookingState by cookingViewModel.uiState.collectAsState()

                ActiveTimersScreen(
                    state = cookingState,
                    onBack = { navController.popBackStack() },
                    onAddMinute = cookingViewModel::addMinuteToTimer,
                    onTogglePause = cookingViewModel::togglePauseTimer,
                    onCancel = cookingViewModel::cancelTimer,
                    onStopAll = cookingViewModel::stopAllTimers,
                    onBackToCooking = { navController.popBackStack() }
                )
            }

            composable(
                AppDestinations.KitchenTimer
            ) {
                KitchenTimerScreen(
                    onBack = { navController.popBackStack() },
                    onStartTimer = { minutes, label ->
                        cookingViewModel.startStandaloneTimer(minutes, label)
                        navController.navigate(AppDestinations.ActiveTimers)
                    }
                )
            }

            composable(
                AppDestinations.RecipeComplete
            ) {
                val cookingState by cookingViewModel.uiState.collectAsState()
                val recipeTitle = cookingState.recipe?.title ?: "Recipe"

                RecipeCompleteScreen(
                    recipeTitle = recipeTitle,
                    onBack = { navController.popBackStack() },
                    onSave = { rating, difficulty, note ->
                        cookingViewModel.completeCooking(rating, difficulty, note) {
                            navController.navigate(AppDestinations.Home) {
                                popUpTo(AppDestinations.Home) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(
                AppDestinations.CookingHistory
            ) {
                val cookingHistoryViewModel: CookingHistoryViewModel = viewModel()
                val cookingHistoryState by cookingHistoryViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) { cookingHistoryViewModel.load() }

                CookingHistoryScreen(
                    state = cookingHistoryState,
                    onBack = { navController.popBackStack() },
                    onRecipeClick = { recipeSourceId ->
                        navController.navigate(AppDestinations.recipeDetailsRoute(recipeSourceId))
                    },
                    onRetry = cookingHistoryViewModel::load
                )
            }

            composable(
                AppDestinations.Notifications
            ) {

                NotificationsScreen(
                    onBack = { navController.popBackStack() }
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


                            }
                        }
                    }
                )
            }
        }
    }
}