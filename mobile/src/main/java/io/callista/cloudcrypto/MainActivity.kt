package io.callista.cloudcrypto

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
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.callista.cloudcrypto.data.AccountSummaryData
import io.callista.cloudcrypto.data.NetworkStatusResponse
import io.callista.cloudcrypto.data.Transaction
import io.callista.cloudcrypto.presentation.theme.CloudCryptoTheme
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationUiState
import io.callista.cloudcrypto.presentation.viewmodel.RegistrationViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+ (API 33+)
        requestNotificationPermission()

        setContent {
            CloudCryptoTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RegistrationScreen(viewModel)
                }
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
    val memo by viewModel.memo.collectAsStateWithLifecycle()
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
                accountId = state.accountId,
                onRegisterClicked = {
                    viewModel.onSerialNumberChanged(UUID.randomUUID().toString())
                    viewModel.showRegistrationForm()
                },
                onAccountClicked = viewModel::showAccountScreen,
                onTransferClicked = viewModel::showTransferScreen,
                onNetworkClicked = viewModel::showNetworkStatusScreen
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
                memo = memo,
                onToAccountChanged = viewModel::onToAccountChanged,
                onAmountChanged = viewModel::onAmountChanged,
                onMemoChanged = viewModel::onMemoChanged,
                onSendClicked = viewModel::executeTransfer,
                onCancelClicked = viewModel::closeTransferScreen,
                isTransferring = isTransferring
            )
        }
        is RegistrationUiState.NetworkStatus -> {
            NetworkStatusScreen(
                networkStatus = state.data,
                onBackClicked = viewModel::closeNetworkStatusScreen
            )
        }
        is RegistrationUiState.Loading -> {
            LoadingScreen(message = state.message)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    serialNumber: String?,
    timestamp: Long,
    accountId: String?,
    onRegisterClicked: () -> Unit,
    onAccountClicked: () -> Unit,
    onTransferClicked: () -> Unit,
    onNetworkClicked: () -> Unit
) {
    val isRegistered = serialNumber != null && serialNumber.isNotEmpty()

    val dateFormatted = remember(timestamp) {
        if (timestamp > 0) {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            "Not registered"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cloud Crypto") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Serial Number Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Serial Number",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = serialNumber ?: "---",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Date Registered",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                     if (accountId != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Account ID: ${accountId}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isRegistered) {
                Button(
                    onClick = onRegisterClicked,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("REGISTER DEVICE")
                }
            } else {
                Button(
                    onClick = onAccountClicked,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 8.dp)
                ) {
                    Text("ACCOUNT SUMMARY")
                }

                Button(
                    onClick = onTransferClicked,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 8.dp)
                ) {
                    Text("TRANSFER FUNDS")
                }

                Button(
                    onClick = onNetworkClicked,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text("NETWORK STATUS")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationInputScreen(
    serialNumber: String,
    onSerialNumberChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    BackHandler(onBack = onCancelClicked)

    val generateSerialNumber = {
        val uuid = UUID.randomUUID().toString()
        "PHONE-${uuid.substring(0, 8).uppercase()}"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Device Registration") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = serialNumber,
                onValueChange = onSerialNumberChanged,
                label = { Text("Serial Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSerialNumberChanged(generateSerialNumber()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Text("GENERATE SERIAL")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onCancelClicked,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCEL")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = onSaveClicked,
                    modifier = Modifier.weight(1f),
                    enabled = serialNumber.isNotBlank()
                ) {
                    Text("SAVE")
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String = "Loading...") {
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
                text = message,
                style = MaterialTheme.typography.bodyLarge
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
    BackHandler(onBack = onCancelClicked)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )
            
            Text(
                text = "Operation Failed",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRetryClicked) {
                Text("RETRY")
            }
            
            TextButton(onClick = onCancelClicked) {
                Text("CANCEL")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSummaryScreen(
    accountData: AccountSummaryData,
    transactions: List<Transaction>,
    onBackClicked: () -> Unit
) {
    BackHandler(onBack = onBackClicked)
    
    val decimalFormat = remember { DecimalFormat("#,##0.00") }
    
    val formattedBalance = remember(accountData.balance) {
        accountData.balance?.let {
            try {
                decimalFormat.format(it.toDouble())
            } catch (e: Exception) {
                it
            }
        } ?: "0.00"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Account Summary") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Text("←") // Use icon in real app
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formattedBalance,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Total Sent",
                        value = accountData.totalSentAmount?.let { decimalFormat.format(it.toDoubleOrNull() ?: 0.0) } ?: "0.00",
                        subtext = "${accountData.totalSentTransactions} txns",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Total Received",
                        value = accountData.totalReceivedAmount?.let { decimalFormat.format(it.toDoubleOrNull() ?: 0.0) } ?: "0.00",
                        subtext = "${accountData.totalReceivedTransactions} txns",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(transactions.size) { index ->
                    TransactionItem(transactions[index], decimalFormat)
                    if (index < transactions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    toAccount: String,
    amount: String,
    memo: String,
    onToAccountChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onMemoChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    isTransferring: Boolean
) {
    BackHandler(enabled = !isTransferring, onBack = onCancelClicked)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transfer Funds") },
                navigationIcon = {
                    IconButton(onClick = onCancelClicked, enabled = !isTransferring) {
                        Text("←")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = toAccount,
                onValueChange = onToAccountChanged,
                label = { Text("To Account ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isTransferring
            )

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChanged,
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isTransferring
            )

            OutlinedTextField(
                value = memo,
                onValueChange = onMemoChanged,
                label = { Text("Memo (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isTransferring
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSendClicked,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTransferring
            ) {
                if (isTransferring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SENDING...")
                } else {
                    Text("SEND FUNDS")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStatusScreen(
    networkStatus: NetworkStatusResponse,
    onBackClicked: () -> Unit
) {
    BackHandler(onBack = onBackClicked)
    val decimalFormat = remember { DecimalFormat("#,###") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Network Status") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Text("←")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (networkStatus.status == "Online")
                            Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "● ${networkStatus.status ?: "Unknown"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (networkStatus.status == "Online")
                                Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }

            // Ledger Stats
            networkStatus.ledgerStats?.let { stats ->
                item {
                    Text(
                        text = "Ledger Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            NetworkStatRow("Accounts", stats.totalAccounts, decimalFormat)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            NetworkStatRow("Transactions", stats.totalTransactions, decimalFormat)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            NetworkStatRow("Mints", stats.totalMints, decimalFormat)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            NetworkStatRow("Transfers", stats.totalTransfers, decimalFormat)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            NetworkStatRow("Minted", stats.totalMinted, decimalFormat)
                        }
                    }
                }
            }

            // Device Stats
            item {
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    networkStatus.deviceStats?.android?.let {
                        StatCard(
                            label = "Google WearOS",
                            value = "${it.count ?: 0}",
                            subtext = "Active",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    networkStatus.deviceStats?.ios?.let {
                        StatCard(
                            label = "Apple WatchOS",
                            value = "${it.count ?: 0}",
                            subtext = "Active",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkStatRow(label: String, value: Any?, format: DecimalFormat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = when (value) {
                is Int -> format.format(value)
                is Long -> format.format(value)
                is Double -> format.format(value)
                else -> value?.toString() ?: "0"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    decimalFormat: DecimalFormat
) {
    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val outputFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

    val dateStr = try {
        transaction.completedAt?.let {
            val date = dateTimeFormat.parse(it)
            date?.let { outputFormat.format(it) } ?: "N/A"
        } ?: "N/A"
    } catch (e: Exception) { "N/A" }

    val amountStr = try {
        transaction.amount?.toDouble()?.let { decimalFormat.format(it) } ?: "0.00"
    } catch (e: Exception) { transaction.amount ?: "0.00" }

    val isIncoming = transaction.direction?.lowercase() == "received"
    val isMint = transaction.txType?.lowercase() == "mint"
    
    val amountColor = when {
        isMint -> Color(0xFF4CAF50) // Green
        isIncoming -> Color(0xFF2196F3) // Blue
        else -> Color(0xFFFF9800) // Orange
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(amountColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isMint) "⬇" else if (isIncoming) "←" else "→",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMint) "Minted" else if (isIncoming) "Received from ${transaction.fromId}" else "Sent to ${transaction.toId}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!transaction.memo.isNullOrBlank()) {
                Text(
                    text = transaction.memo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Amount
        Text(
            text = (if (isMint || isIncoming) "+" else "-") + amountStr,
            style = MaterialTheme.typography.titleMedium,
            color = amountColor
        )
    }
}
