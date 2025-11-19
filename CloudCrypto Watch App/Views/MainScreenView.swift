//
//  MainScreenView.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI

struct MainScreenView: View {
    @EnvironmentObject var viewModel: RegistrationViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // Title
                Text("Cloud Crypto")
                    .font(.headline)
                    .padding(.top, 8)

                // Status Section
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("Status:")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(viewModel.isRegistered ? "Registered" : "Not Registered")
                            .font(.caption)
                            .foregroundColor(viewModel.isRegistered ? .green : .orange)
                            .bold()
                    }

                    if viewModel.isRegistered {
                        Divider()

                        HStack {
                            Text("Serial:")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(truncateSerialNumber(viewModel.serialNumber))
                                .font(.caption2)
                                .lineLimit(1)
                        }

                        if let date = viewModel.registrationDate {
                            HStack {
                                Text("Registered:")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text(formatDate(date))
                                    .font(.caption2)
                            }
                        }
                    }
                }
                .padding(12)
                .background(Color(.systemGray6))
                .cornerRadius(12)

                // Action Buttons
                VStack(spacing: 8) {
                    if viewModel.isRegistered {
                        Button(action: {
                            Task {
                                await viewModel.deregisterDevice()
                            }
                        }) {
                            Label("De-Register", systemImage: "xmark.circle")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.red)

                        Button(action: {
                            viewModel.showAccountSummary()
                        }) {
                            Label("Account", systemImage: "chart.bar")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    } else {
                        Button(action: {
                            viewModel.showRegistrationForm()
                        }) {
                            Label("Register", systemImage: "checkmark.circle")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.green)
                    }

                    Button(action: {
                        // Settings action (placeholder)
                        print("Settings tapped")
                    }) {
                        Label("Settings", systemImage: "gear")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
                .padding(.horizontal, 4)
            }
            .padding(.horizontal, 8)
        }
    }

    // MARK: - Helper Functions

    private func truncateSerialNumber(_ serial: String) -> String {
        if serial.count > 12 {
            let start = serial.prefix(6)
            let end = serial.suffix(6)
            return "\(start)...\(end)"
        }
        return serial
    }

    private func formatDate(_ isoDate: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: isoDate) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .short
            displayFormatter.timeStyle = .short
            return displayFormatter.string(from: date)
        }
        return isoDate
    }
}

#Preview {
    let viewModel = RegistrationViewModel()
    viewModel.isRegistered = true
    viewModel.serialNumber = "12345678-1234-1234-1234-123456789012"
    viewModel.registrationDate = ISO8601DateFormatter().string(from: Date())

    return MainScreenView()
        .environmentObject(viewModel)
}
