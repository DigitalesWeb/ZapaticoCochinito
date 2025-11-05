package com.digitalesweb.zapaticocochinito.util

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.digitalesweb.zapaticocochinito.model.AppLanguage

private fun LocaleListCompat?.normalizedTags(): List<String> {
    val tags = this?.toLanguageTags()?.split(',') ?: emptyList()
    return tags
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.lowercase() }
}

fun AppLanguage.applyAppLocales(logTag: String) {
    val desiredLocales = LocaleListCompat.forLanguageTags(localeTags())
    val desiredTags = desiredLocales.normalizedTags()
    val currentTags = AppCompatDelegate.getApplicationLocales().normalizedTags()

    if (desiredTags == currentTags) {
        Log.d(logTag, "Locales ya aplicados. Se omite la reconfiguración")
        return
    }

    Log.d(logTag, "Aplicando locales ${'$'}desiredTags")
    AppCompatDelegate.setApplicationLocales(desiredLocales)
}
