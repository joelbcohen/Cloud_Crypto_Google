//
//  RegistrationViewModel.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import Foundation
import Combine

enum AppScreen: Equatable {
    case main
    case registrationForm
    case accountSummary
    case loading
    case error(String)
}

@MainActor
class RegistrationViewModel: ObservableObject {
    @Published var currentScreen: AppScreen = .main
    @Published var serialNumber: String = ""
    @Published var isRegistered: Bool = false
    @Published var registrationDate: String? = nil
    @Published var accountSummary: AccountSummaryResponse? = nil

    private let repository: RegistrationRepository
    private var cancellables = Set<AnyCancellable>()

    init(repository: RegistrationRepository = RegistrationRepository()) {
        self.repository = repository
        loadRegistrationStatus()
    }

    // MARK: - Screen Navigation

    func showMainScreen() {
        currentScreen = .main
    }

    func showRegistrationForm() {
        currentScreen = .registrationForm
    }

    func showAccountSummary() {
        currentScreen = .accountSummary
        Task {
            await fetchAccountSummary()
        }
    }

    func showLoading() {
        currentScreen = .loading
    }

    func showError(_ message: String) {
        currentScreen = .error(message)
    }

    // MARK: - Registration Status

    func loadRegistrationStatus() {
        isRegistered = repository.isRegistered()
        serialNumber = repository.getSerialNumber() ?? ""
        registrationDate = repository.getRegistrationDate()
    }

    // MARK: - Registration Actions

    func registerDevice() async {
        showLoading()

        do {
            try await repository.registerDevice(serialNumber: serialNumber)
            loadRegistrationStatus()
            showMainScreen()
        } catch {
            showError("Registration failed: \(error.localizedDescription)")
        }
    }

    func deregisterDevice() async {
        showLoading()

        do {
            try await repository.deregisterDevice()
            loadRegistrationStatus()
            serialNumber = ""
            showMainScreen()
        } catch {
            showError("Deregistration failed: \(error.localizedDescription)")
        }
    }

    func fetchAccountSummary() async {
        do {
            accountSummary = try await repository.fetchAccountSummary()
        } catch {
            showError("Failed to fetch account summary: \(error.localizedDescription)")
        }
    }

    // MARK: - Utility

    func generateSerialNumber() {
        serialNumber = UUID().uuidString
    }

    func retryLastOperation() {
        showMainScreen()
    }

    func cancelOperation() {
        showMainScreen()
    }
}
