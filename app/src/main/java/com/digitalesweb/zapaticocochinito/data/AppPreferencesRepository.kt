package com.digitalesweb.zapaticocochinito.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.digitalesweb.zapaticocochinito.model.AppLanguage
import com.digitalesweb.zapaticocochinito.model.AppSettings
import com.digitalesweb.zapaticocochinito.model.AppTheme
import com.digitalesweb.zapaticocochinito.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DATASTORE_NAME = "zapatico_settings"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class AppPreferencesRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                difficulty = preferences[DIFFICULTY_KEY]?.let { Difficulty.valueOrDefault(it) }
                    ?: AppSettings.DEFAULT_DIFFICULTY,
                volume = preferences[VOLUME_KEY] ?: AppSettings.DEFAULT_VOLUME,
                metronomeEnabled = preferences[METRONOME_KEY] ?: AppSettings.DEFAULT_METRONOME_ENABLED,
                theme = preferences[THEME_KEY]?.let { AppTheme.valueOrDefault(it) }
                    ?: AppSettings.DEFAULT_THEME,
                language = preferences[LANGUAGE_KEY]?.let { AppLanguage.fromTag(it) }
                    ?: AppSettings.DEFAULT_LANGUAGE
            )
        }

    val highScoreFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HIGH_SCORE_KEY] ?: 0
        }

    suspend fun updateDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { preferences ->
            preferences[DIFFICULTY_KEY] = difficulty.name
        }
    }

    suspend fun updateVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_KEY] = volume.coerceIn(0f, 1f)
        }
    }

    suspend fun updateMetronomeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[METRONOME_KEY] = enabled
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.tag
        }
    }

    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[HIGH_SCORE_KEY] ?: 0
            if (score > current) {
                preferences[HIGH_SCORE_KEY] = score
            }
        }
    }

    private companion object {
        val DIFFICULTY_KEY = stringPreferencesKey("difficulty")
        val VOLUME_KEY = floatPreferencesKey("volume")
        val METRONOME_KEY = booleanPreferencesKey("metronome")
        val THEME_KEY = stringPreferencesKey("theme")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val HIGH_SCORE_KEY = intPreferencesKey("high_score")

        fun emptyPreferences(): Preferences = androidx.datastore.preferences.core.emptyPreferences()
    }
}
