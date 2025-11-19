//
//  DeviceAttestationManager.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//
//  Device attestation and cryptographic key management for iOS
//  Equivalent to DeviceAttestationManager.kt in Android version

import Foundation
import Security
import CryptoKit
import DeviceCheck

enum AttestationError: Error {
    case keyGenerationFailed
    case keyRetrievalFailed
    case attestationNotSupported
    case attestationFailed(Error)
    case keyDeletionFailed

    var localizedDescription: String {
        switch self {
        case .keyGenerationFailed:
            return "Failed to generate cryptographic key"
        case .keyRetrievalFailed:
            return "Failed to retrieve cryptographic key"
        case .attestationNotSupported:
            return "Device attestation is not supported on this device"
        case .attestationFailed(let error):
            return "Attestation failed: \(error.localizedDescription)"
        case .keyDeletionFailed:
            return "Failed to delete cryptographic key"
        }
    }
}

class DeviceAttestationManager {
    static let shared = DeviceAttestationManager()

    private let keyTag = "io.callista.cloudcrypto.devicekey"
    private let keySize = 2048
    private let appAttestService = DCAppAttestService.shared

    private init() {}

    // MARK: - Key Management

    /// Generate a new RSA 2048-bit key pair in the Secure Enclave (or Keychain)
    func generateKeyPair() throws {
        // Delete existing key if present
        try? deleteKeyPair()

        // Define key attributes
        let attributes: [String: Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeySizeInBits as String: keySize,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String: true,
                kSecAttrApplicationTag as String: keyTag.data(using: .utf8)!,
                // Use Secure Enclave if available (only on some devices)
                // kSecAttrTokenID as String: kSecAttrTokenIDSecureEnclave
            ]
        ]

        var error: Unmanaged<CFError>?
        guard let privateKey = SecKeyCreateRandomKey(attributes as CFDictionary, &error) else {
            if let error = error {
                print("Key generation error: \(error.takeRetainedValue())")
            }
            throw AttestationError.keyGenerationFailed
        }

        print("Key pair generated successfully")
        print("Key tag: \(keyTag)")
    }

    /// Retrieve the existing private key from the Keychain
    func getPrivateKey() throws -> SecKey {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: keyTag.data(using: .utf8)!,
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecReturnRef as String: true
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)

        guard status == errSecSuccess,
              let privateKey = item as! SecKey? else {
            print("Failed to retrieve private key. Status: \(status)")
            throw AttestationError.keyRetrievalFailed
        }

        return privateKey
    }

    /// Get the public key from the private key
    func getPublicKey() throws -> SecKey {
        let privateKey = try getPrivateKey()

        guard let publicKey = SecKeyCopyPublicKey(privateKey) else {
            throw AttestationError.keyRetrievalFailed
        }

        return publicKey
    }

    /// Get the public key as a Base64-encoded string (DER format)
    func getPublicKeyBase64() throws -> String {
        let publicKey = try getPublicKey()

        var error: Unmanaged<CFError>?
        guard let publicKeyData = SecKeyCopyExternalRepresentation(publicKey, &error) as Data? else {
            if let error = error {
                print("Public key export error: \(error.takeRetainedValue())")
            }
            throw AttestationError.keyRetrievalFailed
        }

        return publicKeyData.base64EncodedString()
    }

    /// Delete the key pair from the Keychain
    func deleteKeyPair() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassKey,
            kSecAttrApplicationTag as String: keyTag.data(using: .utf8)!
        ]

        let status = SecItemDelete(query as CFDictionary)

        if status != errSecSuccess && status != errSecItemNotFound {
            print("Failed to delete key pair. Status: \(status)")
            throw AttestationError.keyDeletionFailed
        }

        print("Key pair deleted successfully")
    }

    // MARK: - Device Attestation

    /// Check if App Attest is supported on this device
    func isAttestationSupported() -> Bool {
        return appAttestService.isSupported
    }

    /// Generate attestation data using App Attest
    /// Returns a Base64-encoded attestation string
    func generateAttestation() async throws -> String {
        // For watchOS, App Attest may not be fully supported
        // We'll use a fallback approach: sign the public key with the private key
        // and return that as "attestation"

        if isAttestationSupported() {
            return try await generateAppAttestAttestation()
        } else {
            return try generateFallbackAttestation()
        }
    }

    /// Generate attestation using App Attest (iOS 14+)
    private func generateAppAttestAttestation() async throws -> String {
        do {
            // Generate a key ID for App Attest
            let keyId = try await appAttestService.generateKey()
            print("App Attest key generated: \(keyId)")

            // Create a client data hash (challenge)
            let clientData = UUID().uuidString.data(using: .utf8)!
            let clientDataHash = SHA256.hash(data: clientData)
            let hash = Data(clientDataHash)

            // Generate attestation
            let attestation = try await appAttestService.attestKey(keyId, clientDataHash: hash)

            // Store the key ID for later use
            UserDefaults.standard.set(keyId, forKey: "appAttestKeyId")

            return attestation.base64EncodedString()
        } catch {
            print("App Attest error: \(error)")
            throw AttestationError.attestationFailed(error)
        }
    }

    /// Generate a fallback attestation by signing device info
    private func generateFallbackAttestation() throws -> String {
        print("Using fallback attestation (App Attest not supported)")

        // Create attestation data from device information
        let deviceInfo = DeviceInfoManager.shared.getAllDeviceInfo()
        let attestationData = [
            "deviceId": deviceInfo["deviceId"] ?? "",
            "model": deviceInfo["model"] ?? "",
            "osVersion": deviceInfo["osVersion"] ?? "",
            "timestamp": ISO8601DateFormatter().string(from: Date())
        ]

        // Convert to JSON
        guard let jsonData = try? JSONSerialization.data(withJSONObject: attestationData, options: []) else {
            throw AttestationError.attestationFailed(NSError(domain: "Attestation", code: -1))
        }

        // Sign the data with the private key
        let privateKey = try getPrivateKey()

        var error: Unmanaged<CFError>?
        guard let signature = SecKeyCreateSignature(
            privateKey,
            .rsaSignatureMessagePKCS1v15SHA256,
            jsonData as CFData,
            &error
        ) as Data? else {
            if let error = error {
                print("Signing error: \(error.takeRetainedValue())")
            }
            throw AttestationError.attestationFailed(NSError(domain: "Attestation", code: -2))
        }

        // Combine JSON data and signature
        let attestation = jsonData + signature
        return attestation.base64EncodedString()
    }

    /// Verify that the key pair exists and is valid
    func verifyKeyPair() -> Bool {
        do {
            let _ = try getPrivateKey()
            let _ = try getPublicKey()
            return true
        } catch {
            return false
        }
    }
}
