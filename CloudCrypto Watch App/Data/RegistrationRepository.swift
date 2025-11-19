//
//  RegistrationRepository.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import Foundation

class RegistrationRepository {
    private let api = RegistrationApi.shared
    private let deviceInfo = DeviceInfoManager.shared
    private let apnsManager = APNsManager.shared
    private let attestationManager = DeviceAttestationManager.shared

    // UserDefaults keys
    private let registeredKey = "isRegistered"
    private let serialNumberKey = "serialNumber"
    private let registrationDateKey = "registrationDate"
    private let publicKeyKey = "publicKey"
    private let attestationBlobKey = "attestationBlob"

    // MARK: - Registration Status

    func isRegistered() -> Bool {
        return UserDefaults.standard.bool(forKey: registeredKey)
    }

    func getSerialNumber() -> String? {
        return UserDefaults.standard.string(forKey: serialNumberKey)
    }

    func getRegistrationDate() -> String? {
        return UserDefaults.standard.string(forKey: registrationDateKey)
    }

    private func getStoredPublicKey() -> String? {
        return UserDefaults.standard.string(forKey: publicKeyKey)
    }

    private func getStoredAttestationBlob() -> String? {
        return UserDefaults.standard.string(forKey: attestationBlobKey)
    }

    // MARK: - Registration

    func registerDevice(serialNumber: String) async throws {
        print("Starting device registration...")

        // Step 1: Request notification permission
        let notificationGranted = await apnsManager.requestNotificationPermission()
        print("Notification permission: \(notificationGranted)")

        // Step 2: Generate cryptographic key pair
        print("Generating key pair...")
        try attestationManager.generateKeyPair()

        // Step 3: Get public key
        print("Retrieving public key...")
        let publicKey = try attestationManager.getPublicKeyBase64()
        print("Public key length: \(publicKey.count)")

        // Step 4: Generate attestation
        print("Generating attestation...")
        let attestationBlob = try await attestationManager.generateAttestation()
        print("Attestation blob length: \(attestationBlob.count)")

        // Step 5: Get device information
        let deviceId = deviceInfo.getDeviceId()
        let apnsToken = apnsManager.getToken()
        let model = deviceInfo.getDeviceModel()
        let brand = deviceInfo.getDeviceBrand()
        let osVersion = deviceInfo.getOSVersion()
        let nodeId = deviceInfo.getNodeId()

        print("Device info collected:")
        print("  Device ID: \(deviceId)")
        print("  APNs Token: \(apnsToken ?? "none")")
        print("  Model: \(model)")
        print("  Brand: \(brand)")
        print("  OS Version: \(osVersion)")

        // Step 6: Create registration request
        let request = RegistrationRequest(
            serialNumber: serialNumber,
            id: deviceId,
            apnsToken: apnsToken,
            publicKey: publicKey,
            attestationBlob: attestationBlob,
            deviceModel: model,
            deviceBrand: brand,
            osVersion: osVersion,
            nodeId: nodeId
        )

        // Step 7: Send registration request to backend
        print("Sending registration request to backend...")
        let response = try await api.register(request: request)

        // Step 8: Store registration data locally
        if response.success {
            print("Registration successful!")

            UserDefaults.standard.set(true, forKey: registeredKey)
            UserDefaults.standard.set(serialNumber, forKey: serialNumberKey)
            UserDefaults.standard.set(publicKey, forKey: publicKeyKey)
            UserDefaults.standard.set(attestationBlob, forKey: attestationBlobKey)

            // Store registration date
            let dateFormatter = ISO8601DateFormatter()
            let now = dateFormatter.string(from: Date())
            UserDefaults.standard.set(now, forKey: registrationDateKey)

            // Update complication
            NotificationCenter.default.post(name: .registrationStatusChanged, object: nil)
        } else {
            throw NSError(
                domain: "Registration",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: response.message ?? "Registration failed"]
            )
        }
    }

    // MARK: - Deregistration

    func deregisterDevice() async throws {
        print("Starting device deregistration...")

        guard let serialNumber = getSerialNumber(),
              let publicKey = getStoredPublicKey(),
              let attestationBlob = getStoredAttestationBlob() else {
            throw NSError(
                domain: "Deregistration",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Missing registration data"]
            )
        }

        // Create deregistration request
        let request = DeregistrationRequest(
            publicKey: publicKey,
            attestationBlob: attestationBlob,
            serialNumber: serialNumber
        )

        // Send deregistration request to backend
        print("Sending deregistration request to backend...")
        let response = try await api.deregister(request: request)

        // Clear local storage
        if response.success {
            print("Deregistration successful!")

            UserDefaults.standard.set(false, forKey: registeredKey)
            UserDefaults.standard.removeObject(forKey: serialNumberKey)
            UserDefaults.standard.removeObject(forKey: registrationDateKey)
            UserDefaults.standard.removeObject(forKey: publicKeyKey)
            UserDefaults.standard.removeObject(forKey: attestationBlobKey)

            // Delete cryptographic keys
            try? attestationManager.deleteKeyPair()

            // Delete APNs token
            apnsManager.deleteToken()

            // Update complication
            NotificationCenter.default.post(name: .registrationStatusChanged, object: nil)
        } else {
            throw NSError(
                domain: "Deregistration",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: response.message ?? "Deregistration failed"]
            )
        }
    }

    // MARK: - Account Summary

    func fetchAccountSummary() async throws -> AccountSummaryResponse {
        print("Fetching account summary...")

        guard let serialNumber = getSerialNumber(),
              let publicKey = getStoredPublicKey(),
              let attestationBlob = getStoredAttestationBlob() else {
            throw NSError(
                domain: "AccountSummary",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Missing registration data"]
            )
        }

        // Create account summary request
        let request = AccountSummaryRequest(
            serialNumber: serialNumber,
            publicKey: publicKey,
            attestationBlob: attestationBlob
        )

        // Send account summary request to backend
        print("Sending account summary request to backend...")
        let response = try await api.fetchAccountSummary(request: request)

        print("Account summary fetched successfully")
        return response
    }
}
