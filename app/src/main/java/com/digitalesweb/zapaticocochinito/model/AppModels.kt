package com.digitalesweb.zapaticocochinito.model

import androidx.annotation.StringRes
import com.digitalesweb.zapaticocochinito.R

data class AppSettings(
    val difficulty: Difficulty = DEFAULT_DIFFICULTY,
    val volume: Float = DEFAULT_VOLUME,
    val metronomeEnabled: Boolean = DEFAULT_METRONOME_ENABLED,
    val theme: AppTheme = DEFAULT_THEME,
    val language: AppLanguage = DEFAULT_LANGUAGE
) {
    companion object {
        val DEFAULT_DIFFICULTY = Difficulty.Normal
        const val DEFAULT_VOLUME = 0.7f
        const val DEFAULT_METRONOME_ENABLED = true
        val DEFAULT_THEME = AppTheme.Light
        val DEFAULT_LANGUAGE = AppLanguage.SpanishLatam
    }
}

enum class Difficulty(val bpm: Int, @StringRes val label: Int) {
    Kid(70, R.string.difficulty_kid),
    Normal(90, R.string.difficulty_normal),
    Pro(120, R.string.difficulty_pro);

    companion object {
        fun valueOrDefault(name: String): Difficulty = entries.find { it.name == name } ?: Normal
    }
}

enum class AppTheme(@StringRes val label: Int) {
    Light(R.string.theme_light),
    Dark(R.string.theme_dark);

    companion object {
        fun valueOrDefault(name: String): AppTheme = entries.find { it.name == name } ?: Light
    }
}

enum class AppLanguage(
    val tag: String,
    @StringRes val label: Int,
    private val fallbacks: List<String> = emptyList()
) {
    SpanishLatam(
        tag = "es-419",
        label = R.string.language_spanish_label,
        fallbacks = listOf("es")
    ),
    EnglishUs(
        tag = "en-US",
        label = R.string.language_english_label,
        fallbacks = listOf("en")
    );

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

data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val bestScore: Int = 0
)
