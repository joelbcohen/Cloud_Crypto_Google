package io.callista.cloudcrypto.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.callista.cloudcrypto.complication.MainComplicationService
import io.callista.cloudcrypto.data.AccountSummaryData
import io.callista.cloudcrypto.data.NetworkStatusResponse
import io.callista.cloudcrypto.data.RegistrationRepository
import io.callista.cloudcrypto.data.Transaction
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

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _toAccount = MutableStateFlow("")
    val toAccount: StateFlow<String> = _toAccount.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _memo = MutableStateFlow("")
    val memo: StateFlow<String> = _memo.asStateFlow()

    private val _isTransferring = MutableStateFlow(false)
    val isTransferring: StateFlow<Boolean> = _isTransferring.asStateFlow()

    init {
        loadMainScreen()
    }

    /**
     * Clears the toast message after it's been shown.
     */
    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * Loads the main screen with current registration status.
     */
    private fun loadMainScreen() {
        val status = repository.getRegistrationStatus()
        _serialNumber.value = status.serialNumber ?: ""
        _uiState.value = RegistrationUiState.MainScreen(
            serialNumber = status.serialNumber,
            timestamp = status.timestamp,
            accountId = status.accountId
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

        _uiState.value = RegistrationUiState.Loading("Registering...")

        viewModelScope.launch {
            try {
                val result = repository.registerDevice(currentSerialNumber)

                result.fold(
                    onSuccess = { response ->
                        var accountId = response.accountId
                        
                        // Save initial status so we can fetch summary if needed
                        repository.saveRegistrationStatus(currentSerialNumber, true, accountId)

                        // If accountId is missing, try to fetch it from account summary
                        if (accountId == null) {
                             val summaryResult = repository.getAccountSummary()
                             summaryResult.onSuccess { summary ->
                                 accountId = summary.data?.id
                                 // Update status with fetched accountId
                                 repository.saveRegistrationStatus(currentSerialNumber, true, accountId)
                             }
                        }

                        MainComplicationService.requestUpdate(getApplication())
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
     * Deregisters the device and clears registration data.
     */
    fun deregisterDevice() {
        _uiState.value = RegistrationUiState.Loading("Deregistering...")

        viewModelScope.launch {
            try {
                val result = repository.deregisterDevice()

                result.fold(
                    onSuccess = { response ->
                        // Clear registration status and cached balance
                        repository.saveRegistrationStatus("", false)
                        repository.saveBalance("0")
                        MainComplicationService.requestUpdate(getApplication())
                        // Show toast message
                        _toastMessage.value = "Device deregistered successfully"
                        // Return to main screen
                        loadMainScreen()
                    },
                    onFailure = { error ->
                        _uiState.value = RegistrationUiState.Error(
                            error.message ?: "Deregistration failed"
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
     * Shows the account screen and fetches account summary.
     */
    fun showAccountScreen() {
        _uiState.value = RegistrationUiState.Loading("Fetching Account Data...")

        viewModelScope.launch {
            try {
                val result = repository.getAccountSummary()

                result.fold(
                    onSuccess = { response ->
                        if (response.data != null) {
                            // Cache balance for complication display
                            response.data.balance?.let {
                                repository.saveBalance(it)
                                MainComplicationService.requestUpdate(getApplication())
                            }
                            _uiState.value = RegistrationUiState.AccountSummary(
                                response.data,
                                response.transactions ?: emptyList()
                            )
                        } else {
                            _uiState.value = RegistrationUiState.Error(
                                response.message ?: "Failed to fetch account summary"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = RegistrationUiState.Error(
                            error.message ?: "Failed to fetch account summary"
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
     * Returns to the main screen from account summary.
     */
    fun closeAccountScreen() {
        loadMainScreen()
    }

    /**
     * Shows the settings screen (placeholder for future implementation).
     */
    fun showSettingsScreen() {
        // TODO: Navigate to settings screen
    }

    /**
     * Shows the network status screen and fetches network status.
     */
    fun showNetworkStatusScreen() {
        _uiState.value = RegistrationUiState.Loading("Fetching Network Status...")

        viewModelScope.launch {
            try {
                val result = repository.getNetworkStatus()

                result.fold(
                    onSuccess = { response ->
                        _uiState.value = RegistrationUiState.NetworkStatus(response)
                    },
                    onFailure = { error ->
                        _uiState.value = RegistrationUiState.Error(
                            error.message ?: "Failed to fetch network status"
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
     * Returns to the main screen from network status screen.
     */
    fun closeNetworkStatusScreen() {
        loadMainScreen()
    }

    /**
     * Shows the transfer screen.
     */
    fun showTransferScreen() {
        _toAccount.value = ""
        _amount.value = ""
        _memo.value = ""
        _uiState.value = RegistrationUiState.TransferScreen
    }

    /**
     * Updates the to account input.
     */
    fun onToAccountChanged(newValue: String) {
        _toAccount.value = newValue
    }

    /**
     * Updates the amount input.
     */
    fun onAmountChanged(newValue: String) {
        _amount.value = newValue
    }

    /**
     * Updates the memo input.
     */
    fun onMemoChanged(newValue: String) {
        _memo.value = newValue
    }

    /**
     * Executes the transfer.
     */
    fun executeTransfer() {
        val currentToAccount = _toAccount.value
        val currentAmount = _amount.value
        val currentMemo = _memo.value.ifBlank { null }

        if (currentToAccount.isBlank()) {
            _toastMessage.value = "To Account cannot be empty"
            return
        }

        if (currentAmount.isBlank()) {
            _toastMessage.value = "Amount cannot be empty"
            return
        }

        // Prevent double-click
        if (_isTransferring.value) {
            return
        }

        _isTransferring.value = true
        _uiState.value = RegistrationUiState.Loading("Processing Transfer...")

        viewModelScope.launch {
            try {
                val result = repository.transfer(currentToAccount, currentAmount, currentMemo)

                result.fold(
                    onSuccess = { response ->
                        _isTransferring.value = false
                        // Update cached balance for complication
                        response.newBalance?.let {
                            repository.saveBalance(it)
                            MainComplicationService.requestUpdate(getApplication())
                        }
                        // Show success message
                        _toastMessage.value = response.message ?: "Transfer successful"
                        // Return to main screen
                        loadMainScreen()
                    },
                    onFailure = { error ->
                        _isTransferring.value = false
                        _uiState.value = RegistrationUiState.Error(
                            error.message ?: "Transfer failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _isTransferring.value = false
                _uiState.value = RegistrationUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Returns to the main screen from transfer screen.
     */
    fun closeTransferScreen() {
        _isTransferring.value = false
        loadMainScreen()
    }
}

/**
 * Sealed interface representing different UI states.
 */
sealed interface RegistrationUiState {
    data class MainScreen(val serialNumber: String?, val timestamp: Long, val accountId: String? = null) : RegistrationUiState
    data object RegistrationForm : RegistrationUiState
    data class AccountSummary(val data: AccountSummaryData, val transactions: List<Transaction>) : RegistrationUiState
    data object TransferScreen : RegistrationUiState
    data class NetworkStatus(val data: NetworkStatusResponse) : RegistrationUiState
    data class Loading(val message: String = "Loading...") : RegistrationUiState
    data class Error(val message: String) : RegistrationUiState
}
