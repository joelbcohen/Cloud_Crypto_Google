//
//  CloudCryptoApp.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI

@main
struct CloudCryptoApp: App {
    @StateObject private var registrationViewModel = RegistrationViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(registrationViewModel)
        }
    }
}
