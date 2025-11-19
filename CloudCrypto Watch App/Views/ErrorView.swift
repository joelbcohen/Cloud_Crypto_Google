//
//  ErrorView.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI

struct ErrorView: View {
    @EnvironmentObject var viewModel: RegistrationViewModel

    let message: String

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Error Icon
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.largeTitle)
                    .foregroundColor(.red)
                    .padding(.top, 8)

                // Error Title
                Text("Error")
                    .font(.headline)

                // Error Message
                Text(message)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 12)

                // Action Buttons
                VStack(spacing: 8) {
                    Button(action: {
                        viewModel.retryLastOperation()
                    }) {
                        Label("Retry", systemImage: "arrow.clockwise")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.blue)

                    Button(action: {
                        viewModel.cancelOperation()
                    }) {
                        Label("Cancel", systemImage: "xmark")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(.red)
                }
                .padding(.horizontal, 12)
            }
        }
    }
}

#Preview {
    ErrorView(message: "Failed to connect to the server. Please check your internet connection and try again.")
        .environmentObject(RegistrationViewModel())
}
