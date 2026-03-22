package com.digitalesweb.zapaticocochinito.model

import androidx.annotation.StringRes
import com.digitalesweb.zapaticocochinito.R

data class AppSettings(
    val difficulty: Difficulty = DEFAULT_DIFFICULTY,
    val volume: Float = DEFAULT_VOLUME,
    val metronomeEnabled: Boolean = DEFAULT_METRONOME_ENABLED,
    val theme: AppTheme = DEFAULT_THEME,
    val language: AppLanguage = DEFAULT_LANGUAGE,
    val cambiaChaosLevel: CambiaChaosLevel = DEFAULT_CAMBIA_CHAOS_LEVEL
) {
    companion object {
        val DEFAULT_DIFFICULTY = Difficulty.Normal
        const val DEFAULT_VOLUME = 0.7f
        const val DEFAULT_METRONOME_ENABLED = true
        val DEFAULT_THEME = AppTheme.Light
        val DEFAULT_LANGUAGE = AppLanguage.SpanishLatam
        val DEFAULT_CAMBIA_CHAOS_LEVEL = CambiaChaosLevel.Standard
    }
}

enum class Difficulty(val bpm: Int, @field:StringRes val label: Int) {
    Kid(70, R.string.difficulty_kid),
    Normal(90, R.string.difficulty_normal),
    Pro(120, R.string.difficulty_pro);

    companion object {
        fun valueOrDefault(name: String): Difficulty = entries.find { it.name == name } ?: Normal
    }
}

enum class AppTheme(@field:StringRes val label: Int) {
    Light(R.string.theme_light),
    Dark(R.string.theme_dark);

    companion object {
        fun valueOrDefault(name: String): AppTheme = entries.find { it.name == name } ?: Light
    }
}

enum class CambiaChaosLevel(
    val probabilityMultiplier: Float,
    val durationMultiplier: Float,
    @field:StringRes val title: Int,
    @field:StringRes val description: Int
) {
    Relaxed(
        probabilityMultiplier = 0.6f,
        durationMultiplier = 0.8f,
        title = R.string.settings_cambia_level_relaxed,
        description = R.string.settings_cambia_level_relaxed_description
    ),
    Standard(
        probabilityMultiplier = 1f,
        durationMultiplier = 1f,
        title = R.string.settings_cambia_level_standard,
        description = R.string.settings_cambia_level_standard_description
    ),
    Frenzy(
        probabilityMultiplier = 1.45f,
        durationMultiplier = 1.25f,
        title = R.string.settings_cambia_level_frenzy,
        description = R.string.settings_cambia_level_frenzy_description
    );

    companion object {
        fun valueOrDefault(name: String): CambiaChaosLevel = entries.find { it.name == name } ?: Standard
    }
}

enum class AppLanguage(
    val tag: String,
    @field:StringRes val label: Int,
    val resourceQualifier: String,
    private val fallbacks: List<String> = emptyList()
) {
    SpanishLatam(
        tag = "es-419",
        label = R.string.language_spanish_label,
        resourceQualifier = "values-b+es+419",
        fallbacks = listOf("es")
    ),
    EnglishUs(
        tag = "en-US",
        label = R.string.language_english_label,
        resourceQualifier = "values-en-rUS",
        fallbacks = listOf("en")
    );

    val resourceFilePath: String
        get() = "res/" + resourceQualifier + "/strings.xml"

    fun localeTags(): String = (listOf(tag) + fallbacks)
        .distinctBy { it.lowercase() }
        .joinToString(separator = ",")

    private fun matchesTag(value: String): Boolean {
        if (value.equals(tag, ignoreCase = true)) return true
        return fallbacks.any { it.equals(value, ignoreCase = true) }
    }

    companion object {
        fun fromTag(tag: String): AppLanguage = entries.find { it.matchesTag(tag) } ?: SpanishLatam
    }
}

enum class Foot {
    Left,
    Right;

    fun flipped(): Foot = if (this == Left) Right else Left
}

enum class GamePrompt {
    Left,
    Right
}

enum class AchievementKey {
    ACH_PRIMERA_PARTIDA,
    ACH_SCORE_50,
    ACH_SCORE_100,
    ACH_RACHA_15,
    ACH_CAMBIA_ACEPTADO,
    ACH_PRO_100,
    ACH_BPM_180,
    ACH_PARTIDAS_25,
    ACH_ACIERTOS_200,
    ACH_CAMBIA_50,
    ACH_MAESTRO_300
}

sealed interface GameAchievementEvent {
    data class Unlock(val achievement: AchievementKey) : GameAchievementEvent

    data class Increment(
        val achievement: AchievementKey,
        val amount: Int = 1
    ) : GameAchievementEvent
}

enum class PendingAchievementType {
    Unlock,
    IncrementTarget
}

data class PendingAchievementEntry(
    val type: PendingAchievementType,
    val achievement: AchievementKey,
    val targetProgress: Int = 0,
    val attemptCount: Int = 0,
    val nextAttemptAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L
)

data class GameUiState(
    val currentPrompt: GamePrompt = GamePrompt.Left,
    val expectedFoot: Foot = Foot.Left,
    val showCambia: Boolean = false,
    val invertActive: Boolean = false,
    val score: Int = 0,
    val bestScore: Int = 0,
    val lives: Int = MAX_LIVES,
    val lastScore: Int = 0,
    val isRunning: Boolean = false,
    val isGameOver: Boolean = false,
    val gameOverEventId: Int = 0,
    val beat: Long = 0L,
    val baseBpm: Int = Difficulty.Normal.bpm,
    val currentBpm: Int = Difficulty.Normal.bpm,
    val metronomeEnabled: Boolean = AppSettings.DEFAULT_METRONOME_ENABLED,
    val volume: Float = AppSettings.DEFAULT_VOLUME
) {
    companion object {
        const val MAX_LIVES = 3
    }
}

data class RatingPromptState(
    val disabled: Boolean = false,
    val remindAfterMillis: Long = 0L
) {
    fun canShow(nowMillis: Long): Boolean = !disabled && nowMillis >= remindAfterMillis
}

data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val bestScore: Int = 0,
    val ratingPrompt: RatingPromptState = RatingPromptState()
)
