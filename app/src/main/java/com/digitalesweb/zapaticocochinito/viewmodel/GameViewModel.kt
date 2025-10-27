package com.digitalesweb.zapaticocochinito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalesweb.zapaticocochinito.data.AppPreferencesRepository
import com.digitalesweb.zapaticocochinito.model.AppSettings
import com.digitalesweb.zapaticocochinito.model.GamePrompt
import com.digitalesweb.zapaticocochinito.model.GameUiState
import com.digitalesweb.zapaticocochinito.model.Foot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameViewModel(private val repository: AppPreferencesRepository) : ViewModel() {

    private val mutableState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = mutableState.asStateFlow()

    private var currentSettings: AppSettings = AppSettings()
    private var random = Random(System.currentTimeMillis())
    private var beatsSinceLastCambia = 0
    private var invertBeatsRemaining = 0
    private var cambiaAnnounceBeats = 0

    init {
        viewModelScope.launch {
            repository.highScoreFlow.collect { highScore ->
                mutableState.update { state ->
                    state.copy(bestScore = max(state.bestScore, highScore))
                }
            }
        }
    }

    fun applySettings(settings: AppSettings) {
        currentSettings = settings
        mutableState.update { state ->
            state.copy(
                baseBpm = settings.difficulty.bpm,
                currentBpm = if (state.isRunning) state.currentBpm else settings.difficulty.bpm,
                metronomeEnabled = settings.metronomeEnabled,
                volume = settings.volume
            )
        }
    }

    fun startGame() {
        beatsSinceLastCambia = 0
        invertBeatsRemaining = 0
        cambiaAnnounceBeats = 0
        mutableState.update {
            it.copy(
                currentPrompt = GamePrompt.Left,
                expectedFoot = Foot.Left,
                showCambia = false,
                invertActive = false,
                score = 0,
                lives = GameUiState.MAX_LIVES,
                isRunning = true,
                isGameOver = false,
                beat = 0,
                currentBpm = currentSettings.difficulty.bpm,
            )
        }
    }

    fun stopGame() {
        mutableState.update {
            it.copy(isRunning = false)
        }
    }

    fun resetGame() {
        beatsSinceLastCambia = 0
        invertBeatsRemaining = 0
        cambiaAnnounceBeats = 0
        mutableState.update {
            it.copy(
                currentPrompt = GamePrompt.Left,
                expectedFoot = Foot.Left,
                showCambia = false,
                invertActive = false,
                score = 0,
                lives = GameUiState.MAX_LIVES,
                isRunning = false,
                isGameOver = false,
                beat = 0,
                currentBpm = currentSettings.difficulty.bpm
            )
        }
    }

    fun onBeat() {
        val state = mutableState.value
        if (!state.isRunning || state.isGameOver) return

        val nextBeat = state.beat + 1
        val shouldTriggerCambia = shouldTriggerCambia()
        var nextInvertActive = state.invertActive
        if (shouldTriggerCambia) {
            nextInvertActive = !state.invertActive
            invertBeatsRemaining = CAMBIA_DURATION_BEATS + 1
            cambiaAnnounceBeats = CAMBIA_ANNOUNCE_BEATS
            beatsSinceLastCambia = 0
        } else {
            beatsSinceLastCambia++
        }

        if (invertBeatsRemaining > 0) {
            invertBeatsRemaining--
            if (invertBeatsRemaining == 0) {
                nextInvertActive = false
            }
        }

        val showCambiaNow = shouldTriggerCambia || cambiaAnnounceBeats > 0
        val baseFoot = if (random.nextBoolean()) Foot.Left else Foot.Right

        val expectedFoot = if (nextInvertActive) baseFoot.flipped() else baseFoot
        val prompt = if (expectedFoot == Foot.Left) GamePrompt.Left else GamePrompt.Right

        if (cambiaAnnounceBeats > 0) {
            cambiaAnnounceBeats--
        }

        mutableState.update { current ->
            val acceleratedBpm = accelerateBpm(current.score)
            current.copy(
                currentPrompt = prompt,
                expectedFoot = expectedFoot,
                showCambia = showCambiaNow,
                invertActive = nextInvertActive,
                beat = nextBeat,
                currentBpm = acceleratedBpm
            )
        }
    }

    fun onFootPressed(foot: Foot) {
        val state = mutableState.value
        if (!state.isRunning || state.isGameOver) return

        val wasCorrect = foot == state.expectedFoot
        if (wasCorrect) {
            val newScore = state.score + 1
            val newBest = max(state.bestScore, newScore)
            mutableState.update {
                it.copy(
                    score = newScore,
                    bestScore = newBest,
                    currentBpm = accelerateBpm(newScore)
                )
            }
            if (newBest > state.bestScore) {
                viewModelScope.launch {
                    repository.updateHighScore(newBest)
                }
            }
        } else {
            val newLives = state.lives - 1
            if (newLives <= 0) {
                mutableState.update {
                    it.copy(
                        lives = 0,
                        lastScore = it.score,
                        isRunning = false,
                        isGameOver = true,
                        gameOverEventId = it.gameOverEventId + 1
                    )
                }
                viewModelScope.launch {
                    repository.updateHighScore(state.score)
                }
            } else {
                mutableState.update {
                    it.copy(lives = newLives)
                }
            }
        }
    }

    private fun shouldTriggerCambia(): Boolean {
        if (invertBeatsRemaining > 0) return false
        if (beatsSinceLastCambia < MIN_BEATS_BEFORE_CAMBIA) return false
        return random.nextFloat() < CAMBIA_TRIGGER_PROBABILITY
    }

    private fun accelerateBpm(score: Int): Int {
        val base = currentSettings.difficulty.bpm
        if (score <= 0) return base
        val additional = (score / SCORE_PER_BPM_STEP) * BPM_INCREMENT
        return min(base + additional, MAX_BPM)
    }

    class Factory(private val repository: AppPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                return GameViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val BPM_INCREMENT = 4
        private const val SCORE_PER_BPM_STEP = 6
        private const val MAX_BPM = 200
        private const val CAMBIA_DURATION_BEATS = 6
        private const val CAMBIA_ANNOUNCE_BEATS = 2
        private const val MIN_BEATS_BEFORE_CAMBIA = 6
        private const val CAMBIA_TRIGGER_PROBABILITY = 0.22f
    }
}
