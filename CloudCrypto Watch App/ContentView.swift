//
//  ContentView.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var viewModel: RegistrationViewModel

    var body: some View {
        switch viewModel.currentScreen {
        case .main:
            MainScreenView()
        case .registrationForm:
            RegistrationFormView()
        case .accountSummary:
            AccountSummaryView()
        case .loading:
            LoadingView()
        case .error(let message):
            ErrorView(message: message)
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(RegistrationViewModel())
}
