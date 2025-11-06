package com.digitalesweb.zapaticocochinito.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.digitalesweb.zapaticocochinito.R

@Composable
fun ReviewPromptDialog(
    onRemindLater: () -> Unit,
    onReject: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = stringResource(id = R.string.review_prompt_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.review_prompt_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        dismissButton = {
            TextButton(onClick = onRemindLater) {
                Text(text = stringResource(id = R.string.review_prompt_later))
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.review_prompt_no))
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.review_prompt_yes))
                }
            }
        }
    )
}
