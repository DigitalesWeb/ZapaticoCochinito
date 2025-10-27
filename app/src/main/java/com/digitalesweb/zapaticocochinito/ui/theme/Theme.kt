package com.digitalesweb.zapaticocochinito.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Midnight,
    secondary = CoralPink,
    onSecondary = Midnight,
    tertiary = MintGreen,
    onTertiary = Midnight,
    background = Midnight,
    onBackground = SoftWhite,
    surface = Midnight,
    onSurface = SoftWhite,
    primaryContainer = DeepNavy,
    onPrimaryContainer = SoftWhite,
    secondaryContainer = PeachGlow,
    onSecondaryContainer = Midnight
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = DeepNavy,
    secondary = CoralPink,
    onSecondary = Midnight,
    tertiary = MintGreen,
    onTertiary = DeepNavy,
    background = SoftWhite,
    onBackground = Midnight,
    surface = SoftWhite,
    onSurface = Midnight,
    primaryContainer = SoftYellow,
    onPrimaryContainer = Midnight,
    secondaryContainer = PeachGlow,
    onSecondaryContainer = Midnight
)

@Composable
fun ZapaticoCochinitoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}