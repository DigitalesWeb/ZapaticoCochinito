package com.digitalesweb.zapaticocochinito

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.digitalesweb.zapaticocochinito.di.ServiceLocator
import com.digitalesweb.zapaticocochinito.games.PlayGamesService
import com.digitalesweb.zapaticocochinito.model.AppLanguage
import com.digitalesweb.zapaticocochinito.model.AppTheme
import com.digitalesweb.zapaticocochinito.model.Foot
import com.digitalesweb.zapaticocochinito.ui.game.GameOverScreen
import com.digitalesweb.zapaticocochinito.ui.game.GameScreen
import com.digitalesweb.zapaticocochinito.ui.game.ReviewPromptDialog
import com.digitalesweb.zapaticocochinito.ui.home.HomeScreen
import com.digitalesweb.zapaticocochinito.ui.navigation.ZapaticoRoutes
import com.digitalesweb.zapaticocochinito.ui.navigation.bottomDestinations
import com.digitalesweb.zapaticocochinito.ui.notifications.NotificationsScreen
import com.digitalesweb.zapaticocochinito.ui.settings.SettingsScreen
import com.digitalesweb.zapaticocochinito.ui.theme.ZapaticoCochinitoTheme
import com.digitalesweb.zapaticocochinito.util.applyAppLocales
import com.digitalesweb.zapaticocochinito.viewmodel.AppViewModel
import com.digitalesweb.zapaticocochinito.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private lateinit var playGamesService: PlayGamesService

    private val logTag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        playGamesService = PlayGamesService(this)
        val repository = ServiceLocator.provideAppPreferencesRepository(this)
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(repository))
            val appState by appViewModel.uiState.collectAsStateWithLifecycle()
            val gameViewModel: GameViewModel = viewModel(factory = GameViewModel.Factory(repository))
            val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(appState.settings) {
                gameViewModel.applySettings(appState.settings)
            }

            LaunchedEffect(appState.settings.language) {
                Log.d(logTag, "Reaplicando idioma desde ajustes: ${'$'}{appState.settings.language.tag}")
                appState.settings.language.applyAppLocales(logTag)
                Log.d(logTag, "Locales verificados en actividad")
            }

            ZapaticoCochinitoTheme(darkTheme = appState.settings.theme == AppTheme.Dark) {
                ZapaticoApp(
                    appState = appState,
                    gameState = gameState,
                    onStartGame = { gameViewModel.startGame() },
                    onFootPressed = { foot -> gameViewModel.onFootPressed(foot) },
                    onBeat = { gameViewModel.onBeat() },
                    onPauseGame = { gameViewModel.stopGame() },
                    onDifficultyChange = appViewModel::updateDifficulty,
                    onVolumeChange = appViewModel::updateVolume,
                    onMetronomeToggle = appViewModel::updateMetronome,
                    onThemeChange = appViewModel::updateTheme,
                    onLanguageChange = appViewModel::updateLanguage,
                    onRestartGame = { gameViewModel.startGame() },
                    onResetGameState = { gameViewModel.resetGame() },
                    onBestScoreUpdated = { score -> playGamesService.submitBestScore(score) },
                    onCambiaChaosChange = appViewModel::updateCambiaChaos,
                    onShowLeaderboard = { playGamesService.showLeaderboard() },
                    onRateApp = {
                        appViewModel.disableReviewPrompt()
                        openPlayStoreReview()
                    },
                    onReviewLater = appViewModel::remindReviewLater,
                    onReviewDeclined = appViewModel::disableReviewPrompt,
                    onReviewAccepted = {
                        appViewModel.disableReviewPrompt()
                        openPlayStoreReview()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playGamesService.signInIfNeeded()
    }
}

@Composable
private fun ZapaticoApp(
    appState: com.digitalesweb.zapaticocochinito.model.AppUiState,
    gameState: com.digitalesweb.zapaticocochinito.model.GameUiState,
    onStartGame: () -> Unit,
    onFootPressed: (Foot) -> Unit,
    onBeat: () -> Unit,
    onPauseGame: () -> Unit,
    onDifficultyChange: (com.digitalesweb.zapaticocochinito.model.Difficulty) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMetronomeToggle: (Boolean) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onRestartGame: () -> Unit,
    onResetGameState: () -> Unit,
    onBestScoreUpdated: (Int) -> Unit,
    onCambiaChaosChange: (com.digitalesweb.zapaticocochinito.model.CambiaChaosLevel) -> Unit,
    onShowLeaderboard: () -> Unit,
    onRateApp: () -> Unit,
    onReviewLater: () -> Unit,
    onReviewDeclined: () -> Unit,
    onReviewAccepted: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var showReviewPrompt by remember { mutableStateOf(false) }
    var lastPromptEventId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(gameState.gameOverEventId) {
        if (gameState.isGameOver) {
            if (navController.currentDestination?.route != ZapaticoRoutes.GAME_OVER) {
                navController.navigate(ZapaticoRoutes.GAME_OVER)
            }
        }
    }

    LaunchedEffect(gameState.bestScore) {
        if (gameState.bestScore > 0) {
            onBestScoreUpdated(gameState.bestScore)
        }
    }

    LaunchedEffect(gameState.gameOverEventId, appState.ratingPrompt, gameState.isGameOver) {
        val eventId = gameState.gameOverEventId
        val shouldShow = gameState.isGameOver &&
            gameState.lastScore > REVIEW_SCORE_THRESHOLD &&
            appState.ratingPrompt.canShow(System.currentTimeMillis())

        if (shouldShow) {
            if (lastPromptEventId != eventId) {
                showReviewPrompt = true
                lastPromptEventId = eventId
            }
        } else {
            showReviewPrompt = false
            if (!gameState.isGameOver) {
                lastPromptEventId = null
            }
        }
    }

    Scaffold(
        bottomBar = {
            val destinationsWithBottomBar = setOf(
                ZapaticoRoutes.HOME,
                ZapaticoRoutes.NOTIFICATIONS
            )
            if (currentDestination?.route in destinationsWithBottomBar) {
                NavigationBar(
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                ) {
                    bottomDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(text = stringResource(destination.label)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ZapaticoRoutes.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(ZapaticoRoutes.HOME) {
                HomeScreen(
                    bestScore = appState.bestScore,
                    onPlay = {
                        navController.navigate(ZapaticoRoutes.GAME)
                    },
                    onOpenSettings = {
                        navController.navigate(ZapaticoRoutes.SETTINGS)
                    },
                    onShowLeaderboard = onShowLeaderboard
                )
            }
            composable(ZapaticoRoutes.GAME) {
                GameScreen(
                    uiState = gameState,
                    onStart = onStartGame,
                    onBeat = onBeat,
                    onFootPressed = onFootPressed,
                    onPause = onPauseGame,
                    onExit = {
                        onPauseGame()
                        navController.navigate(ZapaticoRoutes.HOME)
                    }

                )
            }
            composable(ZapaticoRoutes.NOTIFICATIONS) {
                NotificationsScreen()
            }
            composable(ZapaticoRoutes.SETTINGS) {
                SettingsScreen(
                    settings = appState.settings,
                    bestScore = appState.bestScore,
                    onDifficultyChange = onDifficultyChange,
                    onVolumeChange = onVolumeChange,
                    onMetronomeToggle = onMetronomeToggle,
                    onThemeChange = onThemeChange,
                    onLanguageChange = onLanguageChange,
                    onCambiaChaosChange = onCambiaChaosChange,
                    onRateApp = onRateApp,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ZapaticoRoutes.GAME_OVER) {
                GameOverScreen(
                    score = gameState.lastScore,
                    bestScore = gameState.bestScore,
                    onPlayAgain = {
                        onRestartGame()
                        navController.navigate(ZapaticoRoutes.GAME) {
                            popUpTo(ZapaticoRoutes.HOME)
                        }
                    },
                    onBackHome = {
                        onResetGameState()
                        navController.popBackStack(ZapaticoRoutes.HOME, false)
                    },
                    onShowLeaderboard = onShowLeaderboard
                )
            }
        }

        if (showReviewPrompt) {
            val currentEventId = gameState.gameOverEventId
            ReviewPromptDialog(
                onRemindLater = {
                    showReviewPrompt = false
                    lastPromptEventId = currentEventId
                    onReviewLater()
                },
                onReject = {
                    showReviewPrompt = false
                    lastPromptEventId = currentEventId
                    onReviewDeclined()
                },
                onAccept = {
                    showReviewPrompt = false
                    lastPromptEventId = currentEventId
                    onReviewAccepted()
                }
            )
        }
    }
}

private fun MainActivity.openPlayStoreReview() {
    val appPackage = packageName
    val reviewUri = Uri.parse("market://details?id=$appPackage")
    val reviewIntent = Intent(Intent.ACTION_VIEW, reviewUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
    try {
        startActivity(reviewIntent)
    } catch (exception: ActivityNotFoundException) {
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$appPackage")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        startActivity(webIntent)
    }
}

private const val REVIEW_SCORE_THRESHOLD = 50
