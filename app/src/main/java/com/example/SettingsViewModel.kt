package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = repository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun updateBrightness(value: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBrightness(value)
        }
    }

    fun updateVolume(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVolume(value)
        }
    }

    fun updateOrientation(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOrientation(value)
        }
    }

    fun updateIsGridMode(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateIsGridMode(value)
        }
    }

    fun updateSortOrder(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSortOrder(value)
        }
    }
    
    fun updateDeepProcessingMode(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDeepProcessingMode(value)
            
            // Enqueue WorkManager trigger if enabled
            if (value) {
                // Not passing Context directly to ViewModel but this is triggered from UI later
            }
        }
    }

    class Factory(private val repo: UserPreferencesRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
