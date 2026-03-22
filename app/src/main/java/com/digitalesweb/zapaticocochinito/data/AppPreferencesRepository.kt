package com.digitalesweb.zapaticocochinito.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.digitalesweb.zapaticocochinito.model.AchievementKey
import com.digitalesweb.zapaticocochinito.model.AppLanguage
import com.digitalesweb.zapaticocochinito.model.AppSettings
import com.digitalesweb.zapaticocochinito.model.AppTheme
import com.digitalesweb.zapaticocochinito.model.CambiaChaosLevel
import com.digitalesweb.zapaticocochinito.model.Difficulty
import com.digitalesweb.zapaticocochinito.model.PendingAchievementEntry
import com.digitalesweb.zapaticocochinito.model.PendingAchievementType
import com.digitalesweb.zapaticocochinito.model.RatingPromptState
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "zapatico_settings"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class AppPreferencesRepository(private val context: Context) {

    private val logTag = "AppPreferencesRepo"

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
                    ?: AppSettings.DEFAULT_LANGUAGE,
                cambiaChaosLevel = preferences[CAMBIA_CHAOS_KEY]?.let { CambiaChaosLevel.valueOrDefault(it) }
                    ?: AppSettings.DEFAULT_CAMBIA_CHAOS_LEVEL
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

    val ratingPromptFlow: Flow<RatingPromptState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            RatingPromptState(
                disabled = preferences[RATING_DISABLED_KEY] ?: false,
                remindAfterMillis = preferences[RATING_REMIND_AFTER_KEY] ?: 0L
            )
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
        Log.d(logTag, "Idioma guardado en DataStore: ${language.tag}")
    }

    suspend fun updateCambiaChaos(level: CambiaChaosLevel) {
        context.dataStore.edit { preferences ->
            preferences[CAMBIA_CHAOS_KEY] = level.name
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

    suspend fun disableReviewPrompt() {
        context.dataStore.edit { preferences ->
            preferences[RATING_DISABLED_KEY] = true
        }
    }

    suspend fun scheduleReviewReminder(remindAfterMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[RATING_DISABLED_KEY] = false
            preferences[RATING_REMIND_AFTER_KEY] = remindAfterMillis
        }
    }

    suspend fun isAchievementUnlockSent(achievement: AchievementKey): Boolean {
        return preferencesSnapshot()[ACHIEVEMENT_UNLOCKS_SENT_KEY]
            ?.contains(achievement.name)
            ?: false
    }

    suspend fun markAchievementUnlockSent(achievement: AchievementKey) {
        context.dataStore.edit { preferences ->
            val current = preferences[ACHIEVEMENT_UNLOCKS_SENT_KEY].orEmpty().toMutableSet()
            current.add(achievement.name)
            preferences[ACHIEVEMENT_UNLOCKS_SENT_KEY] = current
        }
    }

    suspend fun getAchievementIncrementSent(achievement: AchievementKey): Int {
        val encoded = preferencesSnapshot()[ACHIEVEMENT_INCREMENT_SENT_KEY]
        return decodeIncrementSent(encoded)[achievement.name] ?: 0
    }

    suspend fun setAchievementIncrementSent(achievement: AchievementKey, sentProgress: Int) {
        context.dataStore.edit { preferences ->
            val current = decodeIncrementSent(preferences[ACHIEVEMENT_INCREMENT_SENT_KEY]).toMutableMap()
            current[achievement.name] = sentProgress.coerceAtLeast(0)
            preferences[ACHIEVEMENT_INCREMENT_SENT_KEY] = encodeIncrementSent(current)
        }
    }

    suspend fun getPendingAchievementQueue(): List<PendingAchievementEntry> {
        val encoded = preferencesSnapshot()[ACHIEVEMENT_PENDING_QUEUE_KEY]
        return decodePendingQueue(encoded)
    }

    suspend fun savePendingAchievementQueue(queue: List<PendingAchievementEntry>) {
        context.dataStore.edit { preferences ->
            preferences[ACHIEVEMENT_PENDING_QUEUE_KEY] = encodePendingQueue(queue)
        }
    }

    private suspend fun preferencesSnapshot(): Preferences = context.dataStore.data.first()

    private fun decodeIncrementSent(encoded: String?): Map<String, Int> {
        if (encoded.isNullOrBlank()) return emptyMap()
        return encoded.split(";")
            .mapNotNull { token ->
                val separator = token.indexOf('=')
                if (separator <= 0 || separator >= token.length - 1) {
                    null
                } else {
                    val key = token.substring(0, separator)
                    val value = token.substring(separator + 1).toIntOrNull()
                    if (value == null) {
                        Log.w(logTag, "Registro incremental invalido para key=$key")
                        null
                    } else {
                        key to value
                    }
                }
            }
            .toMap()
    }

    private fun encodeIncrementSent(values: Map<String, Int>): String {
        return values.entries
            .filter { it.value >= 0 }
            .sortedBy { it.key }
            .joinToString(separator = ";") { "${it.key}=${it.value}" }
    }

    private fun encodePendingQueue(queue: List<PendingAchievementEntry>): String {
        return queue.joinToString(separator = "\n") { entry ->
            listOf(
                entry.type.name,
                entry.achievement.name,
                entry.targetProgress.toString(),
                entry.attemptCount.toString(),
                entry.nextAttemptAtMillis.toString(),
                entry.createdAtMillis.toString()
            ).joinToString(separator = "|")
        }
    }

    private fun decodePendingQueue(encoded: String?): List<PendingAchievementEntry> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.lineSequence()
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 6) {
                    Log.w(logTag, "Entrada de cola invalida: $line")
                    return@mapNotNull null
                }
                val type = runCatching { PendingAchievementType.valueOf(parts[0]) }.getOrNull()
                val key = runCatching { AchievementKey.valueOf(parts[1]) }.getOrNull()
                val target = parts[2].toIntOrNull()
                val attempts = parts[3].toIntOrNull()
                val nextAt = parts[4].toLongOrNull()
                val createdAt = parts[5].toLongOrNull()
                if (type == null || key == null || target == null || attempts == null || nextAt == null || createdAt == null) {
                    Log.w(logTag, "No se pudo parsear entrada de cola: $line")
                    null
                } else {
                    PendingAchievementEntry(
                        type = type,
                        achievement = key,
                        targetProgress = target,
                        attemptCount = attempts,
                        nextAttemptAtMillis = nextAt,
                        createdAtMillis = createdAt
                    )
                }
            }
            .toList()
    }

    private companion object {
        val DIFFICULTY_KEY = stringPreferencesKey("difficulty")
        val VOLUME_KEY = floatPreferencesKey("volume")
        val METRONOME_KEY = booleanPreferencesKey("metronome")
        val THEME_KEY = stringPreferencesKey("theme")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val HIGH_SCORE_KEY = intPreferencesKey("high_score")
        val CAMBIA_CHAOS_KEY = stringPreferencesKey("cambia_chaos")
        val RATING_DISABLED_KEY = booleanPreferencesKey("rating_disabled")
        val RATING_REMIND_AFTER_KEY = longPreferencesKey("rating_remind_after")
        val ACHIEVEMENT_UNLOCKS_SENT_KEY = stringSetPreferencesKey("achievement_unlocks_sent")
        val ACHIEVEMENT_INCREMENT_SENT_KEY = stringPreferencesKey("achievement_increment_sent")
        val ACHIEVEMENT_PENDING_QUEUE_KEY = stringPreferencesKey("achievement_pending_queue")

        fun emptyPreferences(): Preferences = androidx.datastore.preferences.core.emptyPreferences()
    }
}
