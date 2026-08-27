package com.example.shufa.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("shufa_prefs", 0)

    private val _darkTheme = MutableStateFlow(
        prefs.getBoolean(KEY_DARK_THEME, false)
    )
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    fun toggleDarkTheme() {
        viewModelScope.launch {
            val newValue = !_darkTheme.value
            prefs.edit().putBoolean(KEY_DARK_THEME, newValue).apply()
            _darkTheme.value = newValue
        }
    }

    private companion object {
        const val KEY_DARK_THEME = "dark_theme"
    }
}
