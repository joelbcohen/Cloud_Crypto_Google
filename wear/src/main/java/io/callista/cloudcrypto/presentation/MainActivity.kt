package io.callista.cloudcrypto.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import io.callista.cloudcrypto.data.AccountSummaryData
import io.callista.cloudcrypto.data.Transaction
import io.callista.cloudcrypto.presentation.theme.CloudCryptoTheme
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationUiState
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val toAccount by viewModel.toAccount.collectAsStateWithLifecycle()
    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val isTransferring by viewModel.isTransferring.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Show toast message when available
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    when (val state = uiState) {
        is RegistrationUiState.MainScreen -> {
            MainScreen(
                serialNumber = state.serialNumber,
                timestamp = state.timestamp,
                onRegisterClicked = {
                    viewModel.onSerialNumberChanged(UUID.randomUUID().toString())
                    viewModel.showRegistrationForm()
                },
                onDeregisterClicked = viewModel::deregisterDevice,
                onAccountClicked = viewModel::showAccountScreen,
                onTransferClicked = viewModel::showTransferScreen,
                onSettingsClicked = viewModel::showSettingsScreen
            )
        }
        is RegistrationUiState.RegistrationForm -> {
            RegistrationInputScreen(
                serialNumber = serialNumber,
                onSerialNumberChanged = viewModel::onSerialNumberChanged,
                onSaveClicked = viewModel::registerDevice,
                onCancelClicked = viewModel::cancelRegistration
            )
        }
        is RegistrationUiState.AccountSummary -> {
            AccountSummaryScreen(
                accountData = state.data,
                transactions = state.transactions,
                onBackClicked = viewModel::closeAccountScreen
            )
        }
        is RegistrationUiState.TransferScreen -> {
            TransferScreen(
                toAccount = toAccount,
                amount = amount,
                onToAccountChanged = viewModel::onToAccountChanged,
                onAmountChanged = viewModel::onAmountChanged,
                onSendClicked = viewModel::executeTransfer,
                onCancelClicked = viewModel::closeTransferScreen,
                isTransferring = isTransferring
            )
        }
        is RegistrationUiState.Loading -> {
            LoadingScreen()
        }
        is RegistrationUiState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetryClicked = viewModel::registerDevice,
                onCancelClicked = viewModel::cancelRegistration
            )
        }
    }
}

@Composable
fun MainScreen(
    serialNumber: String?,
    timestamp: Long,
    onRegisterClicked: () -> Unit,
    onDeregisterClicked: () -> Unit,
    onAccountClicked: () -> Unit,
    onTransferClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    val isRegistered = serialNumber != null && serialNumber.isNotEmpty()

    val dateFormatted = remember(timestamp) {
        if (timestamp > 0) {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            "Not registered"
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Cloud Crypto",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Serial Number Display
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Serial Number",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = serialNumber ?: "---",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = androidx.compose.ui.graphics.Color.White
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Date Registered
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Date Registered",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // REGISTER Button (only show when not registered)
        if (!isRegistered) {
            item {
                FilledTonalButton(
                    onClick = onRegisterClicked,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text("REGISTER")
                }
            }
        }

        // DE-REGISTER Button (hidden/commented out)
        // item {
        //     if (isRegistered) {
        //         FilledTonalButton(
        //             onClick = onDeregisterClicked,
        //             modifier = Modifier.fillMaxWidth(0.85f)
        //         ) {
        //             Text("DE-REGISTER")
        //         }
        //     }
        // }

        // ACCOUNT Button (only show when registered)
        if (isRegistered) {
            item {
                FilledTonalButton(
                    onClick = onAccountClicked,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text("ACCOUNT")
                }
            }
        }

        // TRANSFER Button (only show when registered)
        if (isRegistered) {
            item {
                FilledTonalButton(
                    onClick = onTransferClicked,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text("TRANSFER")
                }
            }
        }

        // SETTINGS Button (only show when registered)
        if (isRegistered) {
            item {
                FilledTonalButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text("SETTINGS")
                }
            }
        }
    }
}

@Composable
fun RegistrationInputScreen(
    serialNumber: String,
    onSerialNumberChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    // Handle back gesture/button
    BackHandler(onBack = onCancelClicked)

    // Function to generate a unique serial number
    val generateSerialNumber = {
        val uuid = UUID.randomUUID().toString()
        // Use first 8 characters of UUID for a shorter, readable serial
        "WATCH-${uuid.substring(0, 8).uppercase()}"
    }

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

        // GENERATE Button
        item {
            FilledTonalButton(
                onClick = {
                    onSerialNumberChanged(generateSerialNumber())
                },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text("GENERATE")
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // SAVE Button
        item {
            FilledTonalButton(
                onClick = onSaveClicked,
                modifier = Modifier.fillMaxWidth(0.85f),
                enabled = serialNumber.isNotBlank()
            ) {
                Text("SAVE")
            }
        }

        // CANCEL Button
        item {
            OutlinedButton(
                onClick = onCancelClicked,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text("CANCEL")
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
fun ErrorScreen(
    message: String,
    onRetryClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    // Handle back gesture/button
    BackHandler(onBack = onCancelClicked)

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
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text("Retry")
            }
        }

        item {
            OutlinedButton(
                onClick = onCancelClicked,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text("CANCEL")
            }
        }
    }
}

@Composable
fun AccountSummaryScreen(
    accountData: AccountSummaryData,
    transactions: List<Transaction>,
    onBackClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    var showTransactions by remember { mutableStateOf(false) }

    // Handle back gesture/button
    BackHandler(onBack = onBackClicked)

    // Format large numbers with commas
    val decimalFormat = remember { DecimalFormat("#,##0.00") }

    // Format balance
    val formattedBalance = remember(accountData.balance) {
        accountData.balance?.let {
            try {
                decimalFormat.format(it.toDouble())
            } catch (e: Exception) {
                it
            }
        } ?: "0.00"
    }

    // Format sent amount
    val formattedSentAmount = remember(accountData.totalSentAmount) {
        accountData.totalSentAmount?.let {
            try {
                decimalFormat.format(it.toDouble())
            } catch (e: Exception) {
                it
            }
        } ?: "0.00"
    }

    // Format received amount
    val formattedReceivedAmount = remember(accountData.totalReceivedAmount) {
        accountData.totalReceivedAmount?.let {
            try {
                decimalFormat.format(it.toDouble())
            } catch (e: Exception) {
                it
            }
        } ?: "0.00"
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        item {
            Text(
                text = "Account Summary",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Balance Card
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = formattedBalance,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Transaction Statistics Title
        item {
            Text(
                text = "Transaction Stats",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Sent Transactions
        item {
            StatisticRow(
                label = "Total Sent",
                count = accountData.totalSentTransactions.toString() + " txns",
                amount = formattedSentAmount
            )
        }

        // Received Transactions
        item {
            StatisticRow(
                label = "Total Received",
                count = accountData.totalReceivedTransactions.toString() + " txns",
                amount = formattedReceivedAmount
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Device Info Title
        item {
            Text(
                text = "Device Info",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Device Model
        if (!accountData.model.isNullOrBlank() || !accountData.brand.isNullOrBlank()) {
            item {
                InfoRow(
                    label = "Device",
                    value = "${accountData.brand ?: ""} ${accountData.model ?: ""}".trim()
                )
            }
        }

        // Account ID
        if (!accountData.id.isNullOrBlank()) {
            item {
                InfoRow(
                    label = "Account ID",
                    value = accountData.id.take(12) + "..."
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Transaction History Dropdown
        item {
            FilledTonalButton(
                onClick = { showTransactions = !showTransactions },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(if (showTransactions) "HIDE HISTORY" else "SHOW HISTORY")
            }
        }

        // Transaction List
        if (showTransactions) {
            item {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                transactions.forEach { transaction ->
                    item {
                        TransactionItem(transaction = transaction, decimalFormat = decimalFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun TransferScreen(
    toAccount: String,
    amount: String,
    onToAccountChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    isTransferring: Boolean
) {
    val listState = rememberScalingLazyListState()

    // Handle back gesture/button (only when not transferring)
    BackHandler(enabled = !isTransferring, onBack = onCancelClicked)

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        item {
            Text(
                text = "Transfer",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // To Account Text Field
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Text(
                    text = "To Account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = toAccount,
                    onValueChange = onToAccountChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color = androidx.compose.ui.graphics.Color.White
                    ),
                    singleLine = true,
                    enabled = !isTransferring
                )
            }
        }

        // Amount Text Field
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color = androidx.compose.ui.graphics.Color.White
                    ),
                    singleLine = true,
                    enabled = !isTransferring
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Send Button
        item {
            FilledTonalButton(
                onClick = onSendClicked,
                modifier = Modifier.fillMaxWidth(0.85f),
                enabled = !isTransferring
            ) {
                Text(if (isTransferring) "SENDING..." else "SEND")
            }
        }
    }
}

@Composable
fun StatisticRow(
    label: String,
    count: String,
    amount: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = count,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    decimalFormat: DecimalFormat
) {
    // Parse and format completed date and time
    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val (completedDate, completedTime) = try {
        transaction.completedAt?.let {
            val date = dateTimeFormat.parse(it)
            if (date != null) {
                Pair(dateFormat.format(date), timeFormat.format(date))
            } else {
                Pair("N/A", "N/A")
            }
        } ?: Pair("N/A", "N/A")
    } catch (e: Exception) {
        Pair("N/A", "N/A")
    }

    // Format amount
    val formattedAmount = try {
        transaction.amount?.toDouble()?.let { decimalFormat.format(it) } ?: "0.00"
    } catch (e: Exception) {
        transaction.amount ?: "0.00"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = transaction.txType?.uppercase() ?: "UNKNOWN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "From: ${transaction.fromId ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = " → ",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "To: ${transaction.toId ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = completedDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = completedTime,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}