//
//  AppDelegate.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import Foundation
import WatchKit
import UserNotifications

class AppDelegate: NSObject, WKExtensionDelegate {
    func applicationDidFinishLaunching() {
        print("CloudCrypto Watch App launched")

        // Set up notification delegate
        UNUserNotificationCenter.current().delegate = APNsManager.shared

        // Log device information
        DeviceInfoManager.shared.logDeviceInfo()
    }

    // MARK: - Remote Notification Registration

    func didRegisterForRemoteNotifications(withDeviceToken deviceToken: Data) {
        print("Successfully registered for remote notifications")
        APNsManager.shared.storeToken(deviceToken)
    }

    func didFailToRegisterForRemoteNotificationsWithError(_ error: Error) {
        print("Failed to register for remote notifications: \(error.localizedDescription)")
    }

    // MARK: - Remote Notification Handling

    func didReceiveRemoteNotification(
        _ userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (WKBackgroundFetchResult) -> Void
    ) {
        print("Remote notification received: \(userInfo)")

        // Process the notification
        APNsManager.shared.userNotificationCenter(
            UNUserNotificationCenter.current(),
            willPresent: UNNotification(),
            withCompletionHandler: { _ in }
        )

        completionHandler(.newData)
    }
}
