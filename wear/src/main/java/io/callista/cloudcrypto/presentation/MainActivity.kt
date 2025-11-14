package io.callista.cloudcrypto.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import io.callista.cloudcrypto.presentation.theme.CloudCryptoTheme
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationUiState
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationViewModel

/**
 * Main Activity for the Cloud Crypto Wear OS application.
 * Handles device registration with Material 3 UI.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: RegistrationViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+ (API 33+)
        requestNotificationPermission()

        setContent {
            CloudCryptoTheme {
                RegistrationScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    Log.d("MainActivity", "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Device Registration",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Serial Number Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Serial Number",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = onSerialNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter Serial #", style = MaterialTheme.typography.bodySmall)
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = androidx.compose.ui.graphics.Color.White
                    ),
                    singleLine = true
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
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