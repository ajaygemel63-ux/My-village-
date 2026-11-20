package com.example.localization

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(name = "village_prefs")

class LanguagePreferences(private val context: Context) {
    private val langKey = stringPreferencesKey("app_language")

    val languageFlow: Flow<AppLanguage> = context.languageDataStore.data.map { prefs ->
        AppLanguage.fromStored(prefs[langKey])
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { prefs ->
            prefs[langKey] = language.name
        }
    }
}
