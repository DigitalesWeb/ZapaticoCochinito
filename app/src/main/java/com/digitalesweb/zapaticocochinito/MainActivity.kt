package com.digitalesweb.zapaticocochinito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.digitalesweb.zapaticocochinito.model.AppTheme
import com.digitalesweb.zapaticocochinito.model.Foot
import com.digitalesweb.zapaticocochinito.ui.game.GameOverScreen
import com.digitalesweb.zapaticocochinito.ui.game.GameScreen
import com.digitalesweb.zapaticocochinito.ui.home.HomeScreen
import com.digitalesweb.zapaticocochinito.ui.navigation.ZapaticoRoutes
import com.digitalesweb.zapaticocochinito.ui.navigation.bottomDestinations
import com.digitalesweb.zapaticocochinito.ui.notifications.NotificationsScreen
import com.digitalesweb.zapaticocochinito.ui.settings.SettingsScreen
import com.digitalesweb.zapaticocochinito.ui.theme.ZapaticoCochinitoTheme
import com.digitalesweb.zapaticocochinito.viewmodel.AppViewModel
import com.digitalesweb.zapaticocochinito.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = ServiceLocator.provideAppPreferencesRepository(this)
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(repository))
            val appState by appViewModel.uiState.collectAsStateWithLifecycle()
            val gameViewModel: GameViewModel = viewModel(factory = GameViewModel.Factory(repository))
            val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(appState.settings) {
                gameViewModel.applySettings(appState.settings)
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
                    onRestartGame = { gameViewModel.startGame() },
                    onResetGameState = { gameViewModel.resetGame() }
                )
            }
        }
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
    onRestartGame: () -> Unit,
    onResetGameState: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(gameState.gameOverEventId) {
        if (gameState.isGameOver) {
            if (navController.currentDestination?.route != ZapaticoRoutes.GAME_OVER) {
                navController.navigate(ZapaticoRoutes.GAME_OVER)
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
                NavigationBar {
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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ZapaticoRoutes.HOME) {
                HomeScreen(
                    bestScore = appState.bestScore,
                    onPlay = {
                        navController.navigate(ZapaticoRoutes.GAME)
                    },
                    onOpenSettings = {
                        navController.navigate(ZapaticoRoutes.SETTINGS)
                    }
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
                    }
                )
            }
        }
    }
}
