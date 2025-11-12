package io.callista.cloudcrypto.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import io.callista.cloudcrypto.presentation.theme.CloudCryptoTheme
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationUiState
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationViewModel

/**
 * Main Activity for the Cloud Crypto Wear OS application.
 * Handles device registration with Material 3 UI.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: RegistrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            CloudCryptoTheme {
                RegistrationScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RegistrationScreen(viewModel: RegistrationViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serialNumber by viewModel.serialNumber.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is RegistrationUiState.Initial -> {
            RegistrationInputScreen(
                serialNumber = serialNumber,
                onSerialNumberChanged = viewModel::onSerialNumberChanged,
                onSaveClicked = viewModel::registerDevice
            )
        }
        is RegistrationUiState.Loading -> {
            LoadingScreen()
        }
        is RegistrationUiState.Registered -> {
            RegisteredScreen(
                serialNumber = state.serialNumber,
                onResetClicked = viewModel::resetRegistration
            )
        }
        is RegistrationUiState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetryClicked = viewModel::registerDevice
            )
        }
    }
}

@Composable
fun RegistrationInputScreen(
    serialNumber: String,
    onSerialNumberChanged: (String) -> Unit,
    onSaveClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Device Registration",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Text(
                text = "Enter Serial Number",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    // On Wear OS, text input is typically handled via voice or companion app
                    // For this demo, we'll use a simple increment mechanism
                    // In production, you'd use rememberLauncherForActivityResult with RemoteInput
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(
                    text = if (serialNumber.isEmpty()) "Tap to Enter" else serialNumber,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            TextInputField(
                value = serialNumber,
                onValueChange = onSerialNumberChanged,
                label = "Serial #"
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            FilledTonalButton(
                onClick = onSaveClicked,
                modifier = Modifier.fillMaxWidth(0.9f),
                enabled = serialNumber.isNotBlank()
            ) {
                Text("SAVE")
            }
        }
    }
}

@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    // Simple text input for Wear OS
    // Note: In production, you'd want to use proper Wear OS text input methods
    Column(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Input buttons for demonstration
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    if (value.isNotEmpty()) {
                        onValueChange(value.dropLast(1))
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("←", style = MaterialTheme.typography.bodySmall)
            }

            (0..9).chunked(5).forEach { rowNumbers ->
                rowNumbers.forEach { num ->
                    OutlinedButton(
                        onClick = { onValueChange(value + num.toString()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(num.toString(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Registering...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun RegisteredScreen(
    serialNumber: String,
    onResetClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "✓",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Text(
                text = "Registered",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = "Serial: $serialNumber",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            OutlinedButton(
                onClick = onResetClicked,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Re-register")
            }
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetryClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⚠",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        item {
            Text(
                text = "Registration Failed",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            FilledTonalButton(
                onClick = onRetryClicked,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Retry")
            }
        }
    }
}