//
//  ApiModels.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import Foundation

// MARK: - Registration Request

struct RegistrationRequest: Codable {
    let serialNumber: String
    let id: String
    let apnsToken: String?
    let publicKey: String
    let attestationBlob: String
    let deviceModel: String
    let deviceBrand: String
    let osVersion: String
    let nodeId: String
}

// MARK: - Registration Response

struct RegistrationResponse: Codable {
    let success: Bool
    let message: String?
    let account: AccountData?
}

struct AccountData: Codable {
    let id: Int?
    let serialNumber: String?
    let balance: String?
    let createdAt: String?
}

// MARK: - Deregistration Request

struct DeregistrationRequest: Codable {
    let publicKey: String
    let attestationBlob: String
    let serialNumber: String
}

// MARK: - Deregistration Response

struct DeregistrationResponse: Codable {
    let success: Bool
    let message: String?
}

// MARK: - Account Summary Request

struct AccountSummaryRequest: Codable {
    let serialNumber: String
    let publicKey: String
    let attestationBlob: String
}

// MARK: - Account Summary Response

struct AccountSummaryResponse: Codable {
    let account: AccountInfo
}

struct AccountInfo: Codable {
    let id: Int
    let serialNumber: String
    let balance: String
    let model: String
    let brand: String
    let osVersion: String
    let createdAt: String
    let totalSent: String
    let totalReceived: String
    let transactionsSent: Int
    let transactionsReceived: Int

    enum CodingKeys: String, CodingKey {
        case id
        case serialNumber = "serial_number"
        case balance
        case model
        case brand
        case osVersion = "os_version"
        case createdAt = "created_at"
        case totalSent = "total_sent"
        case totalReceived = "total_received"
        case transactionsSent = "transactions_sent"
        case transactionsReceived = "transactions_received"
    }
}

// MARK: - Error Response

struct ErrorResponse: Codable {
    let success: Bool
    let message: String?
}
