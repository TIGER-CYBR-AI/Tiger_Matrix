package com.tigermatrix.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val themeKey = intPreferencesKey("theme_mode")
    private val speakKey = booleanPreferencesKey("speak_replies")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val callKey = intPreferencesKey("call_mode")
    private val smsKey = intPreferencesKey("sms_mode")
    private val waKey = intPreferencesKey("whatsapp_mode")
    private val waAppKey = intPreferencesKey("whatsapp_app")
    private val allowWaKey = booleanPreferencesKey("allow_whatsapp")
    private val allowBizKey = booleanPreferencesKey("allow_business")
    private val countryKey = stringPreferencesKey("country_code")

    /** 0 = حسب النظام، 1 = فاتح، 2 = داكن */
    val themeMode: Flow<Int> = context.dataStore.data.map { it[themeKey] ?: 0 }

    val speakReplies: Flow<Boolean> = context.dataStore.data.map { it[speakKey] ?: true }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[onboardedKey] ?: false }

    val actionSettings: Flow<ActionSettings> = context.dataStore.data.map { p ->
        ActionSettings(
            callMode = modeOf(p[callKey]),
            smsMode = modeOf(p[smsKey]),
            whatsappMode = modeOf(p[waKey]),
            whatsappApp = p[waAppKey] ?: 0,
            allowWhatsApp = p[allowWaKey] ?: false,
            allowBusiness = p[allowBizKey] ?: false,
            countryCode = p[countryKey] ?: "963"
        )
    }

    private fun modeOf(i: Int?): ActionMode =
        ActionMode.values().getOrElse(i ?: 0) { ActionMode.ASK }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[themeKey] = mode }
    }

    suspend fun setSpeakReplies(enabled: Boolean) {
        context.dataStore.edit { it[speakKey] = enabled }
    }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[onboardedKey] = true }
    }

    suspend fun setCallMode(mode: ActionMode) {
        context.dataStore.edit { it[callKey] = mode.ordinal }
    }

    suspend fun setSmsMode(mode: ActionMode) {
        context.dataStore.edit { it[smsKey] = mode.ordinal }
    }

    suspend fun setWhatsappMode(mode: ActionMode) {
        context.dataStore.edit { it[waKey] = mode.ordinal }
    }

    suspend fun setWhatsappApp(value: Int) {
        context.dataStore.edit { it[waAppKey] = value }
    }

    suspend fun setAllowWhatsApp(value: Boolean) {
        context.dataStore.edit { it[allowWaKey] = value }
    }

    suspend fun setAllowBusiness(value: Boolean) {
        context.dataStore.edit { it[allowBizKey] = value }
    }

    suspend fun setCountryCode(value: String) {
        context.dataStore.edit { it[countryKey] = value.filter { c -> c.isDigit() }.take(4) }
    }
}
