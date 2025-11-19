//
//  RegistrationApi.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import Foundation

enum NetworkError: Error {
    case invalidURL
    case invalidResponse
    case httpError(statusCode: Int, message: String?)
    case decodingError(Error)
    case encodingError(Error)
    case unknown(Error)

    var localizedDescription: String {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let statusCode, let message):
            return "HTTP Error \(statusCode): \(message ?? "Unknown error")"
        case .decodingError(let error):
            return "Failed to decode response: \(error.localizedDescription)"
        case .encodingError(let error):
            return "Failed to encode request: \(error.localizedDescription)"
        case .unknown(let error):
            return "Unknown error: \(error.localizedDescription)"
        }
    }
}

class RegistrationApi {
    static let shared = RegistrationApi()

    private let baseURL = "https://fusio.callista.io"
    private let session: URLSession

    init() {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30
        configuration.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: configuration)
    }

    // MARK: - API Endpoints

    func register(request: RegistrationRequest) async throws -> RegistrationResponse {
        let endpoint = "/public/crypto/register"
        let url = try buildURL(endpoint: endpoint)

        let (data, response) = try await performRequest(url: url, method: "POST", body: request)
        try validateResponse(response)

        // Log the response for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("Registration response: \(jsonString)")
        }

        return try decodeResponse(data: data)
    }

    func deregister(request: DeregistrationRequest) async throws -> DeregistrationResponse {
        let endpoint = "/public/crypto/deregister"
        let url = try buildURL(endpoint: endpoint)

        let (data, response) = try await performRequest(url: url, method: "POST", body: request)
        try validateResponse(response)

        return try decodeResponse(data: data)
    }

    func fetchAccountSummary(request: AccountSummaryRequest) async throws -> AccountSummaryResponse {
        let endpoint = "/public/crypto/account_summary"
        let url = try buildURL(endpoint: endpoint)

        let (data, response) = try await performRequest(url: url, method: "POST", body: request)
        try validateResponse(response)

        // Log the response for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("Account summary response: \(jsonString)")
        }

        return try decodeResponse(data: data)
    }

    // MARK: - Private Helper Methods

    private func buildURL(endpoint: String) throws -> URL {
        guard let url = URL(string: baseURL + endpoint) else {
            throw NetworkError.invalidURL
        }
        return url
    }

    private func performRequest<T: Encodable>(url: URL, method: String, body: T) async throws -> (Data, URLResponse) {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        do {
            let encoder = JSONEncoder()
            encoder.keyEncodingStrategy = .convertToSnakeCase
            request.httpBody = try encoder.encode(body)

            // Log request for debugging
            if let bodyString = String(data: request.httpBody!, encoding: .utf8) {
                print("\(method) \(url.absoluteString)")
                print("Request body: \(bodyString)")
            }
        } catch {
            throw NetworkError.encodingError(error)
        }

        do {
            return try await session.data(for: request)
        } catch {
            throw NetworkError.unknown(error)
        }
    }

    private func validateResponse(_ response: URLResponse) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw NetworkError.invalidResponse
        }

        print("Response status code: \(httpResponse.statusCode)")

        guard (200...299).contains(httpResponse.statusCode) else {
            throw NetworkError.httpError(statusCode: httpResponse.statusCode, message: nil)
        }
    }

    private func decodeResponse<T: Decodable>(data: Data) throws -> T {
        do {
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            return try decoder.decode(T.self, from: data)
        } catch {
            print("Decoding error: \(error)")
            throw NetworkError.decodingError(error)
        }
    }
}
