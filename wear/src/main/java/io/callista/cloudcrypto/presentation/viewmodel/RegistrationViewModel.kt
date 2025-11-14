package io.callista.cloudcrypto.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.callista.cloudcrypto.data.RegistrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing device registration state.
 */
class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RegistrationRepository(application)

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.MainScreen(null, 0L))
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _serialNumber = MutableStateFlow("")
    val serialNumber: StateFlow<String> = _serialNumber.asStateFlow()

    init {
        loadMainScreen()
    }

    /**
     * Loads the main screen with current registration status.
     */
    private fun loadMainScreen() {
        val status = repository.getRegistrationStatus()
        _serialNumber.value = status.serialNumber ?: ""
        _uiState.value = RegistrationUiState.MainScreen(
            serialNumber = status.serialNumber,
            timestamp = status.timestamp
        )
    }

    /**
     * Navigates to the registration form.
     */
    fun showRegistrationForm() {
        _serialNumber.value = ""
        _uiState.value = RegistrationUiState.RegistrationForm
    }

    /**
     * Cancels registration and returns to main screen.
     */
    fun cancelRegistration() {
        _serialNumber.value = ""
        loadMainScreen()
    }

    /**
     * Updates the serial number input.
     */
    fun onSerialNumberChanged(newValue: String) {
        _serialNumber.value = newValue
    }

    /**
     * Registers the device with the current serial number.
     */
    fun registerDevice() {
        val currentSerialNumber = _serialNumber.value

        if (currentSerialNumber.isBlank()) {
            _uiState.value = RegistrationUiState.Error("Serial number cannot be empty")
            return
        }

        _uiState.value = RegistrationUiState.Loading

        viewModelScope.launch {
            try {
                val result = repository.registerDevice(currentSerialNumber)

                result.fold(
                    onSuccess = { response ->
                        repository.saveRegistrationStatus(currentSerialNumber, true)
                        // Return to main screen after successful registration
                        loadMainScreen()
                    },
                    onFailure = { error ->
                        _uiState.value = RegistrationUiState.Error(
                            error.message ?: "Registration failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = RegistrationUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Shows the account screen (placeholder for future implementation).
     */
    fun showAccountScreen() {
        // TODO: Navigate to account screen
    }

    /**
     * Shows the settings screen (placeholder for future implementation).
     */
    fun showSettingsScreen() {
        // TODO: Navigate to settings screen
    }
}

/**
 * Sealed interface representing different UI states.
 */
sealed interface RegistrationUiState {
    data class MainScreen(val serialNumber: String?, val timestamp: Long) : RegistrationUiState
    data object RegistrationForm : RegistrationUiState
    data object Loading : RegistrationUiState
    data class Error(val message: String) : RegistrationUiState
}
