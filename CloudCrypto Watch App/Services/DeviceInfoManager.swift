//
//  DeviceInfoManager.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import Foundation
import WatchKit

class DeviceInfoManager {
    static let shared = DeviceInfoManager()

    private init() {}

    // MARK: - Device Information

    /// Get unique device identifier (equivalent to Android ID)
    /// Uses the identifierForVendor which is unique per vendor per device
    func getDeviceId() -> String {
        if let identifier = WKInterfaceDevice.current().identifierForVendor {
            return identifier.uuidString
        }
        // Fallback: generate and store a UUID in UserDefaults
        let key = "deviceId"
        if let storedId = UserDefaults.standard.string(forKey: key) {
            return storedId
        }
        let newId = UUID().uuidString
        UserDefaults.standard.set(newId, forKey: key)
        return newId
    }

    /// Get device model (e.g., "Apple Watch Series 8")
    func getDeviceModel() -> String {
        let device = WKInterfaceDevice.current()
        return device.model
    }

    /// Get device brand (always "Apple" for Apple Watch)
    func getDeviceBrand() -> String {
        return "Apple"
    }

    /// Get OS version (e.g., "10.0")
    func getOSVersion() -> String {
        let device = WKInterfaceDevice.current()
        return device.systemVersion
    }

    /// Get node ID (equivalent to Wearable Node ID in Android)
    /// For iOS, we can use the same device identifier or a unique watch identifier
    func getNodeId() -> String {
        // Use device identifier as node ID
        return getDeviceId()
    }

    /// Get all device info as a dictionary for logging
    func getAllDeviceInfo() -> [String: String] {
        return [
            "deviceId": getDeviceId(),
            "model": getDeviceModel(),
            "brand": getDeviceBrand(),
            "osVersion": getOSVersion(),
            "nodeId": getNodeId()
        ]
    }

    /// Log device info for debugging
    func logDeviceInfo() {
        let info = getAllDeviceInfo()
        print("Device Info:")
        for (key, value) in info {
            print("  \(key): \(value)")
        }
    }
}
