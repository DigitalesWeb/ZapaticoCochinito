package com.digitalesweb.zapaticocochinito.ui.game

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var hasStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.isRunning, uiState.isGameOver) {
        if (!hasStarted && !uiState.isGameOver) {
            onStart()
            hasStarted = true
        }
    }

    LaunchedEffect(uiState.gameOverEventId) {
        if (uiState.isGameOver) {
            hasStarted = false
        }
    }

    val currentState by rememberUpdatedState(uiState)
    LaunchedEffect(uiState.isRunning, uiState.currentBpm, uiState.isGameOver) {
        while (currentState.isRunning && !currentState.isGameOver) {
            onBeat()
            val bpm = currentState.currentBpm.coerceAtLeast(40)
            delay((60_000f / bpm).toLong())
        }
    }

    val colorPalette = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )
    val targetColor = colorPalette[(uiState.beat % colorPalette.size).toInt()]
    val animatedBackground by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "backgroundColor"
    )
    val gradient = Brush.verticalGradient(
        colors = listOf(animatedBackground, MaterialTheme.colorScheme.background)
    )

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(uiState.beat) {
        pulse.snapTo(1.08f)
        pulse.animateTo(1f, animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
    }

    val haptic = LocalHapticFeedback.current

    val toneGenerator = remember(uiState.volume, uiState.metronomeEnabled) {
        if (uiState.metronomeEnabled) {
            try {
                ToneGenerator(AudioManager.STREAM_MUSIC, (uiState.volume * 100).roundToInt().coerceIn(10, 100))
            } catch (_: Throwable) {
                null
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

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreHeader(uiState)
            Spacer(modifier = Modifier.height(16.dp))
            PromptDisplay(uiState = uiState, scale = pulse.value)
            Spacer(modifier = Modifier.height(32.dp))
            FootButtons(
                onLeft = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) // Corrected
                    onFootPressed(Foot.Left)
                },
                onRight = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) // Corrected
                    onFootPressed(Foot.Right)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ScoreHeader(uiState: GameUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.game_score, uiState.score),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.game_bpm, uiState.currentBpm),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.game_best, uiState.bestScore),
                style = MaterialTheme.typography.bodyLarge
            )
            LivesIndicator(lives = uiState.lives)
        }
    }
}

@Composable
private fun LivesIndicator(lives: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(GameUiState.MAX_LIVES) { index ->
            val tint = if (index < lives) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PromptDisplay(uiState: GameUiState, scale: Float, modifier: Modifier = Modifier) {
    val text = when {
        uiState.showCambia -> stringResource(id = R.string.game_prompt_cambia)
        uiState.currentPrompt == GamePrompt.Left -> stringResource(id = R.string.game_prompt_left)
        else -> stringResource(id = R.string.game_prompt_right)
    }
    val description = if (uiState.showCambia) {
        stringResource(id = R.string.game_prompt_cambia_subtitle)
    } else if (uiState.invertActive) {
        stringResource(id = R.string.game_prompt_inverted)
    } else {
        stringResource(id = R.string.game_prompt_follow)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
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
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FootButton(
            text = stringResource(id = R.string.game_button_left),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            onClick = onLeft,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        )
        FootButton(
            text = stringResource(id = R.string.game_button_right),
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            onClick = onRight,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        )
    }
}

@Composable
private fun FootButton(
    text: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color,
        shadowElevation = 12.dp,
        tonalElevation = 6.dp,
        onClick = onClick,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = contentColor
            )
        }
    }
}
