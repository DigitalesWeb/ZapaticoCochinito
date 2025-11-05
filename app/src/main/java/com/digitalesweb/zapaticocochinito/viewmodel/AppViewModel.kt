package com.digitalesweb.zapaticocochinito.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalesweb.zapaticocochinito.data.AppPreferencesRepository
import com.digitalesweb.zapaticocochinito.model.AppLanguage
import com.digitalesweb.zapaticocochinito.model.AppTheme
import com.digitalesweb.zapaticocochinito.model.AppUiState
import com.digitalesweb.zapaticocochinito.model.CambiaChaosLevel
import com.digitalesweb.zapaticocochinito.model.Difficulty
import com.digitalesweb.zapaticocochinito.util.applyAppLocales
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val repository: AppPreferencesRepository) : ViewModel() {

    private val logTag = "AppViewModel"

    val uiState = combine(
        repository.settingsFlow,
        repository.highScoreFlow
    ) { settings, highScore ->
        AppUiState(settings = settings, bestScore = highScore)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState()
    )

    fun updateDifficulty(difficulty: Difficulty) {
        viewModelScope.launch {
            repository.updateDifficulty(difficulty)
        }
    }

    fun updateVolume(volume: Float) {
        viewModelScope.launch {
            repository.updateVolume(volume)
        }
    }

    fun updateMetronome(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMetronomeEnabled(enabled)
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            repository.updateTheme(theme)
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            Log.d(logTag, "Actualizando idioma a ${'$'}{language.tag}")
            repository.updateLanguage(language)
            Log.d(logTag, "Persistencia completa. Aplicando locales")
            language.applyAppLocales(logTag)
            Log.d(logTag, "Locales aplicados correctamente")
        }
    }

    fun updateCambiaChaos(level: CambiaChaosLevel) {
        viewModelScope.launch {
            repository.updateCambiaChaos(level)
        }
    }

    class Factory(private val repository: AppPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                return AppViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
