package com.digitalesweb.zapaticocochinito.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector
import com.digitalesweb.zapaticocochinito.R

object ZapaticoRoutes {
    const val HOME = "home"
    const val GAME = "game"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val GAME_OVER = "game_over"
}

data class BottomDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val label: Int
)

val bottomDestinations = listOf(
    BottomDestination(
        route = ZapaticoRoutes.HOME,
        icon = Icons.Outlined.Home,
        label = R.string.bottom_home
    ),
    BottomDestination(
        route = ZapaticoRoutes.NOTIFICATIONS,
        icon = Icons.Outlined.Notifications,
        label = R.string.bottom_notifications
    ),
    BottomDestination(
        route = ZapaticoRoutes.GAME,
        icon = Icons.Outlined.VideogameAsset,
        label = R.string.bottom_game
    )
)
