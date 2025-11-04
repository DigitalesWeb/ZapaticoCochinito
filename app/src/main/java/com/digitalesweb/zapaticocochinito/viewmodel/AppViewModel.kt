package com.digitalesweb.zapaticocochinito.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalesweb.zapaticocochinito.data.AppPreferencesRepository
import com.digitalesweb.zapaticocochinito.model.AppLanguage
import com.digitalesweb.zapaticocochinito.model.AppTheme
import com.digitalesweb.zapaticocochinito.model.AppUiState
import com.digitalesweb.zapaticocochinito.model.Difficulty
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val repository: AppPreferencesRepository) : ViewModel() {

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
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        viewModelScope.launch {
            repository.updateLanguage(language)
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
