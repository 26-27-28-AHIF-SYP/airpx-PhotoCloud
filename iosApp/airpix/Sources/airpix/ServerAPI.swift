import Foundation
import PhotoCloudSync

struct ServerInfo: Codable, Sendable {
    let status: String
    let name: String?
    let apiVersion: String?
}

struct LoginRequest: Codable, Sendable {
    let email: String
    let password: String
}

struct LoginResponse: Codable, Sendable {
    let accessToken: String
    let tokenType: String
    let expiresAt: Date
}

struct ApiError: Codable, Sendable {
    let type: String?
    let title: String?
    let status: Int?
    let detail: String?
    let instance: String?
    let code: String?
    let message: String?
}

struct MissingPhotosResponse: Codable, Sendable {
    let missingHashes: [String]
}

enum ServerAPIError: Error, LocalizedError, Sendable {
    case invalidURL
    case unreachable
    case invalidResponse
    case invalidCredentials
    case unauthorized
    case http(status: Int, apiError: ApiError?)
    case credentialStorage

    var errorDescription: String? {
        switch self {
        case .invalidURL: AppStrings.invalidServerURL
        case .unreachable: AppStrings.serverUnreachable
        case .invalidResponse: AppStrings.invalidServerResponse
        case .invalidCredentials: AppStrings.invalidCredentials
        case .unauthorized: AppStrings.loginAgain
        case let .http(_, apiError): apiError?.detail ?? apiError?.message ?? AppStrings.requestFailed
        case .credentialStorage: AppStrings.credentialStorageError
        }
    }
}

protocol ServerAPI: PhotoSyncAPIClient {
    func serverInfo(baseURL: URL) async throws -> ServerInfo
    func login(baseURL: URL, request: LoginRequest) async throws -> LoginResponse
}

extension JSONDecoder {
    static var serverAPI: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}

extension JSONEncoder {
    static var serverAPI: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }
}
