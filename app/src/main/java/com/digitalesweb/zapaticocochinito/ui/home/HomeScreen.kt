package com.digitalesweb.zapaticocochinito.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalesweb.zapaticocochinito.R

@Composable
fun HomeScreen(
    bestScore: Int,
    onPlay: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        PulsingZapaticoIcon(
            modifier = Modifier.size(96.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.home_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        ScoreCard(bestScore = bestScore)
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(id = R.string.home_play),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.home_settings),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.home_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PulsingZapaticoIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "zapaticoPulse")
    val scale = transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zapaticoScale"
    )

    Canvas(modifier = modifier) {
        val shoeColor = color
        val accentColor = color.copy(alpha = 0.75f)
        val highlightColor = color.copy(alpha = 0.5f)

        val width = size.width * scale.value
        val height = size.height * scale.value
        val left = (size.width - width) / 2f
        val top = (size.height - height) / 2f

        translate(left, top) {
            val soleHeight = height * 0.25f
            val heelWidth = width * 0.18f

            val solePath = Path().apply {
                moveTo(heelWidth, height - soleHeight)
                lineTo(width - heelWidth * 0.5f, height - soleHeight)
                quadTo(width, height - soleHeight * 0.9f, width - heelWidth * 0.2f, height - soleHeight * 0.3f)
                quadTo(width * 0.85f, height, width * 0.55f, height)
                lineTo(heelWidth * 0.4f, height)
                quadTo(0f, height, heelWidth * 0.8f, height - soleHeight * 0.2f)
                quadTo(heelWidth * 0.2f, height - soleHeight * 0.6f, heelWidth, height - soleHeight)
                close()
            }

            drawPath(path = solePath, color = shoeColor)

            val upperPath = Path().apply {
                moveTo(heelWidth * 0.8f, height - soleHeight)
                cubicTo(
                    width * 0.1f,
                    height * 0.35f,
                    width * 0.55f,
                    height * 0.05f,
                    width * 0.9f,
                    height * 0.32f
                )
                quadTo(width * 0.98f, height * 0.4f, width * 0.92f, height - soleHeight * 1.4f)
                lineTo(width * 0.42f, height - soleHeight * 1.5f)
                quadTo(width * 0.28f, height - soleHeight * 1.25f, heelWidth * 0.8f, height - soleHeight)
                close()
            }

            drawPath(path = upperPath, color = accentColor)

            val laceCount = 3
            repeat(laceCount) { index ->
                val progress = index / (laceCount - 1f)
                val y = lerp(height - soleHeight * 1.45f, height - soleHeight * 0.9f, progress)
                val startX = lerp(width * 0.3f, width * 0.55f, progress)
                val endX = lerp(width * 0.55f, width * 0.78f, progress)
                drawLine(
                    color = highlightColor,
                    start = androidx.compose.ui.geometry.Offset(startX, y),
                    end = androidx.compose.ui.geometry.Offset(endX, y),
                    strokeWidth = height * 0.03f
                )
            }

            drawCircle(
                color = highlightColor,
                radius = heelWidth * 0.45f,
                center = androidx.compose.ui.geometry.Offset(heelWidth, height - soleHeight * 0.4f)
            )
        }
    }
}

@Composable
private fun ScoreCard(bestScore: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.home_best_score_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bestScore.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_best_score_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
