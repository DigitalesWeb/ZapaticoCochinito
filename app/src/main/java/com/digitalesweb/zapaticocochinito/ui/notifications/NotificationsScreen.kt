package com.digitalesweb.zapaticocochinito.ui.notifications

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalesweb.zapaticocochinito.R

@Composable
fun NotificationsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val highlights = remember {
        NotificationHighlight.defaults()
    }

    LazyColumn(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.notifications_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.notifications_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(highlights) { highlight ->
            NotificationCard(highlight = highlight)
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = CardDefaults.shape,
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.notifications_share_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.notifications_share_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Button(onClick = { shareApp(context) }) {
                        Text(text = stringResource(id = R.string.notifications_share_button))
                    }
                }
            }
        }
    }
}

private fun shareApp(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.notifications_share_subject))
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.notifications_share_message))
    }

    if (shareIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.notifications_share_title)
            )
        )
    }
}

private data class NotificationHighlight(
    val emoji: String,
    val tag: Int,
    val title: Int,
    val description: Int
) {
    companion object {
        fun defaults(): List<NotificationHighlight> = listOf(
            NotificationHighlight(
                emoji = "🔥",
                tag = R.string.notification_highlight_tag_daily,
                title = R.string.notification_highlight_daily_title,
                description = R.string.notification_highlight_daily_description
            ),
            NotificationHighlight(
                emoji = "🏆",
                tag = R.string.notification_highlight_tag_league,
                title = R.string.notification_highlight_league_title,
                description = R.string.notification_highlight_league_description
            ),
            NotificationHighlight(
                emoji = "🛍️",
                tag = R.string.notification_highlight_tag_style,
                title = R.string.notification_highlight_style_title,
                description = R.string.notification_highlight_style_description
            )
        )
    }
}

@Composable
private fun NotificationCard(highlight: NotificationHighlight, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = highlight.emoji, fontSize = MaterialTheme.typography.displayMedium.fontSize)
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = CardDefaults.shape
                ) {
                    Text(
                        text = stringResource(id = highlight.tag),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = stringResource(id = highlight.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = highlight.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
