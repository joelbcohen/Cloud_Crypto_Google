//
//  APNsManager.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//
//  Apple Push Notification Service Manager
//  Equivalent to FcmTokenManager.kt in Android version

import Foundation
import UserNotifications

class APNsManager: NSObject {
    static let shared = APNsManager()

    private let tokenKey = "apnsToken"
    private var currentToken: String?

    override private init() {
        super.init()
        loadTokenFromStorage()
    }

    // MARK: - Token Management

    /// Request permission for push notifications and register for APNs
    func requestNotificationPermission() async -> Bool {
        let center = UNUserNotificationCenter.current()

        do {
            let granted = try await center.requestAuthorization(options: [.alert, .sound, .badge])
            if granted {
                print("Notification permission granted")
                // Trigger registration on main thread
                await MainActor.run {
                    WKExtension.shared().registerForRemoteNotifications()
                }
            } else {
                print("Notification permission denied")
            }
            return granted
        } catch {
            print("Error requesting notification permission: \(error)")
            return false
        }
    }

    /// Store the APNs device token
    func storeToken(_ token: Data) {
        let tokenString = token.map { String(format: "%02.2hhx", $0) }.joined()
        currentToken = tokenString
        UserDefaults.standard.set(tokenString, forKey: tokenKey)
        print("APNs token stored: \(tokenString)")
    }

    /// Get the current APNs token
    func getToken() -> String? {
        return currentToken
    }

    /// Delete the stored APNs token
    func deleteToken() {
        currentToken = nil
        UserDefaults.standard.removeObject(forKey: tokenKey)
        print("APNs token deleted")
    }

    /// Load token from UserDefaults
    private func loadTokenFromStorage() {
        currentToken = UserDefaults.standard.string(forKey: tokenKey)
        if let token = currentToken {
            print("APNs token loaded from storage: \(token)")
        }
    }

    /// Check if we have a valid token
    func hasToken() -> Bool {
        return currentToken != nil && !currentToken!.isEmpty
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension APNsManager: UNUserNotificationCenterDelegate {
    /// Handle notification when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        print("Notification received in foreground: \(notification.request.content.userInfo)")

        // Show alert, sound, and badge
        completionHandler([.banner, .sound, .badge])
    }

    /// Handle notification tap
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        print("Notification tapped: \(userInfo)")

        handleNotificationPayload(userInfo)

        completionHandler()
    }

    /// Process notification payload
    private func handleNotificationPayload(_ userInfo: [AnyHashable: Any]) {
        // Check for custom data in the payload
        if let messageType = userInfo["type"] as? String {
            print("Message type: \(messageType)")

            switch messageType {
            case "registration_update":
                print("Registration update received")
                // Update complication
                NotificationCenter.default.post(name: .registrationStatusChanged, object: nil)

            case "config_update":
                print("Config update received")
                // Handle configuration changes

            case "status_update":
                print("Status update received")
                // Update UI

            default:
                print("Unknown message type: \(messageType)")
            }
        }

        // Store the notification in UserDefaults for later reference
        if let messageData = try? JSONSerialization.data(withJSONObject: userInfo, options: []) {
            UserDefaults.standard.set(messageData, forKey: "lastNotification")
        }
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let registrationStatusChanged = Notification.Name("registrationStatusChanged")
}
