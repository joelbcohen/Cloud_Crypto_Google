package io.callista.cloudcrypto.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * Material 3 theme for Cloud Crypto Wear OS app.
 * Uses the default Material 3 color scheme optimized for Wear OS.
 */
@Composable
fun CloudCryptoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}