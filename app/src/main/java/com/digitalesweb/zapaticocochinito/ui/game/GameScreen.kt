package com.digitalesweb.zapaticocochinito.ui.game

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.digitalesweb.zapaticocochinito.R
import com.digitalesweb.zapaticocochinito.model.Foot
import com.digitalesweb.zapaticocochinito.model.GamePrompt
import com.digitalesweb.zapaticocochinito.model.GameUiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    uiState: GameUiState,
    onStart: () -> Unit,
    onBeat: () -> Unit,
    onFootPressed: (Foot) -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                onPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentState by rememberUpdatedState(uiState)
    LaunchedEffect(uiState.isRunning, uiState.currentBpm, uiState.gameOverEventId) {
        while (currentState.isRunning && !currentState.isGameOver) {
            onBeat()
            val bpm = currentState.currentBpm.coerceAtLeast(40)
            delay((60_000f / bpm).toLong())
        }
    }

    val toneGenerator = remember(uiState.volume, uiState.metronomeEnabled) {
        if (uiState.metronomeEnabled) {
            val volumeLevel = (uiState.volume * 100).roundToInt().coerceIn(0, 100)
            if (volumeLevel == 0) {
                null
            } else {
                try {
                    ToneGenerator(
                        AudioManager.STREAM_MUSIC,
                        volumeLevel
                    )
                } catch (_: Throwable) {
                    null
                }
            }
        } else {
            null
        }
    }
    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator?.release() }
    }

    LaunchedEffect(uiState.beat) {
        if (uiState.metronomeEnabled && uiState.isRunning && !uiState.isGameOver) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background,
                lerp(colorScheme.background, colorScheme.surfaceVariant, 0.35f),
                colorScheme.surface
            )
        )
    }

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(uiState.beat) {
        if (uiState.isRunning && !uiState.isGameOver) {
            pulse.snapTo(1.08f)
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
    }

    val haptic = LocalHapticFeedback.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val showStartCard = !uiState.isRunning && !uiState.isGameOver

//    BackHandler {
//        onPause()
//        onExit()
//    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameTopBar(
                lives = uiState.lives,
                score = uiState.score,
                onExit = {
                    onPause()
                    onExit()
                    //backDispatcher?.onBackPressed()
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center
            ) {
                if (showStartCard) {
                    StartGameCard(onStart = onStart)
                } else {
                    PromptDisplay(
                        uiState = uiState,
                        scale = pulse.value
                    )
                }
            }
            if (uiState.isRunning) {
                Spacer(modifier = Modifier.height(24.dp))
                BpmBadge(bpm = uiState.currentBpm)
                Spacer(modifier = Modifier.height(32.dp))
                FootButtons(
                    onLeft = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFootPressed(Foot.Left)
                    },
                    onRight = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFootPressed(Foot.Right)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GameTopBar(lives: Int, score: Int, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onExit,
            shape = CircleShape,
            color = colorScheme.surfaceVariant.copy(alpha = 0.9f),
            tonalElevation = 2.dp,
            shadowElevation = 6.dp
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(id = R.string.game_exit),
                tint = colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
        LivesIndicator(lives = lives)
        ScoreBadge(score = score)
    }
}

@Composable
private fun LivesIndicator(lives: Int, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(GameUiState.MAX_LIVES) { index ->
            val tint = if (index < lives) {
                colorScheme.secondary
            } else {
                colorScheme.outline
            }
            Surface(
                shape = CircleShape,
                color = colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 1.dp
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun PromptDisplay(uiState: GameUiState, scale: Float, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = if (uiState.showCambia) {
        colorScheme.secondary
    } else {
        colorScheme.primary
    }
    val promptText = when {
        uiState.showCambia -> stringResource(id = R.string.game_prompt_cambia)
        uiState.currentPrompt == GamePrompt.Left -> stringResource(id = R.string.game_prompt_left)
        else -> stringResource(id = R.string.game_prompt_right)
    }
    val helperText = when {
        uiState.showCambia -> stringResource(id = R.string.game_prompt_cambia_subtitle)
        uiState.invertActive -> stringResource(id = R.string.game_prompt_inverted)
        else -> stringResource(id = R.string.game_prompt_follow)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = colorScheme.surface,
            shadowElevation = 18.dp,
            tonalElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 36.dp, vertical = 42.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = promptText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BpmBadge(bpm: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Text(
            text = stringResource(id = R.string.game_bpm, bpm),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun FootButtons(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        FootButton(
            label = stringResource(id = R.string.game_button_left),
            emoji = "👈",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onLeft,
            modifier = Modifier.weight(1f)
        )
        FootButton(
            label = stringResource(id = R.string.game_button_right),
            emoji = "👉",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onRight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FootButton(
    label: String,
    emoji: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(130.dp),
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 36.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun StartGameCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(id = R.string.game_icon_text), fontSize = 44.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.game_ready_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.game_ready_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
