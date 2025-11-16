package io.callista.cloudcrypto.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages device attestation and key generation using Android KeyStore.
 *
 * This manager handles:
 * - Generating key pairs with hardware-backed attestation
 * - Extracting public keys for registration
 * - Building attestation certificate chains as proof of device integrity
 */
class DeviceAttestationManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceAttestation"
        private const val KEY_ALIAS = "CloudCryptoDeviceKey"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    }

    /**
     * Data class containing attestation information for device registration.
     */
    data class AttestationData(
        val publicKey: String,           // Base64-encoded public key
        val attestationBlob: String,     // Base64-encoded attestation certificate chain
        val algorithm: String = "RSA"    // Key algorithm
    )

    /**
     * Generates a new key pair with attestation and returns the attestation data.
     *
     * @param challenge A server-provided challenge for attestation (optional)
     * @return AttestationData containing public key and attestation certificate chain
     */
    suspend fun generateAttestationData(challenge: ByteArray? = null): AttestationData = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating device attestation data...")

            // Delete existing key if present
            deleteExistingKey()

            // Generate new key pair with attestation
            val keyPair = generateKeyPairWithAttestation(challenge)

            // Extract public key
            val publicKeyBytes = keyPair.public.encoded
            val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)

            Log.d(TAG, "Public key generated: ${publicKeyBase64.take(50)}...")

            // Get attestation certificate chain
            val attestationBlob = getAttestationCertificateChain()

            Log.d(TAG, "Attestation blob size: ${attestationBlob.length} chars")

            AttestationData(
                publicKey = publicKeyBase64,
                attestationBlob = attestationBlob,
                algorithm = "RSA"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate attestation data", e)

            // Return a fallback with just a public key (no attestation)
            generateFallbackKey()
        }
    }

    /**
     * Generates a key pair with hardware-backed attestation if available.
     */
    private fun generateKeyPairWithAttestation(challenge: ByteArray?): java.security.KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            KEYSTORE_PROVIDER
        )

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)

        // Add attestation challenge if available (Android 7.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && challenge != null) {
            builder.setAttestationChallenge(challenge)
            Log.d(TAG, "Attestation challenge included")
        }

        keyPairGenerator.initialize(builder.build())

        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Retrieves the attestation certificate chain from the KeyStore.
     * The chain provides proof of key properties and device integrity.
     */
    private fun getAttestationCertificateChain(): String {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            val certChain = keyStore.getCertificateChain(KEY_ALIAS)

            if (certChain == null || certChain.isEmpty()) {
                Log.w(TAG, "No certificate chain available")
                return ""
            }

            Log.d(TAG, "Certificate chain length: ${certChain.size}")

            // Encode all certificates in the chain
            val certChainData = certChain.mapIndexed { index, cert ->
                val x509Cert = cert as X509Certificate
                Log.d(TAG, "Cert $index - Subject: ${x509Cert.subjectDN}")
                Log.d(TAG, "Cert $index - Issuer: ${x509Cert.issuerDN}")

                Base64.encodeToString(cert.encoded, Base64.NO_WRAP)
            }

            // Join certificates with a delimiter for transmission
            return certChainData.joinToString(separator = "|")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get attestation certificate chain", e)
            return ""
        }
    }

    /**
     * Deletes the existing key from the KeyStore if present.
     */
    private fun deleteExistingKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.d(TAG, "Existing key deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete existing key", e)
        }
    }

    /**
     * Generates a fallback key without attestation for devices that don't support it.
     */
    private fun generateFallbackKey(): AttestationData {
        try {
            Log.w(TAG, "Using fallback key generation without attestation")

            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                KEYSTORE_PROVIDER
            )

            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .build()

            keyPairGenerator.initialize(spec)
            val keyPair = keyPairGenerator.generateKeyPair()

            val publicKeyBytes = keyPair.public.encoded
            val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)

            return AttestationData(
                publicKey = publicKeyBase64,
                attestationBlob = "",
                algorithm = "RSA"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fallback key generation failed", e)
            throw e
        }
    }

    /**
     * Gets the cached public key if it exists (without regenerating).
     */
    suspend fun getCachedPublicKey(): String? = withContext(Dispatchers.IO) {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                return@withContext null
            }

            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            val publicKey = entry?.certificate?.publicKey

            publicKey?.let {
                Base64.encodeToString(it.encoded, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached public key", e)
            null
        }
    }
}
