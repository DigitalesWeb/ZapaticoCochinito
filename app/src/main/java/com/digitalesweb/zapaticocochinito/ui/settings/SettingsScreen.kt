package com.digitalesweb.zapaticocochinito.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalesweb.zapaticocochinito.R
import com.digitalesweb.zapaticocochinito.model.AppSettings
import com.digitalesweb.zapaticocochinito.model.AppTheme
import com.digitalesweb.zapaticocochinito.model.Difficulty
import androidx.compose.foundation.layout.width // <-- Add this line

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onDifficultyChange: (Difficulty) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMetronomeToggle: (Boolean) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(id = R.string.settings_back)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.settings_difficulty_label),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            DifficultySelector(selected = settings.difficulty, onSelect = onDifficultyChange)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.settings_volume_label),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f
            )
            Text(
                text = stringResource(id = R.string.settings_volume_value, (settings.volume * 100).toInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.settings_metronome_label),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(id = R.string.settings_metronome_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.metronomeEnabled,
                    onCheckedChange = onMetronomeToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.settings_theme_label),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeSelector(selected = settings.theme, onSelect = onThemeChange)
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(id = R.string.settings_back),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DifficultySelector(selected: Difficulty, onSelect: (Difficulty) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        Difficulty.entries.forEachIndexed { index, difficulty ->
            SegmentedButton(
                selected = difficulty == selected,
                onClick = { onSelect(difficulty) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = Difficulty.entries.size)
            ) {
                Text(text = stringResource(id = difficulty.label))
            }
        }
    }
}

@Composable
private fun ThemeSelector(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        AppTheme.entries.forEachIndexed { index, theme ->
            SegmentedButton(
                selected = selected == theme,
                onClick = { onSelect(theme) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AppTheme.entries.size)
            ) {
                Text(text = stringResource(id = theme.label))
            }
        }
    }
}
