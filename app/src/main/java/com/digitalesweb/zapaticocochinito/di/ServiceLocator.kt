package com.digitalesweb.zapaticocochinito.di

import android.content.Context
import com.digitalesweb.zapaticocochinito.data.AppPreferencesRepository

object ServiceLocator {
    @Volatile
    private var repository: AppPreferencesRepository? = null

    fun provideAppPreferencesRepository(context: Context): AppPreferencesRepository {
        return repository ?: synchronized(this) {
            repository ?: AppPreferencesRepository(context.applicationContext).also {
                repository = it
            }
        }
    }
}
