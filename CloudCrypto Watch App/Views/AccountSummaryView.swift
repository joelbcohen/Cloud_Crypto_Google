//
//  AccountSummaryView.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI

struct AccountSummaryView: View {
    @EnvironmentObject var viewModel: RegistrationViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // Title
                Text("Account Summary")
                    .font(.headline)
                    .padding(.top, 8)

                if let summary = viewModel.accountSummary {
                    // Balance Card
                    VStack(spacing: 6) {
                        Text("Balance")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(formatBalance(summary.account.balance))
                            .font(.title3)
                            .bold()
                            .foregroundColor(.green)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(12)
                    .background(Color(.systemGray6))
                    .cornerRadius(12)

                    // Transactions Section
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Transactions")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Label("Sent", systemImage: "arrow.up.circle")
                                    .font(.caption2)
                                    .foregroundColor(.orange)

                                Text(formatBalance(summary.account.totalSent))
                                    .font(.caption)
                                    .bold()

                                Text("\(summary.account.transactionsSent) txns")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            Divider()

                            VStack(alignment: .leading, spacing: 4) {
                                Label("Received", systemImage: "arrow.down.circle")
                                    .font(.caption2)
                                    .foregroundColor(.green)

                                Text(formatBalance(summary.account.totalReceived))
                                    .font(.caption)
                                    .bold()

                                Text("\(summary.account.transactionsReceived) txns")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding(12)
                    .background(Color(.systemGray6))
                    .cornerRadius(12)

                    // Device Info Section
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Device Info")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Divider()

                        InfoRow(label: "Model", value: summary.account.model)
                        InfoRow(label: "Brand", value: summary.account.brand)
                        InfoRow(label: "OS", value: summary.account.osVersion)
                        InfoRow(label: "Registered", value: formatDate(summary.account.createdAt))
                    }
                    .padding(12)
                    .background(Color(.systemGray6))
                    .cornerRadius(12)

                    // Back Button
                    Button(action: {
                        viewModel.showMainScreen()
                    }) {
                        Label("Back", systemImage: "chevron.left")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                } else {
                    // Loading state
                    ProgressView()
                        .padding()

                    Text("Loading account data...")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.horizontal, 12)
        }
    }

    // MARK: - Helper Views

    struct InfoRow: View {
        let label: String
        let value: String

        var body: some View {
            HStack {
                Text(label + ":")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                Spacer()
                Text(value)
                    .font(.caption2)
                    .lineLimit(1)
            }
        }
    }

    // MARK: - Helper Functions

    private func formatBalance(_ balance: String) -> String {
        if let doubleValue = Double(balance) {
            let formatter = NumberFormatter()
            formatter.numberStyle = .decimal
            formatter.maximumFractionDigits = 2
            formatter.minimumFractionDigits = 2
            return formatter.string(from: NSNumber(value: doubleValue)) ?? balance
        }
        return balance
    }

    private func formatDate(_ isoDate: String) -> String {
        let formatter = ISO8601DateFormatter()
        if let date = formatter.date(from: isoDate) {
            let displayFormatter = DateFormatter()
            displayFormatter.dateStyle = .short
            return displayFormatter.string(from: date)
        }
        return isoDate
    }
}

#Preview {
    let viewModel = RegistrationViewModel()
    viewModel.accountSummary = AccountSummaryResponse(
        account: AccountInfo(
            id: 1,
            serialNumber: "test-123",
            balance: "1000.50",
            model: "Apple Watch Series 8",
            brand: "Apple",
            osVersion: "10.0",
            createdAt: ISO8601DateFormatter().string(from: Date()),
            totalSent: "250.00",
            totalReceived: "1250.50",
            transactionsSent: 5,
            transactionsReceived: 10
        )
    )

    return AccountSummaryView()
        .environmentObject(viewModel)
}
