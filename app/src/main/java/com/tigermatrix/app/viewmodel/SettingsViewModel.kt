package com.tigermatrix.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tigermatrix.app.data.ActionMode
import com.tigermatrix.app.data.ActionSettings
import com.tigermatrix.app.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)

    val themeMode: StateFlow<Int> = store.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val speakReplies: StateFlow<Boolean> = store.speakReplies
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** null = لسا عم نقرأ القيمة من التخزين (بنتجنب وميض شاشة الترحيب) */
    val onboarded: StateFlow<Boolean?> = store.onboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val actionSettings: StateFlow<ActionSettings> = store.actionSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ActionSettings())

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }

    fun setSpeakReplies(enabled: Boolean) {
        viewModelScope.launch { store.setSpeakReplies(enabled) }
    }

    fun setOnboarded() {
        viewModelScope.launch { store.setOnboarded() }
    }

    fun setCallMode(mode: ActionMode) {
        viewModelScope.launch { store.setCallMode(mode) }
    }

    fun setSmsMode(mode: ActionMode) {
        viewModelScope.launch { store.setSmsMode(mode) }
    }

    fun setWhatsappMode(mode: ActionMode) {
        viewModelScope.launch { store.setWhatsappMode(mode) }
    }

    fun setWhatsappApp(value: Int) {
        viewModelScope.launch { store.setWhatsappApp(value) }
    }

    fun setAllowWhatsApp(value: Boolean) {
        viewModelScope.launch { store.setAllowWhatsApp(value) }
    }

    fun setAllowBusiness(value: Boolean) {
        viewModelScope.launch { store.setAllowBusiness(value) }
    }

    fun setCountryCode(value: String) {
        viewModelScope.launch { store.setCountryCode(value) }
    }
}
