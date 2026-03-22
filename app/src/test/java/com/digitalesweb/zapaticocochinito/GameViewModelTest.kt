package com.digitalesweb.zapaticocochinito

import com.digitalesweb.zapaticocochinito.model.AppSettings
import com.digitalesweb.zapaticocochinito.model.Difficulty
import com.digitalesweb.zapaticocochinito.model.Foot
import com.digitalesweb.zapaticocochinito.model.GamePrompt
import com.digitalesweb.zapaticocochinito.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun toqueIncorrecto_pierdeUnaVida() {
        val viewModel = createViewModel()

        viewModel.startGame()
        viewModel.onBeat()

        val expected = viewModel.uiState.value.expectedFoot
        viewModel.onFootPressed(expected.flipped())

        assertEquals(2, viewModel.uiState.value.lives)
    }

    @Test
    fun timeoutSinToque_pierdeUnaVida() {
        val viewModel = createViewModel()

        viewModel.startGame()
        viewModel.onBeat()
        viewModel.onBeat()

        assertEquals(2, viewModel.uiState.value.lives)
    }

    @Test
    fun acierto_sumaPuntaje() {
        val viewModel = createViewModel()

        viewModel.startGame()
        viewModel.onBeat()
        val expected = viewModel.uiState.value.expectedFoot
        viewModel.onFootPressed(expected)

        assertEquals(10, viewModel.uiState.value.score)
    }

    @Test
    fun inputSinBeatPendiente_noAlteraEstado() {
        val viewModel = createViewModel()

        viewModel.startGame()
        viewModel.onFootPressed(Foot.Left)

        val state = viewModel.uiState.value
        assertEquals(0, state.score)
        assertEquals(3, state.lives)
        assertEquals(0, state.beat)
    }

    @Test
    fun bpmProgresaSegunDificultadYReglasActuales() {
        val viewModel = createViewModel()
        viewModel.applySettings(AppSettings(difficulty = Difficulty.Pro))

        viewModel.startGame()
        repeat(6) {
            viewModel.onBeat()
            val expected = viewModel.uiState.value.expectedFoot
            viewModel.onFootPressed(expected)
        }

        val state = viewModel.uiState.value
        assertEquals(60, state.score)
        assertEquals(124, state.currentBpm)
    }

    @Test
    fun cambia_sigueComportamientoActualDeInversion() {
        val random = PredictableRandom(
            booleans = ArrayDeque(List(16) { true }),
            floats = ArrayDeque(listOf(0f))
        )
        val viewModel = createViewModel(random)

        viewModel.startGame()
        repeat(6) {
            viewModel.onBeat()
            viewModel.onFootPressed(viewModel.uiState.value.expectedFoot)
        }

        viewModel.onBeat()

        val state = viewModel.uiState.value
        assertTrue(state.showCambia)
        assertTrue(state.invertActive)
        assertEquals(Foot.Right, state.expectedFoot)
        assertEquals(GamePrompt.Right, state.currentPrompt)
    }

    @Test
    fun cambia_noSeActivaAntesDelMinimoDeBeats() {
        val random = PredictableRandom(
            booleans = ArrayDeque(List(10) { true }),
            floats = ArrayDeque(listOf(0f))
        )
        val viewModel = createViewModel(random)

        viewModel.startGame()
        repeat(6) {
            viewModel.onBeat()
            viewModel.onFootPressed(viewModel.uiState.value.expectedFoot)
        }

        val state = viewModel.uiState.value
        assertFalse(state.invertActive)
        assertFalse(state.showCambia)
    }

    @Test
    fun bpm_seLimitaAlMaximo() {
        val viewModel = createViewModel()
        viewModel.applySettings(AppSettings(difficulty = Difficulty.Pro))

        viewModel.startGame()
        repeat(120) {
            viewModel.onBeat()
            viewModel.onFootPressed(viewModel.uiState.value.expectedFoot)
        }

        assertEquals(200, viewModel.uiState.value.currentBpm)
    }

    @Test
    fun tresTimeoutConsecutivos_finalizaPartidaConGameOver() {
        val viewModel = createViewModel()

        viewModel.startGame()
        repeat(4) {
            viewModel.onBeat()
        }

        val state = viewModel.uiState.value
        assertEquals(0, state.lives)
        assertTrue(state.isGameOver)
        assertFalse(state.isRunning)
        assertEquals(1, state.gameOverEventId)
        assertEquals(0, state.lastScore)
    }

    @Test
    fun tresErrores_finalizaPartidaConGameOver() {
        val viewModel = createViewModel()

        viewModel.startGame()
        repeat(3) {
            viewModel.onBeat()
            val expected = viewModel.uiState.value.expectedFoot
            viewModel.onFootPressed(expected.flipped())
        }

        val state = viewModel.uiState.value
        assertEquals(0, state.lives)
        assertTrue(state.isGameOver)
        assertFalse(state.isRunning)
        assertEquals(1, state.gameOverEventId)
        assertEquals(0, state.lastScore)
    }

    private fun createViewModel(random: Random = Random(0)): GameViewModel {
        val highScoreFlow = MutableStateFlow(0)
        return GameViewModel(
            highScoreFlow = highScoreFlow,
            persistHighScore = {},
            random = random
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private val dispatcher = StandardTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class PredictableRandom(
    private val booleans: ArrayDeque<Boolean>,
    private val floats: ArrayDeque<Float>
) : Random() {
    override fun nextBits(bitCount: Int): Int = 0

    override fun nextBoolean(): Boolean {
        return booleans.removeFirstOrNull()
            ?: throw AssertionError("PredictableRandom se quedó sin valores booleanos")
    }

    override fun nextFloat(): Float {
        return floats.removeFirstOrNull()
            ?: throw AssertionError("PredictableRandom se quedó sin valores float")
    }
}
