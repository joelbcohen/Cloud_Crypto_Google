//
//  RegistrationFormView.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI

struct RegistrationFormView: View {
    @EnvironmentObject var viewModel: RegistrationViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Title
                Text("Device Registration")
                    .font(.headline)
                    .padding(.top, 8)

                // Serial Number Input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Serial Number")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    TextField("Enter or generate", text: $viewModel.serialNumber)
                        .textFieldStyle(.roundedBorder)
                        .font(.caption)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)

                    Button(action: {
                        viewModel.generateSerialNumber()
                    }) {
                        Label("Generate UUID", systemImage: "arrow.clockwise")
                            .font(.caption)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(.blue)
                }

                // Description
                Text("A unique serial number will identify this device on the Cloud Crypto ledger.")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 8)

                // Action Buttons
                VStack(spacing: 8) {
                    Button(action: {
                        Task {
                            await viewModel.registerDevice()
                        }
                    }) {
                        Label("Register", systemImage: "checkmark.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.green)
                    .disabled(viewModel.serialNumber.isEmpty)

                    Button(action: {
                        viewModel.showMainScreen()
                    }) {
                        Label("Cancel", systemImage: "xmark")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(.red)
                }
            }
            .padding(.horizontal, 12)
        }
    }
}

#Preview {
    RegistrationFormView()
        .environmentObject(RegistrationViewModel())
}
