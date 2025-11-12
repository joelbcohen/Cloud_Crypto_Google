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
    val imei by viewModel.imei.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is RegistrationUiState.Initial -> {
            RegistrationInputScreen(
                serialNumber = serialNumber,
                imei = imei,
                onSerialNumberChanged = viewModel::onSerialNumberChanged,
                onImeiChanged = viewModel::onImeiChanged,
                onSaveClicked = viewModel::registerDevice
            )
        }
        is RegistrationUiState.Loading -> {
            LoadingScreen()
        }
        is RegistrationUiState.Registered -> {
            RegisteredScreen(
                serialNumber = state.serialNumber,
                imei = state.imei,
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
    imei: String,
    onSerialNumberChanged: (String) -> Unit,
    onImeiChanged: (String) -> Unit,
    onSaveClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                text = "Device Registration",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Serial Number Section
        item {
            Text(
                text = "Serial Number",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Text(
                    text = if (serialNumber.isEmpty()) "Enter Serial" else serialNumber,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            NumericKeyboard(
                value = serialNumber,
                onValueChange = onSerialNumberChanged
            )
        }

        // IMEI Section
        item {
            Text(
                text = "IMEI",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }

        item {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Text(
                    text = if (imei.isEmpty()) "Enter IMEI" else imei,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            AlphanumericKeyboard(
                value = imei,
                onValueChange = onImeiChanged
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            FilledTonalButton(
                onClick = onSaveClicked,
                modifier = Modifier.fillMaxWidth(0.95f),
                enabled = serialNumber.isNotBlank() && imei.isNotBlank()
            ) {
                Text("SAVE")
            }
        }
    }
}

@Composable
fun NumericKeyboard(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.95f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: 1 2 3
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (num in 1..3) {
                OutlinedButton(
                    onClick = { onValueChange(value + num.toString()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(num.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Row 2: 4 5 6
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (num in 4..6) {
                OutlinedButton(
                    onClick = { onValueChange(value + num.toString()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(num.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Row 3: 7 8 9
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (num in 7..9) {
                OutlinedButton(
                    onClick = { onValueChange(value + num.toString()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(num.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Row 4: ← 0 CLR
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
                Text("←", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { onValueChange(value + "0") },
                modifier = Modifier.weight(1f)
            ) {
                Text("0", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = { onValueChange("") },
                modifier = Modifier.weight(1f)
            ) {
                Text("CLR", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AlphanumericKeyboard(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.95f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Row 1: 1-5
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (num in 1..5) {
                OutlinedButton(
                    onClick = { onValueChange(value + num.toString()) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text(num.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Row 2: 6-9, 0
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (num in 6..9) {
                OutlinedButton(
                    onClick = { onValueChange(value + num.toString()) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text(num.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
            OutlinedButton(
                onClick = { onValueChange(value + "0") },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("0", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Row 3: A-E
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (char in 'A'..'E') {
                OutlinedButton(
                    onClick = { onValueChange(value + char.toString()) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text(char.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Row 4: F-J
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (char in 'F'..'J') {
                OutlinedButton(
                    onClick = { onValueChange(value + char.toString()) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text(char.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Row 5: ← CLR
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    if (value.isNotEmpty()) {
                        onValueChange(value.dropLast(1))
                    }
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("←", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { onValueChange("") },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("CLR", style = MaterialTheme.typography.labelSmall)
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
    imei: String,
    onResetClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Serial: $serialNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "IMEI: $imei",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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