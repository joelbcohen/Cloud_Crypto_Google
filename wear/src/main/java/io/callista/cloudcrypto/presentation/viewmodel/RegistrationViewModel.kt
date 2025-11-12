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

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Initial)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _serialNumber = MutableStateFlow("")
    val serialNumber: StateFlow<String> = _serialNumber.asStateFlow()

    init {
        checkRegistrationStatus()
    }

    /**
     * Checks if the device is already registered.
     */
    private fun checkRegistrationStatus() {
        val status = repository.getRegistrationStatus()
        if (status.isRegistered && status.serialNumber != null) {
            _serialNumber.value = status.serialNumber
            _uiState.value = RegistrationUiState.Registered(status.serialNumber)
        }
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
                        _uiState.value = RegistrationUiState.Registered(currentSerialNumber)
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
     * Resets the registration state to allow re-registration.
     */
    fun resetRegistration() {
        _serialNumber.value = ""
        _uiState.value = RegistrationUiState.Initial
        viewModelScope.launch {
            repository.saveRegistrationStatus("", false)
        }
    }
}

/**
 * Sealed interface representing different UI states.
 */
sealed interface RegistrationUiState {
    data object Initial : RegistrationUiState
    data object Loading : RegistrationUiState
    data class Registered(val serialNumber: String) : RegistrationUiState
    data class Error(val message: String) : RegistrationUiState
}
