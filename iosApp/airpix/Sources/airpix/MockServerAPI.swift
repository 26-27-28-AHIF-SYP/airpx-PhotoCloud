#if DEBUG
import Foundation
import PhotoCloudSync

enum MockFailureMode: String, CaseIterable, Identifiable, Sendable {
    case none
    case wrongPassword
    case unreachable
    case expiredToken

    var id: Self { self }
    var title: String {
        switch self {
        case .none: AppStrings.noSimulation
        case .wrongPassword: AppStrings.wrongPasswordMode
        case .unreachable: AppStrings.unreachableMode
        case .expiredToken: AppStrings.expiringTokenMode
        }
    }
}

struct MockServerAPI: ServerAPI {
    let failureMode: MockFailureMode

    func serverInfo(baseURL: URL) async throws -> ServerInfo {
        if failureMode == .unreachable { throw ServerAPIError.unreachable }
        return ServerInfo(status: "up", name: AppStrings.mockServerName, apiVersion: "1")
    }

    func login(baseURL: URL, request: LoginRequest) async throws -> LoginResponse {
        if failureMode == .unreachable { throw ServerAPIError.unreachable }
        guard failureMode != .wrongPassword,
              request.email == "demo@airpx.local", request.password == "airpx-demo" else {
            throw ServerAPIError.invalidCredentials
        }
        let lifetime: TimeInterval = failureMode == .expiredToken ? 90 : 86_400
        return LoginResponse(accessToken: "mock-access-token", tokenType: "Bearer", expiresAt: .now.addingTimeInterval(lifetime))
    }

    func missingHashes(baseURL: URL, token: String, hashes: [String]) async throws -> Set<String> {
        if failureMode == .expiredToken { throw ServerAPIError.unauthorized }
        return Set(hashes)
    }

    func uploadPhoto(baseURL: URL, token: String, asset: PhotoSyncAsset, fileURL: URL, metadata: PhotoUploadMetadata) async throws {}

    func listPhotos(baseURL: URL, token: String) async throws -> [PhotoServerRecord] { [] }
}
#endif
